# GitHub Jenkins Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish a secret-free Booking Master repository and operate a parameterized Jenkins pipeline that builds, deploys, tests, reports, cleans up, and rolls back the teaching environment.

**Architecture:** GitHub is the source of truth, Jenkinsfile is the pipeline definition, and deployment uses immutable numbered releases plus a `current` link and shared runtime state. Jenkins injects credentials, calls versioned deployment scripts over SSH, runs the public Playwright suite, and rolls back automatically when health or smoke gates fail.

**Tech Stack:** Git, GitHub, Jenkins Declarative Pipeline, Maven, Node.js, Python, Playwright, Allure, SSH, systemd, Nginx, PowerShell/Bash deployment scripts.

---

## File structure

- Modify `.gitignore`: complete local/runtime exclusions.
- Create `scripts/ci/scan-secrets.ps1`: deterministic pre-push/CI guard.
- Create `scripts/deploy/{install-release,activate-release,rollback-release,health-check}.sh`: versioned deployment contract.
- Create `deploy/systemd/booking-api.service`: service template using external EnvironmentFile.
- Create `automation-tests/Dockerfile`: reproducible Playwright runner.
- Create root `Jenkinsfile`: parameterized pipeline.
- Create `docs/operations/{jenkins-setup,deployment-runbook,credential-rotation}.md`.

### Task 1: Audit the dirty repository and public-publish boundary

- [ ] Save `git status --short`, `git diff --stat`, `git ls-files`, and the untracked-file list to a private local review artifact outside the repository.
- [ ] Classify each untracked/modified path as source, generated output, runtime data, local helper, secret-bearing config, or unknown.
- [ ] Do not use `git add .`; create an explicit allow-list of source, tests, scripts, documentation, lockfiles, and safe configuration templates.
- [ ] Search tracked and proposed files for password assignments, private key headers, JWT secrets, API tokens, connection strings, and the credentials previously disclosed in chat/source.
- [ ] Rotate any credential found in committed history before pushing; removing it from the tip is not sufficient.
- [ ] Verify the repository contains no uploads, logs, database dumps, `.env`, local configuration, browser artifacts, or dependency directories.

### Task 2: Add an executable secret scan

**Files:**
- Create: `scripts/ci/scan-secrets.ps1`
- Modify: `.gitignore`
- Create: `scripts/tests/secret-scan.Tests.ps1`

- [ ] Write Pester tests that create a temporary safe file and a temporary file containing `password: literal-value`; assert the scanner returns 0 for the first and non-zero for the second.
- [ ] Implement the scanner over `git ls-files` plus explicitly proposed paths. Match private-key headers, common token formats, secret-like YAML properties with literal values, and known disclosed credential fingerprints supplied only through a process-local deny-list variable.
- [ ] Exclude test fixtures that contain the scanner's synthetic string by exact path, not by disabling the pattern globally.
- [ ] Run `Invoke-Pester scripts/tests/secret-scan.Tests.ps1`; expect PASS.
- [ ] Run `pwsh -File scripts/ci/scan-secrets.ps1`; expect exit code 0.
- [ ] Commit as `security: add repository publication guard`.

### Task 3: Create reproducible Playwright runner image

**Files:**
- Create: `automation-tests/Dockerfile`
- Create: `automation-tests/.dockerignore`

- [ ] Base the image on the official Python Playwright image matching the pinned Playwright major/minor version.
- [ ] Copy and install `requirements.txt` before copying the test source to preserve layer caching.
- [ ] Set `PYTHONUNBUFFERED=1`, `PYTHONDONTWRITEBYTECODE=1`, workdir `/tests`, and default command `python -m pytest --collect-only -q`.
- [ ] Exclude reports, traces, videos, caches, `.env`, and legacy tests from the build context.
- [ ] Build with `docker build -t booking-playwright:local automation-tests` and run the default command; expect collection success.
- [ ] Commit as `build: add reproducible Playwright runner`.

### Task 4: Implement immutable release scripts

**Files:**
- Create: `scripts/deploy/install-release.sh`
- Create: `scripts/deploy/activate-release.sh`
- Create: `scripts/deploy/rollback-release.sh`
- Create: `scripts/deploy/health-check.sh`
- Create: `deploy/systemd/booking-api.service`

- [ ] `install-release.sh BUILD_NUMBER JAR DIST_ARCHIVE` creates `/opt/booking-master/releases/$BUILD_NUMBER`, refuses an existing release, validates both artifact checksums, expands frontend files, and never touches `current`.
- [ ] `activate-release.sh BUILD_NUMBER` validates the release, atomically replaces the `current` symlink, restarts `booking-api.service`, reloads Nginx, and records the previous target in `/opt/booking-master/.previous`.
- [ ] `health-check.sh` polls `/actuator/health` and `/login` with bounded attempts, requires backend status `UP` and HTTP 200, then exits 0.
- [ ] `rollback-release.sh` reads `.previous`, atomically restores the link, restarts services, runs the same health check, and exits non-zero if rollback health fails.
- [ ] The systemd unit runs an unprivileged `booking` user, reads `/opt/booking-master/shared/config/booking-api.env`, uses the current JAR, writes only to shared log/upload directories, and sets `Restart=on-failure`.
- [ ] Validate scripts with `bash -n`; test link switching in a temporary directory by overriding `BOOKING_ROOT`.
- [ ] Commit as `ops: add atomic deployment and rollback scripts`.

### Task 5: Build the declarative Jenkins pipeline

**Files:**
- Create: `Jenkinsfile`

- [ ] Define parameters `TARGET_ENV`, `TEST_SUITE`, `DEPLOY_APP`, `ALLOW_WRITE_TESTS`, and `BROWSER` with the values approved in the design.
- [ ] Add stages: Checkout, Secret Scan, Backend Test, Frontend Test/Build, Framework Test/Collect, Package, Install Release, Activate, Health Gate, API Smoke, Playwright, and Publish.
- [ ] Use `withCredentials` for GitHub, SSH, admin username/password, and course test key. Never interpolate secrets into Groovy strings; pass them as masked environment variables to shell steps.
- [ ] Archive the JAR, frontend bundle checksum, JUnit XML, Allure results, HTML report, screenshots, traces, and retained failure videos.
- [ ] In `post { always { ... } }`, run cleanup audit and publish reports. In `post { failure { ... } }`, call rollback only if activation occurred in this build.
- [ ] Use `disableConcurrentBuilds()` for deployment stages and a Jenkins `lock('booking-public-environment')` around public write tests so two builds cannot mutate shared teaching data concurrently.
- [ ] Validate with Jenkins Pipeline Linter or the server's `/pipeline-model-converter/validate` endpoint; expect `Jenkinsfile successfully validated`.
- [ ] Commit as `ci: add Booking Master delivery pipeline`.

### Task 6: Prepare Jenkins credentials and runtime without storing passwords

**Files:**
- Create: `docs/operations/jenkins-setup.md`
- Create: `docs/operations/credential-rotation.md`

- [ ] Rotate the server password disclosed in chat before enabling automation.
- [ ] Create an unprivileged `booking` service account and a distinct `jenkins-deploy` account; do not deploy continuously as root.
- [ ] Generate a dedicated Jenkins SSH key, restrict its authorized command or sudo permissions to the four deployment scripts, and disable password-based deployment after verification.
- [ ] Add Jenkins credentials with stable IDs: `github-booking-master`, `booking-deploy-ssh`, `booking-admin-username-password`, and `booking-course-test-key`.
- [ ] Store database, JWT, payment callback, and course-test secrets in `/opt/booking-master/shared/config/booking-api.env` with owner `booking`, mode `0600`.
- [ ] Record plugin prerequisites: Pipeline, Git, Credentials Binding, SSH Agent, JUnit, HTML Publisher, Allure, Workspace Cleanup, and Lockable Resources.
- [ ] Document verification commands without credential values and commit as `docs: add Jenkins security setup`.

### Task 7: Publish the repository safely

- [ ] Verify the target GitHub repository is still empty or reconcile any new remote commits before push.
- [ ] Configure `origin` as `https://github.com/csjrlfantasy/booking_master.git` only after an authenticated GitHub mechanism is available.
- [ ] Push the current implementation branch and the `api-tests-v1-baseline` tag; do not force-push.
- [ ] Open or prepare a review from the implementation branch into `main`; verify the GitHub diff contains no runtime artifacts or secrets.
- [ ] After approval, fast-forward or merge to `main` and enable branch protection if the account plan supports it.
- [ ] Clone the public repository into a new temporary directory and run the secret scan plus framework collection from the clone to prove it is self-contained.

### Task 8: Create and run the Jenkins job

- [ ] Create Pipeline job `booking-master-cicd` with Pipeline from SCM, repository URL, credential ID `github-booking-master`, branch `main`, and script path `Jenkinsfile`.
- [ ] First run with `DEPLOY_APP=false`, `TARGET_ENV=public`, `TEST_SUITE=smoke`, and `ALLOW_WRITE_TESTS=false`; expect build/test/report stages only.
- [ ] Second run with deployment enabled but write tests disabled; expect release activation, health gate, and read-only smoke PASS.
- [ ] Third run with `ALLOW_WRITE_TESTS=true` and E2E suite; expect creation and deletion of a run-owned user and published Allure/JUnit evidence.
- [ ] Trigger one controlled health-gate failure using a non-routable temporary health URL parameter available only to administrators; verify rollback restores the previous release and the job is red.
- [ ] Remove the temporary failure parameter after the rollback demonstration and run a final green deployment.

### Task 9: Operations documentation and final verification

**Files:**
- Create: `docs/operations/deployment-runbook.md`
- Modify: root `README.md`
- Modify: `automation-tests/README.md`

- [ ] Document prerequisites, job parameters, expected stage output, credential rotation, manual rollback, cleanup audit, Allure access, and common failure recovery.
- [ ] Link the teaching flow: commit -> GitHub -> Jenkins -> build -> deploy -> health gate -> API -> Playwright -> cleanup -> report.
- [ ] Verify the public repo from a clean clone, the live UI/API, Jenkins last successful artifacts, current release symlink, and absence of current run-owned resources.
- [ ] Record exact tool versions and successful command outputs in the runbook without secrets.
- [ ] Commit as `docs: publish Booking Master CI/CD runbook`.
