#!/usr/bin/env python3
import sys
from pathlib import Path

def check_block_format(text: str) -> bool:
    # Very small heuristic: must contain exactly one triple-backtick fenced block
    fences = text.count('```')
    return fences >= 2 and text.strip().startswith('```')

def scan_file(path: Path) -> bool:
    text = path.read_text(encoding='utf-8')
    return check_block_format(text)

def main():
    bad = []
    for p in sys.argv[1:]:
        path = Path(p)
        if not path.exists():
            continue
        if path.suffix in ('.md', '.txt'):
            ok = scan_file(path)
            if not ok:
                bad.append(str(path))
    if bad:
        print('Files missing copyable block format:')
        for b in bad:
            print(' -', b)
        sys.exit(2)
    print('All scanned files OK')

if __name__ == '__main__':
    main()
