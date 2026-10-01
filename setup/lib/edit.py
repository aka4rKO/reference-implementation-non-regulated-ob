#!/usr/bin/env python3
"""
Small, idempotent edits to configure.properties and deployment.toml files.

  edit.py props FILE KEY=VALUE...
      Set KEY=VALUE lines in a .properties file.

  edit.py toml-set FILE SECTION KEY VALUE
      Set KEY = VALUE inside [SECTION]. VALUE is raw TOML (quote strings yourself).
      Replaces the key if it is there (including a multi-line array), adds it otherwise.
      Adds the section at the end if it is missing. A commented-out copy of the key is left alone.

  edit.py toml-append FILE MARKER BLOCK_FILE
      Append the contents of BLOCK_FILE unless MARKER already appears in FILE.

  edit.py toml-remove-array FILE NAME
      Remove every [[NAME]] block.

The files are never parsed as a whole. Edits work line by line, so comments and layout stay as they are.
"""

import re
import sys

HEADER = re.compile(r"^\s*(\[\[?)\s*([^\]]+?)\s*\]\]?\s*(#.*)?$")


def read(path):
    with open(path) as f:
        return f.read().split("\n")


def write(path, lines):
    with open(path, "w") as f:
        f.write("\n".join(lines))


def header_of(line):
    """Return (is_array, name) for a table header line, else None."""
    m = HEADER.match(line)
    if not m:
        return None
    return m.group(1) == "[[", m.group(2)


def section_bounds(lines, section):
    """Index range (start, end) of the body of [section], or None."""
    for i, line in enumerate(lines):
        h = header_of(line)
        if h and not h[0] and h[1] == section:
            end = len(lines)
            for j in range(i + 1, len(lines)):
                if header_of(lines[j]):
                    end = j
                    break
            return i + 1, end
    return None


def value_end(lines, start):
    """Index after the last line of a value that may be a multi-line array."""
    depth = 0
    for i in range(start, len(lines)):
        text = re.sub(r'"(\\.|[^"\\])*"', '""', lines[i])
        text = text.split("#", 1)[0]
        depth += text.count("[") - text.count("]")
        if depth <= 0:
            return i + 1
    return len(lines)


def toml_set(path, section, key, value):
    lines = read(path)
    new_line = f"{key} = {value}"
    bounds = section_bounds(lines, section)
    if bounds is None:
        while lines and lines[-1].strip() == "":
            lines.pop()
        lines += ["", f"[{section}]", new_line, ""]
        write(path, lines)
        return
    start, end = bounds
    key_re = re.compile(r"^\s*" + re.escape(key) + r"\s*=")
    for i in range(start, end):
        if key_re.match(lines[i]):
            stop = value_end(lines, i)
            lines[i:stop] = [new_line]
            write(path, lines)
            return
    # Insert after the last non-blank line of the section.
    insert_at = start
    for i in range(start, end):
        if lines[i].strip():
            insert_at = i + 1
    lines.insert(insert_at, new_line)
    write(path, lines)


def toml_append(path, marker, block_path):
    with open(path) as f:
        content = f.read()
    if marker in content:
        return
    with open(block_path) as f:
        block = f.read().strip("\n")
    content = content.rstrip("\n") + "\n\n" + block + "\n"
    with open(path, "w") as f:
        f.write(content)


def toml_remove_array(path, name):
    lines = read(path)
    out, skipping = [], False
    for line in lines:
        h = header_of(line)
        if h:
            skipping = h[0] and h[1] == name
        if not skipping:
            out.append(line)
    write(path, out)


def props(path, pairs):
    lines = read(path)
    for pair in pairs:
        key, value = pair.split("=", 1)
        key_re = re.compile(r"^\s*" + re.escape(key) + r"\s*=")
        for i, line in enumerate(lines):
            if key_re.match(line):
                lines[i] = f"{key}={value}"
                break
        else:
            lines.append(f"{key}={value}")
    write(path, lines)


def main(argv):
    if len(argv) < 3:
        sys.exit(__doc__)
    cmd, path = argv[1], argv[2]
    if cmd == "props":
        props(path, argv[3:])
    elif cmd == "toml-set" and len(argv) == 6:
        toml_set(path, argv[3], argv[4], argv[5])
    elif cmd == "toml-append" and len(argv) == 5:
        toml_append(path, argv[3], argv[4])
    elif cmd == "toml-remove-array" and len(argv) == 4:
        toml_remove_array(path, argv[3])
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main(sys.argv)
