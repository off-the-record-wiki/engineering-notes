#!/usr/bin/env python3
"""GitHub Wiki와 레포의 노트를 GitBook Git Sync용 구조(SUMMARY.md)로 변환한다.

- 목차는 wiki의 _Sidebar에 나열된 카테고리 페이지(Tech-*)와 Books 페이지에서 만든다.
  카테고리의 `##` 섹션이 GitBook 페이지가 되고, 섹션 아래 링크들이 하위 페이지가 된다.
- 어느 카테고리에도 연결되지 않은 wiki 페이지는 제외하고, 그 페이지로 가는 링크는 wiki URL로 남긴다.
- 레포의 claude/ 노트는 "Claude Code" 그룹으로 넣는다.

사용법: python3 scripts/build_gitbook.py --wiki <wiki clone> --out <output dir>
"""

import argparse
import hashlib
import posixpath
import re
import shutil
import subprocess
import sys
import unicodedata
import urllib.parse
from pathlib import Path

DEFAULT_WIKI_URL = 'https://github.com/off-the-record-wiki/study/wiki'

TEXT = r'(?:[^\[\]\n]|\[[^\[\]\n]*\])*'
DEST = r'(?:[^()\s]|\([^()\s]*\))+'
MD_LINK = re.compile(rf'(?P<bang>!?)\[(?P<text>{TEXT})\]\((?P<dest>{DEST})(?P<title>\s+"[^"]*")?\)')
WIKI_LINK = re.compile(
    r'^(?P<abs>https?://(?:www\.)?github\.com/[\w.-]+/study/wiki/)?(?P<page>[^/#?:\s][^/#?\s]*)(?P<anchor>#.*)?$'
)
IMG_TAG = re.compile(r'<img\b[^>]*>', re.I)
IMG_OPEN = re.compile(r'<img\b[^>]*$', re.I)
IMG_ATTR = re.compile(r'\b(src|alt)\s*=\s*"([^"]*)"|\b(src|alt)\s*=\s*\'([^\']*)\'', re.I)
# GitHub에 붙여넣은 이미지에 자동으로 붙는 alt는 버린다.
AUTO_ALT = re.compile(r'^(image|스크린샷.*|screenshot.*|img)$', re.I)
FENCE_OPEN = re.compile(r'^[\s>]*(`{3,}|~{3,})')
FENCE_CLOSE = re.compile(r'^[\s>]*(`{3,}|~{3,})\s*$')
INLINE_CODE = re.compile(r'(`+).+?\1')
# 본문의 List<String> 같은 표기를 GitBook이 HTML 태그로 보고 지우지 않도록 이스케이프한다.
TAG = re.compile(r'<(?=/?([A-Za-z][\w.-]*)(?:[\s/>,]|$))')
HTML_TAGS = {
    'a', 'b', 'br', 'code', 'details', 'em', 'figcaption', 'figure', 'hr', 'i', 'img', 'kbd',
    'mark', 'strong', 'sub', 'summary', 'sup', 'table', 'tbody', 'td', 'th', 'thead', 'tr', 'u',
}
SUMMARY_LINE = re.compile(r'^[ \t]*<summary>(.*?)</summary>[ \t]*$')
BR_LINE = re.compile(r'^[ \t]*<br\s*/?>[ \t]*$')
SECTION = re.compile(r'^##\s+(.+?)\s*$')
ITEM = re.compile(rf'^\s*[*-]\s+\[(?P<text>{TEXT})\]\((?P<dest>{DEST})\)')
NAV_LINE = re.compile(r'^\[←')


def nfc(s):
    return unicodedata.normalize('NFC', s)


def slugify(text, max_bytes=200):  # 파일명 한도 255바이트 안에서 해시와 .md가 붙을 자리를 남긴다
    s = re.sub(r'[\W_]+', '-', unicodedata.normalize('NFKC', text).lower()).strip('-') or 'page'
    if len(s.encode()) > max_bytes:
        head = s.encode()[:max_bytes].decode('utf-8', 'ignore').rstrip('-')
        s = f'{head}-{hashlib.sha1(text.encode()).hexdigest()[:6]}'
    return s


def github_anchor(text):
    return re.sub(r'[^\w\- ]', '', text.strip().lower()).replace(' ', '-')


def heading_title(raw):
    """`## [Java🔥](https://...)` -> ('Java🔥', 'https://...')"""
    m = re.fullmatch(rf'\[({TEXT})\]\(({DEST})\)', raw)
    return (m[1].strip(), m[2]) if m else (raw.strip(), None)


def read_wiki(wiki_dir):
    """wiki의 HEAD에서 .md를 읽는다. 대소문자만 다른 파일이 있어도 macOS에서 빠지지 않게 git으로 읽는다."""
    git = ['git', '-C', str(wiki_dir)]
    names = subprocess.run(git + ['ls-tree', '-r', '-z', '--name-only', 'HEAD'],
                           check=True, capture_output=True).stdout.decode().split('\0')
    names = [n for n in names if n.endswith('.md') and '/' not in n]
    proc = subprocess.run(git + ['cat-file', '--batch'], check=True, capture_output=True,
                          input=''.join(f'HEAD:{n}\n' for n in names).encode())
    pages, data, pos = {}, proc.stdout, 0
    for name in names:
        header_end = data.index(b'\n', pos)
        header = data[pos:header_end].split()
        if header[-1] == b'missing':
            raise SystemExit(f'wiki 파일을 읽지 못했습니다: {name}')
        size = int(header[2])
        pages[nfc(name[:-3])] = data[header_end + 1:header_end + 1 + size].decode('utf-8', 'replace')
        pos = header_end + 2 + size
    sha = subprocess.run(git + ['rev-parse', 'HEAD'], check=True, capture_output=True, text=True).stdout.strip()
    return pages, sha


class Builder:
    def __init__(self, repo, wiki, out, wiki_url):
        self.repo, self.out, self.wiki_url = repo, out, wiki_url.rstrip('/')
        self.pages, self.wiki_sha = read_wiki(wiki)
        self.by_key = {n.casefold(): n for n in self.pages}
        self.paths = {}          # wiki page -> output path
        self.anchor_paths = {}   # (category page, github anchor) -> section path
        self.files = {}          # output path -> content
        self.used = set()
        self.warnings = []

    # ---------- 링크 해석 ----------

    def resolve(self, name):
        name = nfc(name)
        if name in self.pages:
            return name
        return self.by_key.get(name.casefold())

    def link(self, dest, cur):
        m = WIKI_LINK.match(dest)
        if not m:
            return dest
        page = self.resolve(urllib.parse.unquote(m['page']))
        anchor = m['anchor'] or ''
        if page is None:
            if m['abs']:
                self.warnings.append(f'{cur}: 없는 wiki 페이지 링크 {urllib.parse.unquote(m["page"])}')
            return dest
        section = self.anchor_paths.get((page, urllib.parse.unquote(anchor[1:])))
        if section:
            return posixpath.relpath(section, posixpath.dirname(cur))
        target = self.paths.get(page)
        if target is None:
            return f'{self.wiki_url}/{urllib.parse.quote(page, safe="-_.~")}{anchor}'
        return posixpath.relpath(target, posixpath.dirname(cur)) + anchor

    def asset(self, dest, cur, source):
        """레포 파일에서 /docs/image/... 처럼 레포 기준 경로로 건 이미지를 .gitbook/assets로 복사한다."""
        if source is None or re.match(r'^[a-z]+:', dest):
            return dest
        path = urllib.parse.unquote(dest)
        src = self.repo / path.lstrip('/') if path.startswith('/') else source.parent / path
        if not src.is_file():
            self.warnings.append(f'{cur}: 이미지 파일 없음 {dest}')
            return dest
        rel = f'.gitbook/assets/{src.name}'
        self.files[rel] = src
        return posixpath.relpath(rel, posixpath.dirname(cur))

    # ---------- 본문 변환 ----------

    def convert(self, text, cur, source=None):
        out, fence, after_summary, img = [], None, False, []
        for line in text.replace('\r\n', '\n').split('\n'):
            if fence:
                out.append(line)
                m = FENCE_CLOSE.match(line)
                if m and m[1][0] == fence[0] and len(m[1]) >= len(fence):
                    fence = None
                continue
            # 여러 줄에 걸친 <img ...> 태그는 한 줄로 모아서 변환한다.
            if img or IMG_OPEN.search(line):
                img.append(line.strip() if img else line.rstrip())  # 리스트 안의 이미지는 들여쓰기를 지킨다
                if not IMG_OPEN.search(' '.join(img)):
                    out.append(self.convert_line(' '.join(img), cur, source))
                    img = []
                continue
            m = FENCE_OPEN.match(line)
            if m:
                fence = m[1]
                out.append(line)
                continue
            # GitBook의 펼치기 블록은 <details>/<summary> 앞뒤에 빈 줄이 있어야 본문을 마크다운으로 읽는다.
            if after_summary and BR_LINE.match(line):
                continue
            after_summary = False
            if m := SUMMARY_LINE.match(line):
                out += ['', f'<summary>{m[1].strip()}</summary>', '']
                after_summary = True
                continue
            if line.strip() == '</details>':
                out += ['', '</details>']
                continue
            out.append(self.convert_line(line, cur, source))
        out += img
        return '\n'.join(out)

    def convert_line(self, line, cur, source):
        parts, pos = [], 0
        for m in INLINE_CODE.finditer(line):
            parts += [self.convert_text(line[pos:m.start()], cur, source), m[0]]
            pos = m.end()
        parts.append(self.convert_text(line[pos:], cur, source))
        return ''.join(parts)

    @staticmethod
    def img_to_markdown(tag):
        attrs = {(m[1] or m[3]).lower(): m[2] if m[1] else m[4] for m in IMG_ATTR.finditer(tag)}
        if 'src' not in attrs:
            return tag
        alt = attrs.get('alt', '').strip()
        return f'![{"" if AUTO_ALT.match(alt) else alt}]({attrs["src"]})'

    def convert_text(self, s, cur, source):
        s = IMG_TAG.sub(lambda m: self.img_to_markdown(m[0]), s)
        s = MD_LINK.sub(lambda m: '{}[{}]({}{})'.format(
            m['bang'], m['text'],
            self.asset(m['dest'], cur, source) if m['bang'] else self.link(m['dest'], cur),
            m['title'] or ''), s)
        return TAG.sub(lambda m: '<' if m[1].lower() in HTML_TAGS else r'\<', s)

    def page(self, path, title, text, source=None):
        body = self.convert(text, path, source).strip('\n')
        if not body.lstrip().startswith('# '):
            body = f'# {title}\n\n{body}'
        self.files[path] = body + '\n'

    # ---------- 구조 ----------

    def unique(self, directory, slug):
        candidate, n = slug, 2
        while f'{directory}/{candidate}'.casefold() in self.used:
            candidate, n = f'{slug}-{n}', n + 1
        self.used.add(f'{directory}/{candidate}'.casefold())
        return candidate

    def parse_category(self, name, base):
        """카테고리 페이지를 [{title, ref, path, items: [(text, dest, page)]}]로 나눈다."""
        sections, current = [], None
        for line in self.pages[name].split('\n'):
            if m := SECTION.match(line):
                title, ref = heading_title(m[1])
                path = f'{base}/{self.unique(base, slugify(title))}/README.md'
                current = {'title': title, 'ref': ref, 'path': path, 'items': []}
                sections.append(current)
                self.anchor_paths[(name, github_anchor(title))] = path
            elif current and (m := ITEM.match(line)):
                wm = WIKI_LINK.match(m['dest'])
                page = self.resolve(urllib.parse.unquote(wm['page'])) if wm else None
                current['items'].append((m['text'].strip(), m['dest'], page))
        return sections

    def assign(self, sections):
        for sec in sections:
            directory = posixpath.dirname(sec['path'])
            owned = []
            for text, dest, page in sec['items']:
                if page is None:
                    if WIKI_LINK.match(dest) and WIKI_LINK.match(dest)['abs']:
                        self.warnings.append(f'{sec["title"]}: 없는 wiki 페이지 링크 {urllib.parse.unquote(dest)}')
                    continue
                if page in self.paths:
                    continue  # 여러 섹션에 걸린 페이지는 처음 나온 곳에만 둔다
                stem = re.sub(r'^[^‐]{1,40}?-‐[\x00-\x1f]*-', '', page)
                self.paths[page] = f'{directory}/{self.unique(directory, slugify(stem))}.md'
                owned.append((text, page))
            sec['owned'] = owned

    def section_page(self, sec):
        lines = [f'# {sec["title"]}', '']
        if sec['ref']:
            lines += [f'🔗 [Reference]({sec["ref"]})', '']
        lines += [f'* [{text}]({dest})' for text, dest, _ in sec['items']]
        self.page(sec['path'], sec['title'], '\n'.join(lines))

    def claude_notes(self):
        """claude/의 노트를 번호순 → 번호 없는 노트 → Appendix 순으로 정렬한다."""
        notes = []
        for src in (self.repo / 'claude').glob('*.md'):
            if m := re.fullmatch(r'Appendix_(\d+)_(.+)', src.stem):
                order, title = (2, int(m[1])), f'Appendix {int(m[1])}. {m[2].replace("_", " ")}'
            elif m := re.fullmatch(r'(\d+)_(.+)', src.stem):
                order, title = (0, int(m[1])), m[2].replace('_', ' ')
            else:
                order, title = (1, 0), src.stem
            notes.append((order, title, src))
        return [(title, f'claude-code/{self.unique("claude-code", slugify(title))}.md', src)
                for _, title, src in sorted(notes, key=lambda n: (n[0], n[1]))]

    def build(self):
        categories = {}  # wiki 페이지 -> _Sidebar에 표시된 이름
        for text, dest in re.findall(rf'\[({TEXT})\]\(({DEST})\)', self.pages.get('_Sidebar', '')):
            name = self.resolve(dest)
            if name and name.startswith('Tech-') and name != 'Tech-Stack':
                categories.setdefault(name, text.strip())
        groups = [(label, self.parse_category(c, f'tech-stack/{slugify(c.removeprefix("Tech-"))}'))
                  for c, label in categories.items()]
        groups.append(('Books', self.parse_category('Books', 'books')))
        for _, sections in groups:
            self.assign(sections)

        self.paths.update({'Home': 'README.md', 'Tech-Stack': 'tech-stack/README.md', 'Books': 'books/README.md'})
        for c in categories:
            self.paths[c] = 'tech-stack/README.md'

        readme = self.repo / 'README.md'
        self.page('README.md', 'Backend Roadmap', readme.read_text(encoding='utf-8'), readme)
        for name in ('Tech-Stack', 'Books'):
            text = '\n'.join(l for l in self.pages[name].split('\n') if not NAV_LINE.match(l))
            self.page(self.paths[name], name.replace('-', ' '), text)

        summary = ['# Table of contents', '', '* [Backend Roadmap](README.md)',
                   '* [Tech Stack](tech-stack/README.md)', '* [Books](books/README.md)']
        for label, sections in groups:
            summary += ['', f'## {label}', '']
            for sec in sections:
                self.section_page(sec)
                summary.append(f'* [{sec["title"]}]({sec["path"]})')
                for text, page in sec['owned']:
                    self.page(self.paths[page], text, self.pages[page])
                    summary.append(f'  * [{text}]({self.paths[page]})')

        notes = self.claude_notes()
        if notes:
            summary += ['', '## Claude Code', '']
            for title, path, src in notes:
                self.page(path, title, src.read_text(encoding='utf-8'), src)
                summary.append(f'* [{title}]({path})')

        self.files['SUMMARY.md'] = '\n'.join(summary) + '\n'
        self.files['.gitbook.yaml'] = 'root: ./\n\nstructure:\n  readme: README.md\n  summary: SUMMARY.md\n'
        # 사이트 단위 Git Sync가 읽는 설정. key는 GitBook이 space를 식별하는 값이라 연결 후에는 바꾸지 않는다.
        self.files['gitbook-docs.yaml'] = (
            '$schema: https://api.gitbook.com/gitbook-docs.yaml\n'
            'site:\n'
            '  title: Backend Roadmap\n'
            '  structure:\n'
            '    - type: space\n'
            '      key: backend-roadmap\n'
            '      title: Backend Roadmap\n'
            '      path: backend-roadmap\n'
            '      default: true\n'
            '      content:\n'
            '        directory: ./\n'
        )

    def write(self):
        if self.out.exists():
            if (self.out / '.git').exists() or self.out in (self.repo, *self.repo.parents):
                raise SystemExit(f'출력 디렉터리로 쓸 수 없습니다: {self.out}')
            shutil.rmtree(self.out)
        for rel, content in self.files.items():
            dst = self.out / rel
            dst.parent.mkdir(parents=True, exist_ok=True)
            if isinstance(content, Path):
                shutil.copyfile(content, dst)
            else:
                dst.write_text(content, encoding='utf-8')


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--repo', type=Path, default=Path(__file__).resolve().parent.parent)
    parser.add_argument('--wiki', type=Path, required=True, help='wiki 저장소를 clone한 디렉터리')
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--wiki-url', default=DEFAULT_WIKI_URL, help='제외된 페이지 링크에 쓸 wiki 주소')
    args = parser.parse_args()

    builder = Builder(args.repo.resolve(), args.wiki.resolve(), args.out.resolve(), args.wiki_url)
    builder.build()
    builder.write()

    pages = sum(1 for p in builder.files if p.endswith('.md'))
    skipped = [p for p in builder.pages if p not in builder.paths and p != '_Sidebar']
    print(f'wiki {builder.wiki_sha[:7]}: 페이지 {pages}개 생성, 목차에 없는 wiki 페이지 {len(skipped)}개 제외')
    for w in dict.fromkeys(builder.warnings):
        print(f'warning: {w}', file=sys.stderr)


if __name__ == '__main__':
    main()
