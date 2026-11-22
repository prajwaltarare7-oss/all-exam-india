#!/usr/bin/env python3
"""Build PDF intake automatically from discovered links whose source reuse was explicitly approved.

No source is treated as licensed merely because it is public. Enable autoIntake only
after checking the source's terms/permission. Requires exam/year metadata to be inferable.
"""
from __future__ import annotations
import json, re
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / "sources.json"
DISCOVERIES = ROOT / "data" / "source-discoveries.json"
MANIFEST = ROOT / "intake" / "manifest.json"

def main():
    source_cfg = json.loads(SOURCES.read_text(encoding="utf-8"))
    discoveries = json.loads(DISCOVERIES.read_text(encoding="utf-8")).get("discoveries", [])
    source_by_id = {s["id"]: s for s in source_cfg.get("sources", [])}
    existing = json.loads(MANIFEST.read_text(encoding="utf-8")) if MANIFEST.exists() else {"files": []}
    rows = {x.get("downloadUrl"): x for x in existing.get("files", []) if x.get("downloadUrl")}
    skipped = 0
    for d in discoveries:
        s = source_by_id.get(d.get("sourceId"), {})
        if not (s.get("autoIntake") is True and s.get("reusePermission") == "approved"
                and s.get("downloadPolicy") == "download_and_ocr"
                and s.get("permissionEvidenceUrl") and s.get("permissionBasis")
                and s.get("permissionReviewedAt")):
            skipped += 1
            continue
        exam = s.get("autoExam") or (d.get("examFamilies") or [None])[0]
        text = " ".join([d.get("title", ""), d.get("url", "")])
        year_match = re.search(r"20(?:0[0-9]|1[0-9]|2[0-9])", text)
        year = s.get("autoYear") or (year_match.group(0) if year_match else None)
        url = d.get("url", "")
        host = (urlparse(url).hostname or "").lower()
        domains = s.get("allowedDomains", [])
        if not exam or not year or not any(host == x or host.endswith("." + x) for x in domains):
            skipped += 1
            continue
        filename = re.sub(r"[^A-Za-z0-9._-]+", "-", f"{exam}-{year}-{Path(urlparse(url).path).name}")[:150]
        if not filename.lower().endswith(".pdf"):
            filename += ".pdf"
        rows[url] = {
            "file": filename, "downloadUrl": url, "sourceUrl": s["url"],
            "exam": exam, "year": str(year), "ocrLanguage": s.get("ocrLanguage", "eng+hin+mar"),
            "sourceReviewed": True, "reusePermission": "approved",
            "approvedSourceDomains": domains, "autoDiscovered": True
        }
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text(json.dumps({"version": 1, "files": list(rows.values())}, ensure_ascii=False, indent=2) + "
", encoding="utf-8")
    print(json.dumps({"manifestFiles": len(rows), "notAutoIntaked": skipped}, ensure_ascii=False))

if __name__ == "__main__":
    main()
