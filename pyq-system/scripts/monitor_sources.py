#!/usr/bin/env python3
"""Discover paper/answer-key PDF links on configured official exam portals.

This monitor is intentionally link-only. It does not bypass access controls or
download/re-host question papers. A human must check source terms and reuse
permissions before any paper is imported into the question bank.
"""
from __future__ import annotations
import json, re, sys
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urljoin, urlparse

import requests
from bs4 import BeautifulSoup

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / "sources.json"
OUT = ROOT / "data" / "source-discoveries.json"
KEYWORDS = re.compile(r"(question|paper|answer.?key|previous.?year|pyq|examination)", re.I)
PDF_RE = re.compile(r"\.pdf(?:$|[?#])", re.I)
HEADERS = {"User-Agent": "ALL-EXAM-INDIA-PYQ-Monitor/1.0 (public source-link discovery)"}

def allowed(url: str, domains: list[str]) -> bool:
    host = (urlparse(url).hostname or "").lower()
    return any(host == d or host.endswith("." + d) for d in domains)

def main() -> int:
    config = json.loads(SOURCES.read_text(encoding="utf-8"))
    existing = {}
    if OUT.exists():
        try:
            old = json.loads(OUT.read_text(encoding="utf-8"))
            existing = {x["url"]: x for x in old.get("discoveries", [])}
        except Exception:
            existing = {}
    now = datetime.now(timezone.utc).isoformat()
    errors = []
    for source in config.get("sources", []):
        if not source.get("enabled"):
            continue
        try:
            response = requests.get(source["url"], headers=HEADERS, timeout=25)
            response.raise_for_status()
            soup = BeautifulSoup(response.text, "html.parser")
            for a in soup.find_all("a", href=True):
                url = urljoin(response.url, a["href"].strip())
                title = " ".join(a.get_text(" ", strip=True).split())[:240]
                if not allowed(url, source.get("allowedDomains", [])):
                    continue
                if not PDF_RE.search(url) and not KEYWORDS.search(title + " " + url):
                    continue
                if not PDF_RE.search(url):
                    continue
                row = existing.get(url, {
                    "url": url, "firstSeen": now, "status": "needs_source_and_reuse_review"
                })
                row.update({
                    "sourceId": source["id"], "sourceName": source["name"],
                    "examFamilies": source.get("examFamilies", []),
                    "title": title or "PDF link (title not provided)",
                    "lastSeen": now, "downloadPolicy": source.get("downloadPolicy", "link_only")
                })
                existing[url] = row
        except Exception as exc:
            errors.append({"sourceId": source.get("id"), "error": str(exc)[:300]})
    OUT.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "checkedAt": now,
        "discoveryCount": len(existing),
        "discoveries": sorted(existing.values(), key=lambda x: x.get("lastSeen", ""), reverse=True),
        "errors": errors
    }
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"discoveryCount": len(existing), "errors": errors}, ensure_ascii=False))
    return 0

if __name__ == "__main__":
    sys.exit(main())
