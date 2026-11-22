#!/usr/bin/env python3
"""Validate two distinct lanes: reusable PYQs and independently authored practice questions."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "data" / "reviewed-questions.json"
PYQ_OUT = ROOT / "data" / "verified-questions.json"
ORIGINAL_OUT = ROOT / "data" / "verified-original-questions.json"
REPORT_OUT = ROOT / "data" / "bank-validation-report.json"

COMMON = ("exam", "subject", "question", "options", "correctOption")
def valid_options(q):
    return isinstance(q.get("options"), list) and len(q["options"]) >= 2

def main():
    src = json.loads(SRC.read_text(encoding="utf-8"))
    pyqs, originals, rejected = [], [], []
    for i, q in enumerate(src.get("questions", [])):
        missing = [k for k in COMMON if not q.get(k)]
        if not valid_options(q): missing.append("at least 2 options")
        if q.get("reviewStatus") != "verified": missing.append("reviewStatus=verified")
        if q.get("answerKeyStatus") != "verified": missing.append("answerKeyStatus=verified")
        kind = q.get("contentType", "pyq")
        if kind == "pyq":
            for key in ("year", "sourceUrl"):
                if not q.get(key): missing.append(key)
            if q.get("reusePermission") != "approved":
                missing.append("reusePermission=approved with evidence")
            if not q.get("permissionEvidenceUrl") or not q.get("permissionBasis") or not q.get("permissionReviewedAt"):
                missing.append("complete reuse permission evidence")
            if not missing: pyqs.append(q)
        elif kind == "original_practice":
            if q.get("authorship") != "original": missing.append("authorship=original")
            if q.get("sourceUse") != "topic_only_no_question_text":
                missing.append("sourceUse=topic_only_no_question_text")
            if q.get("originalityReviewStatus") != "verified":
                missing.append("originalityReviewStatus=verified")
            if q.get("reusePermission") != "owned":
                missing.append("reusePermission=owned")
            if not missing: originals.append(q)
        else:
            missing.append("contentType must be pyq or original_practice")
        if missing: rejected.append({"index": i, "contentType": kind, "missing": sorted(set(missing))})

    for path, rows in ((PYQ_OUT, pyqs), (ORIGINAL_OUT, originals)):
        path.write_text(json.dumps({"version": 2, "questions": rows}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    report = {
        "verifiedPyqCount": len(pyqs),
        "verifiedOriginalPracticeCount": len(originals),
        "rejectedCount": len(rejected),
        "rejected": rejected,
        "note": "PYQs require documented reuse permission. Original practice items must be independently authored from topic-level learning objectives, not paraphrased or lightly edited from protected question text."
    }
    REPORT_OUT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"verifiedPyq": len(pyqs), "verifiedOriginalPractice": len(originals), "rejected": len(rejected)}))

if __name__ == "__main__":
    main()
