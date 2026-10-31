"""build_gitbook.py 의 경고 출력 형식을 검사한다. 실행: python3 -m unittest discover -s scripts"""

import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parent / 'build_gitbook.py'
BROKEN = 'https://github.com/off-the-record-wiki/engineering-notes/wiki/Missing-Page'


def make_wiki(directory):
    """목차(_Sidebar)에 걸린 카테고리 페이지가 없는 wiki 페이지를 가리키는 wiki 저장소를 만든다."""
    files = {
        '_Sidebar.md': '* [Language](Tech-Language)\n',
        'Tech-Language.md': f'## Java\n\n* [Java - Missing]({BROKEN})\n',
        'Tech-Stack.md': '',  # 빌드가 항상 읽는 허브 페이지
        'Books.md': '',
        'Retrospective.md': '',
    }
    for name, text in files.items():
        (directory / name).write_text(text, encoding='utf-8')
    git = ['git', '-C', str(directory), '-c', 'user.name=t', '-c', 'user.email=t@t']
    subprocess.run(git[:3] + ['init', '-q'], check=True)
    subprocess.run(git + ['add', '.'], check=True)
    subprocess.run(git + ['commit', '-q', '-m', 'wiki'], check=True)


class 변환_경고_출력(unittest.TestCase):
    def build(self, github_actions):
        with tempfile.TemporaryDirectory() as tmp:
            wiki = Path(tmp) / 'wiki'
            wiki.mkdir()
            make_wiki(wiki)
            env = {k: v for k, v in os.environ.items() if k != 'GITHUB_ACTIONS'}
            if github_actions:
                env['GITHUB_ACTIONS'] = 'true'
            return subprocess.run(
                [sys.executable, str(SCRIPT), '--wiki', str(wiki), '--out', str(Path(tmp) / 'out')],
                env=env, capture_output=True, text=True, check=True)

    def test_GitHub_Actions_에서는_깨진_링크_경고가_Actions_경고_명령으로_출력된다(self):
        # Given 없는 wiki 페이지를 가리키는 링크가 있는 wiki
        # When GitHub Actions 환경에서 빌드하면
        result = self.build(github_actions=True)
        # Then stdout 에 ::warning 명령으로 경고가 나온다
        self.assertIn(f'::warning title=build_gitbook::Java: 없는 wiki 페이지 링크 {BROKEN}', result.stdout)

    def test_로컬에서는_깨진_링크_경고가_stderr_에_평문으로_출력된다(self):
        # Given 없는 wiki 페이지를 가리키는 링크가 있는 wiki
        # When GitHub Actions 밖에서 빌드하면
        result = self.build(github_actions=False)
        # Then stderr 에 기존 형식으로 경고가 나오고 ::warning 명령은 없다
        self.assertIn(f'warning: Java: 없는 wiki 페이지 링크 {BROKEN}', result.stderr)
        self.assertNotIn('::warning', result.stdout + result.stderr)


if __name__ == '__main__':
    unittest.main()
