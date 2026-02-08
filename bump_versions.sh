#!/usr/bin/env bash
# Update OSGi bundle and feature versions by setting a timestamp qualifier.
# Run from repo root.

set -euo pipefail
Q="v$(date +%Y%m%d%H%M%S)"

# Update MANIFEST.MF
find . -name MANIFEST.MF -print0 | while IFS= read -r -d '' f; do
  perl -0777 -pe "s/^(Bundle-Version:\\s*)(\\d+)\\.(\\d+)\\.(\\d+)(?:\\.[A-Za-z0-9_-]+)?\\s*\$/\$1\$2.\$3.\$4.$Q/mg" -i "$f"
done

# Update feature.xml (first occurrence of version="x.y.z.*")
find . -name feature.xml -print0 | while IFS= read -r -d '' f; do
  perl -0777 -pe "s/(\\bversion=\")(\\d+)\\.(\\d+)\\.(\\d+)(?:\\.[A-Za-z0-9_-]+)?(\")/\$1\$2.\$3.\$4.$Q\$5/s" -i "$f"
done

echo "Qualifier set to: $Q"
