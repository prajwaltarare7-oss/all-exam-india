#!/usr/bin/env python3
"""OCR PDFs explicitly listed in the owner-reviewed intake manifest.

This never downloads arbitrary discovered links or publishes OCR output.
"""
from __future__ import annotations
import json, subprocess, sys
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "intake" / "manifest.json"
INTAKE = ROOT / "intake" / "approved"
OUT = ROOT / "data" / "ocr-drafts"
REPORT = ROOT / "data" / "intake-report.json"

def main() -> int:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    rows, errors = [], []
    OUT.mkdir(parents=True, exist_ok=True)
    for item in manifest.get("files", []):
        try:
            if item.get("reusePermission") != "approved" or item.get("sourceReviewed") is not True:
                raise ValueError("source/reuse approval is missing")
            name = Path(item["file"]).name
            pdf = (INTAKE / name).resolve()
            if pdf.parent != INTAKE.resolve() or not pdf.is_file() or pdf.suffix.lower() != ".pdf":
                raise ValueError("PDF must exist directly inside pyq-system/intake/approved/")
            source_url = item["sourceUrl"]
            host = urlparse(source_url).hostname or ""
            allowed = item.get("approvedSourceDomains", [])
            if not any(host == d or host.endswith("." + d) for d in allowed):
                raise ValueError("source URL host is not in approvedSourceDomains")
            target = OUT / (pdf.stem + ".ocr-draft.json")
            cmd = [sys.executable, str(ROOT / "scripts" / "extract_questions.py"),
                   "--input", str(pdf), "--exam", item["exam"], "--year", str(item["year"]),
                   "--source-url", source_url, "--out", str(target), "--ocr",
                   "--language", item.get("ocrLanguage", "eng+hin+mar")]
            subprocess.run(cmd, check=True)
            rows.append({"file": name, "draft": str(target.relative_to(ROOT)), "status": "needs_human_review"})
        except Exception as exc:
            errors.append({"file": item.get("file", "unknown"), "error": str(exc)})
    report = {"processedAt": datetime.now(timezone.utc).isoformat(), "processed": rows, "errors": errors,
              "note": "OCR drafts are not question-bank entries and are not published to the website."}
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"processed": len(rows), "errors": errors}, ensure_ascii=False))
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
