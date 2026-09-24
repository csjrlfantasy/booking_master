"""Offline CI contracts: exercise fail-closed scripts without remote access."""
import json
import os
from pathlib import Path
import subprocess

import pytest

ROOT = Path(__file__).resolve().parents[3]
VERIFY = ROOT / 'ci/jenkins/verify-prerequisites.ps1'
DEPLOY = ROOT / 'ci/jenkins/deploy-public.ps1'


def invoke(script, *args, env=None):
    return subprocess.run(['pwsh', '-NoProfile', '-File', str(script), *args],
                          capture_output=True, text=True, encoding='utf-8', env=env, timeout=20)


def test_pipeline_defaults_and_evidence_contract():
    text = (ROOT / 'Jenkinsfile').read_text(encoding='utf-8')
    for stage in ['Checkout', 'Secret scan', 'Backend focused tests and build',
                  'Frontend build', 'Python and browser prerequisites', 'Offline framework',
                  'API UI E2E collection', 'Controlled deployment', 'Public validation']:
        assert f"stage('{stage}')" in text
    for flag in ['DEPLOY_APP', 'RUN_PUBLIC_TESTS', 'ALLOW_WRITE_TESTS',
                 'ALLOW_DESTRUCTIVE_COMMERCE', 'MIGRATION_ACK', 'TEST_SUPPORT_READY']:
        assert f"booleanParam(name: '{flag}', defaultValue: false" in text
    for credential in ['booking-deploy-ssh', 'booking-known-hosts', 'booking-migration-evidence',
                       'booking-course-test-key', 'booking-admin-username-password']:
        assert credential in text
    assert 'disableConcurrentBuilds()' in text and "lock(resource: 'booking-public-environment')" in text
    assert 'skipDefaultCheckout(true)' in text and 'deleteDir()' in text
    assert 'retry(' not in text and 'catchError' not in text
    assert 'summary.statuses.error' in text and 'summary.collectionOnly' in text
    assert 'summary.statuses.xfail' in text and 'summary.statuses.xpass' in text
    assert 'junit(' in text and 'allure(' in text
    assert '**/*.zip' in text and 'allowEmptyArchive: true' in text
    assert 'archiveArtifacts artifacts: \'**/*\'' not in text


def test_powershell_scripts_parse():
    for script in (VERIFY, DEPLOY):
        assert script.exists()
        env = os.environ.copy()
        env['CI_SCRIPT_TO_PARSE'] = str(script)
        result = subprocess.run(['pwsh', '-NoProfile', '-Command',
            '$e=$null; $t=$null; [System.Management.Automation.Language.Parser]::ParseFile($env:CI_SCRIPT_TO_PARSE,[ref]$t,[ref]$e) | Out-Null; if($e){exit 1}'],
            capture_output=True, text=True, encoding='utf-8', env=env)
        assert result.returncode == 0, result.stderr


def clean_env():
    return {k: v for k, v in os.environ.items() if not k.startswith(('BOOKING_', 'COURSE_', 'DEPLOY_', 'MIGRATION_', 'TEST_SUPPORT_', 'ALLOW_'))}


def test_offline_mode_has_no_credential_dependency():
    result = invoke(VERIFY, env=clean_env())
    assert result.returncode == 0, result.stderr
    assert 'Offline' in result.stdout


@pytest.mark.parametrize('mode', ['deploy', 'public'])
def test_external_mode_rejects_missing_authorization(mode):
    result = invoke(VERIFY, '-Mode', mode, env=clean_env())
    assert result.returncode != 0
    assert 'required' in (result.stdout + result.stderr).lower()


def approved_env(tmp_path):
    env = clean_env()
    for name in ['BOOKING_SSH_KEY', 'BOOKING_KNOWN_HOSTS']:
        p = tmp_path / name
        p.write_text('synthetic fixture', encoding='utf-8')
        env[name] = str(p)
    evidence = tmp_path / 'evidence.json'
    evidence.write_text(json.dumps({'environment': 'public', 'inventoryRestoredColumn': True,
        'unknownInventoryRestoredRows': 0, 'backupReference': 'synthetic-backup',
        'auditedBy': 'synthetic-reviewer', 'auditedAtUtc': '2099-01-01T00:00:00Z'}), encoding='utf-8')
    env.update(BOOKING_MIGRATION_EVIDENCE=str(evidence), MIGRATION_ACK='true',
        TEST_SUPPORT_READY='true', ALLOW_WRITE_TESTS='true', ALLOW_DESTRUCTIVE_COMMERCE='true',
        DEPLOY_APP='true', RUN_PUBLIC_TESTS='true', COURSE_TEST_KEY='synthetic-course',
        BOOKING_ADMIN_USERNAME='synthetic-admin', BOOKING_ADMIN_PASSWORD='synthetic-password',
        BOOKING_SSH_USER='jenkins-deploy')
    return env, evidence


def test_future_or_stale_database_evidence_is_rejected(tmp_path):
    env, evidence = approved_env(tmp_path)
    result = invoke(VERIFY, '-Mode', 'deploy', '-WhatIf', env=env)
    assert result.returncode != 0
    assert 'synthetic-course' not in result.stderr + result.stdout
    document = json.loads(evidence.read_text())
    document['auditedAtUtc'] = '2000-01-01T00:00:00Z'
    evidence.write_text(json.dumps(document), encoding='utf-8')
    assert invoke(VERIFY, '-Mode', 'deploy', '-WhatIf', env=env).returncode != 0


def test_whatif_validates_without_ssh_and_unknown_rows_fail_closed(tmp_path):
    from datetime import datetime, timezone
    env, evidence = approved_env(tmp_path)
    document = json.loads(evidence.read_text())
    document['auditedAtUtc'] = datetime.now(timezone.utc).isoformat()
    evidence.write_text(json.dumps(document), encoding='utf-8')
    result = invoke(VERIFY, '-Mode', 'deploy', '-WhatIf', env=env)
    assert result.returncode == 0, result.stderr
    jar, bundle = tmp_path / 'app.jar', tmp_path / 'frontend.tar.gz'
    jar.write_bytes(b'offline fixture')
    bundle.write_bytes(b'offline fixture')
    result = invoke(DEPLOY, '-Jar', str(jar), '-FrontendArchive', str(bundle),
                    '-ReleaseId', '123-abcdef0', '-Execute', '-WhatIf', env=env)
    assert result.returncode == 0, result.stderr
    assert 'What if' in result.stdout
    document['unknownInventoryRestoredRows'] = 1
    evidence.write_text(json.dumps(document), encoding='utf-8')
    assert invoke(VERIFY, '-Mode', 'public', '-WhatIf', env=env).returncode != 0


def test_deployment_rejects_shell_metacharacters_without_echo(tmp_path):
    result = invoke(DEPLOY, '-ReleaseId', '123;injected', env=clean_env())
    assert result.returncode != 0
    assert 'injected' not in result.stdout


def test_deployment_requires_execute_and_strict_host_keys():
    text = DEPLOY.read_text(encoding='utf-8')
    assert 'if (-not $Execute)' in text and '$PSCmdlet.ShouldProcess' in text
    assert 'StrictHostKeyChecking=yes' in text and 'BatchMode=yes' in text
    assert "'verify'" in text and "'rollback'" in text
    assert 'inventory_restored' not in text  # DB changes belong to audited operator work.
    assert 'Invoke-Expression' not in text


@pytest.mark.parametrize('fail_action,expected', [
    ('', 'verify,prepare,scp,scp,install,activate,health'),
    ('verify', 'verify'),
    ('scp', 'verify,prepare,scp'),
    ('install', 'verify,prepare,scp,scp,install'),
    ('activate', 'verify,prepare,scp,scp,install,activate,rollback'),
    ('health', 'verify,prepare,scp,scp,install,activate,health,rollback'),
])
def test_remote_adapter_propagates_failures_and_rolls_back_only_after_activation(tmp_path, fail_action, expected):
    from datetime import datetime, timezone
    env, evidence = approved_env(tmp_path)
    document = json.loads(evidence.read_text())
    document['auditedAtUtc'] = datetime.now(timezone.utc).isoformat()
    evidence.write_text(json.dumps(document), encoding='utf-8')
    for name in ['JAR', 'BUNDLE']:
        artifact = tmp_path / name
        artifact.write_bytes(b'fixture')
        env['CI_' + name] = str(artifact)
    env.update(CI_FAIL_ACTION=fail_action, CI_DEPLOY=str(DEPLOY))
    command = r'''
        $global:actions = [System.Collections.Generic.List[string]]::new()
        function global:ssh {
            $action = $args[$args.Count - 4]
            $global:actions.Add($action)
            $global:LASTEXITCODE = if ($action -eq $env:CI_FAIL_ACTION) { 7 } else { 0 }
        }
        function global:scp {
            $global:actions.Add('scp')
            $global:LASTEXITCODE = if ($env:CI_FAIL_ACTION -eq 'scp') { 7 } else { 0 }
        }
        $failed = $false
        try { & $env:CI_DEPLOY -Jar $env:CI_JAR -FrontendArchive $env:CI_BUNDLE -ReleaseId '123-abcdef0' -Execute }
        catch { $failed = $true }
        Write-Output ('ACTIONS=' + ($global:actions -join ','))
        if ($failed) { exit 1 }
    '''
    result = subprocess.run(['pwsh', '-NoProfile', '-Command', command], env=env,
        capture_output=True, text=True, encoding='utf-8', timeout=20)
    assert f'ACTIONS={expected}' in result.stdout, result.stdout + result.stderr
    assert result.returncode == (1 if fail_action else 0)
