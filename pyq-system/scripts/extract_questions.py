#!/usr/bin/env python3
"""Extract page text from an approved PDF and create a review draft.

This deliberately does not guess answer keys or silently publish parsed items.
OCR is attempted only when a page has very little embedded text.
"""
from __future__ import annotations
import argparse, json, re
from datetime import datetime, timezone
from pathlib import Path

import fitz

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--input", required=True)
    ap.add_argument("--exam", required=True)
    ap.add_argument("--year", required=True)
    ap.add_argument("--source-url", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--ocr", action="store_true", help="Use Tesseract for pages with little embedded text")
    ap.add_argument("--language", default="eng+hin", help="Tesseract language packs, e.g. eng+hin+mar")
    args = ap.parse_args()

    path = Path(args.input)
    doc = fitz.open(path)
    pages = []
    ocr_used = False
    for i, page in enumerate(doc):
        text = page.get_text("text").strip()
        method = "embedded_text"
        if len(re.sub(r"\s+", "", text)) < 35 and args.ocr:
            try:
                import pytesseract
                from PIL import Image
                pix = page.get_pixmap(matrix=fitz.Matrix(2, 2), alpha=False)
                import io
                image = Image.open(io.BytesIO(pix.tobytes("png")))
                text = pytesseract.image_to_string(image, lang=args.language).strip()
                method = "tesseract_ocr"
                ocr_used = True
            except Exception as exc:
                method = "ocr_failed"
                text = text + "\n[OCR ERROR: " + str(exc)[:180] + "]"
        pages.append({"page": i + 1, "method": method, "text": text})

    output = {
        "schemaVersion": 1,
        "exam": args.exam,
        "year": str(args.year),
        "sourceUrl": args.source_url,
        "sourceFile": path.name,
        "extractedAt": datetime.now(timezone.utc).isoformat(),
        "ocrUsed": ocr_used,
        "reviewStatus": "needs_human_review",
        "answerKeyStatus": "not_verified",
        "reusePermission": "not_checked",
        "pages": pages
    }
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(output, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Draft created: {out} ({len(pages)} pages). Nothing was published.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
