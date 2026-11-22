#!/usr/bin/env python3
"""Discover publicly linked PDF/answer-key resources on configured official portals.

This is link discovery only: it does not bypass logins, CAPTCHAs, robots restrictions,
or infer permission to download/re-publish documents.
"""
from __future__ import annotations
import json, re, sys, time
from collections import deque
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urljoin, urlparse, urldefrag

import requests
from bs4 import BeautifulSoup

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / "sources.json"
OUT = ROOT / "data" / "source-discoveries.json"
KEYWORDS = re.compile(r"(question|paper|answer.?key|previous.?year|pyq|examination|archive|notice|recruitment)", re.I)
PDF_RE = re.compile(r"\.pdf(?:$|[?#])", re.I)
HEADERS = {"User-Agent": "ALL-EXAM-INDIA-PYQ-Monitor/1.1 (+public official-link discovery)"}
MAX_PAGES_PER_SOURCE = 25
MAX_DEPTH = 2

def allowed(url: str, domains: list[str]) -> bool:
    host = (urlparse(url).hostname or "").lower()
    return urlparse(url).scheme in ("http", "https") and any(host == d or host.endswith("." + d) for d in domains)

def clean(url: str) -> str:
    return urldefrag(url)[0]

def main() -> int:
    config = json.loads(SOURCES.read_text(encoding="utf-8"))
    existing = {}
    if OUT.exists():
        try:
            old = json.loads(OUT.read_text(encoding="utf-8"))
            existing = {x["url"]: x for x in old.get("discoveries", [])}
        except Exception:
            pass
    now = datetime.now(timezone.utc).isoformat()
    errors, pages_checked = [], 0
    session = requests.Session()
    session.headers.update(HEADERS)

    for source in config.get("sources", []):
        if not source.get("enabled"):
            continue
        domains = source.get("allowedDomains", [])
        queue = deque([(source["url"], 0)])
        visited = set()
        checked = 0
        while queue and checked < MAX_PAGES_PER_SOURCE:
            page_url, depth = queue.popleft()
            page_url = clean(page_url)
            if page_url in visited or not allowed(page_url, domains):
                continue
            visited.add(page_url)
            try:
                response = session.get(page_url, timeout=20)
                response.raise_for_status()
                if "text/html" not in response.headers.get("Content-Type", "").lower():
                    continue
                checked += 1
                pages_checked += 1
                soup = BeautifulSoup(response.text, "html.parser")
                # Some official portals expose PDF links through embedded viewers or
                # data attributes rather than ordinary anchor hrefs. Inspect common URL
                # attributes while remaining strictly inside configured official domains.
                candidates = []
                for tag in soup.find_all(True):
                    title = " ".join(tag.get_text(" ", strip=True).split())[:240]
                    for attr in ("href", "src", "data", "data-href", "data-url", "data-src"):
                        raw = tag.get(attr)
                        if isinstance(raw, str) and raw.strip():
                            candidates.append((raw.strip(), title))
                for raw_url, title in candidates:
                    url = clean(urljoin(response.url, raw_url))
                    if not allowed(url, domains):
                        continue
                    context = title + " " + url
                    if PDF_RE.search(url):
                        row = existing.get(url, {"url": url, "firstSeen": now, "status": "needs_source_and_reuse_review"})
                        row.update({
                            "sourceId": source["id"], "sourceName": source["name"],
                            "examFamilies": source.get("examFamilies", []),
                            "title": title or "Official PDF link (title not provided)",
                            "landingPage": response.url, "lastSeen": now,
                            "downloadPolicy": source.get("downloadPolicy", "link_only")
                        })
                        existing[url] = row
                    elif depth < MAX_DEPTH and KEYWORDS.search(context):
                        queue.append((url, depth + 1))
                time.sleep(0.35)
            except Exception as exc:
                errors.append({"sourceId": source.get("id"), "page": page_url, "error": str(exc)[:240]})

    OUT.parent.mkdir(parents=True, exist_ok=True)
    discoveries = sorted(existing.values(), key=lambda x: x.get("lastSeen", ""), reverse=True)
    OUT.write_text(json.dumps({
        "checkedAt": now, "pagesChecked": pages_checked,
        "discoveryCount": len(discoveries), "discoveries": discoveries, "errors": errors,
        "note": "Discovered links are not automatically licensed for download, OCR storage, or republication."
    }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"pagesChecked": pages_checked, "discoveryCount": len(discoveries), "errors": len(errors)}, ensure_ascii=False))
    return 0

if __name__ == "__main__":
    sys.exit(main())
