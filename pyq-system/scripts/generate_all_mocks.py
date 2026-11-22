#!/usr/bin/env python3
"""Generate mock drafts for verified exam profiles when the bank has enough approved questions."""
from __future__ import annotations
import json, subprocess, sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PROFILES = ROOT / "exam-profiles.json"
BANK = ROOT / "data" / "verified-questions.json"
OUT = ROOT / "data" / "generated-mocks"
REPORT = ROOT / "data" / "pipeline-report.json"

def main() -> int:
    profiles = json.loads(PROFILES.read_text(encoding="utf-8")).get("profiles", {})
    OUT.mkdir(parents=True, exist_ok=True)
    generated, skipped = [], []
    for exam, profile in profiles.items():
        if profile.get("status") != "verified":
            skipped.append({"exam": exam, "reason": "exam profile not officially verified"})
            continue
        cmd = [sys.executable, str(ROOT / "scripts" / "generate_mock.py"),
               "--exam", exam, "--bank", str(BANK), "--profiles", str(PROFILES), "--out", str(OUT)]
        result = subprocess.run(cmd, text=True, capture_output=True)
        if result.returncode == 0:
            generated.append({"exam": exam, "output": result.stdout.strip()})
        else:
            skipped.append({"exam": exam, "reason": result.stderr.strip() or result.stdout.strip()})
    report = {"generatedAt": datetime.now(timezone.utc).isoformat(), "generated": generated, "skipped": skipped,
              "note": "Mocks are drafts. Only verified, answer-checked and reuse-approved questions are eligible; no website publishing occurs here."}
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"generated": len(generated), "skipped": len(skipped)}, ensure_ascii=False))
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
