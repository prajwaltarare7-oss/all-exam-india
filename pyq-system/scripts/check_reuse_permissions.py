#!/usr/bin/env python3
"""Audit explicit source reuse permissions; never infer a licence from public access."""
import json
from datetime import datetime, timezone
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
CFG=ROOT/"sources.json"
OUT=ROOT/"data"/"reuse-rights-report.json"
def main():
    cfg=json.loads(CFG.read_text(encoding="utf-8"))
    rows=[]
    for s in cfg.get("sources",[]):
        approved=(s.get("reusePermission")=="approved"
                  and bool(s.get("permissionEvidenceUrl"))
                  and bool(s.get("permissionBasis"))
                  and bool(s.get("permissionReviewedAt")))
        rows.append({
            "sourceId":s.get("id"),"sourceName":s.get("name"),
            "reusePermission":s.get("reusePermission","not_reviewed"),
            "permissionEvidenceUrl":s.get("permissionEvidenceUrl"),
            "permissionBasis":s.get("permissionBasis"),
            "permissionReviewedAt":s.get("permissionReviewedAt"),
            "canAutoCopyText":approved,
            "reason":"explicit permission evidence recorded" if approved else
                     "blocked: record an official licence/permission URL, its applicable reuse terms, and review date"
        })
    report={"checkedAt":datetime.now(timezone.utc).isoformat(),
            "sourcesChecked":len(rows),"sourcesApprovedForCopy":sum(x["canAutoCopyText"] for x in rows),
            "sourcesBlocked":sum(not x["canAutoCopyText"] for x in rows),"sources":rows,
            "note":"This is a metadata gate, not a legal opinion. A human must verify that the cited terms actually cover copying, OCR, storage and republication of the specific material."}
    OUT.parent.mkdir(parents=True,exist_ok=True)
    OUT.write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"sourcesChecked":len(rows),"sourcesApprovedForCopy":report["sourcesApprovedForCopy"],"sourcesBlocked":report["sourcesBlocked"]}))
if __name__=="__main__": main()
