import subprocess
import sys
from pathlib import Path

def run_check(file):
    res = subprocess.run([sys.executable, '../format-checker.py', file], cwd=Path(__file__).parent, capture_output=True)
    return res.returncode, res.stdout.decode(), res.stderr.decode()

def test_good():
    p = Path(__file__).parent / 'good.md'
    p.write_text('''```
Hello
A) ok
Reply: A
```''')
    rc, out, err = run_check(str(p))
    assert rc == 0

def test_bad():
    p = Path(__file__).parent / 'bad.md'
    p.write_text('Hello\nNo fences here')
    rc, out, err = run_check(str(p))
    assert rc != 0

if __name__ == '__main__':
    test_good()
    test_bad()
    print('tests passed')
