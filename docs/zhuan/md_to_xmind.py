import json
import zipfile
import uuid
import sys
from pathlib import Path


def make_id():
    return uuid.uuid4().hex


def parse_markdown_to_tree(md_text, root_title=None):
    root = {
        "id": make_id(),
        "class": "topic",
        "title": root_title or "Root",
        "children": {
            "attached": []
        }
    }

    stack = [(0, root)]

    for line in md_text.splitlines():
        raw = line.rstrip()
        if not raw.strip():
            continue

        stripped = raw.strip()

        if stripped == "---":
            continue

        level = None
        title = None

        if stripped.startswith("#"):
            hashes = len(stripped) - len(stripped.lstrip("#"))
            title = stripped[hashes:].strip()
            level = hashes
        elif stripped.startswith("- "):
            title = stripped[2:].strip()
            level = 5
        else:
            continue

        if not title:
            continue

        node = {
            "id": make_id(),
            "class": "topic",
            "title": title
        }

        while stack and stack[-1][0] >= level:
            stack.pop()

        parent = stack[-1][1]

        if "children" not in parent:
            parent["children"] = {"attached": []}

        parent["children"]["attached"].append(node)
        stack.append((level, node))

    return root


def create_xmind(md_path, output_path, sheet_title=None):
    md_text = Path(md_path).read_text(encoding="utf-8")
    root_title = sheet_title or Path(md_path).stem
    root_topic = parse_markdown_to_tree(md_text, root_title=root_title)

    content = [
        {
            "id": make_id(),
            "class": "sheet",
            "title": root_title,
            "rootTopic": root_topic
        }
    ]

    metadata = {
        "creator": {
            "name": "XMind",
            "version": "23.11.04321"
        },
        "activeSheetId": content[0]["id"]
    }

    manifest = {
        "file-entries": {
            "content.json": {},
            "metadata.json": {},
            "manifest.json": {}
        }
    }

    with zipfile.ZipFile(output_path, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("content.json", json.dumps(content, ensure_ascii=False, indent=2))
        z.writestr("metadata.json", json.dumps(metadata, ensure_ascii=False, indent=2))
        z.writestr("manifest.json", json.dumps(manifest, ensure_ascii=False, indent=2))

    print(f"转换完成：{output_path}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("用法: python md_to_xmind.py <输入md文件路径> [输出xmind文件路径] [画布标题]")
        print("示例: python md_to_xmind.py input.md output.xmind \"测试点\"")
        sys.exit(1)

    input_md = sys.argv[1]
    output_xmind = sys.argv[2] if len(sys.argv) > 2 else Path(input_md).with_suffix(".xmind").name
    sheet_title = sys.argv[3] if len(sys.argv) > 3 else None

    create_xmind(input_md, output_xmind, sheet_title)