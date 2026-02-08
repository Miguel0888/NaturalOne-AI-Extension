#!/usr/bin/env python3
# Update OSGi bundle and feature versions by setting a monotonically increasing qualifier.
# Run this script from the repository root before building the p2 update site.

import re
from pathlib import Path
from datetime import datetime

QUALIFIER = "v" + datetime.now().strftime("%Y%m%d%H%M%S")

MANIFEST_RE = re.compile(r"^(Bundle-Version:\s*)(\d+)\.(\d+)\.(\d+)(?:\.([A-Za-z0-9_-]+))?\s*$", re.MULTILINE)
FEATURE_RE = re.compile(r'(\bversion=")(\d+)\.(\d+)\.(\d+)(?:\.([A-Za-z0-9_-]+))?(")')

def update_manifest(path: Path) -> bool:
    text = path.read_text(encoding="utf-8", errors="replace")
    new_text, n = MANIFEST_RE.subn(lambda m: f"{m.group(1)}{m.group(2)}.{m.group(3)}.{m.group(4)}.{QUALIFIER}", text)
    if n > 0 and new_text != text:
        path.write_text(new_text, encoding="utf-8")
        return True
    return False

def update_feature_xml(path: Path) -> bool:
    text = path.read_text(encoding="utf-8", errors="replace")
    def repl(m):
        return f'{m.group(1)}{m.group(2)}.{m.group(3)}.{m.group(4)}.{QUALIFIER}{m.group(6)}'
    new_text, n = FEATURE_RE.subn(repl, text, count=1)  # update only the feature's own version attribute
    if n > 0 and new_text != text:
        path.write_text(new_text, encoding="utf-8")
        return True
    return False

def main():
    changed = 0

    # Update all MANIFEST.MF files
    for mf in Path(".").rglob("MANIFEST.MF"):
        if update_manifest(mf):
            print(f"Updated: {mf}")
            changed += 1

    # Update all feature.xml files (usually under features/*/feature.xml)
    for fx in Path(".").rglob("feature.xml"):
        if update_feature_xml(fx):
            print(f"Updated: {fx}")
            changed += 1

    print(f"Qualifier set to: {QUALIFIER}")
    print(f"Files changed: {changed}")

if __name__ == "__main__":
    main()
