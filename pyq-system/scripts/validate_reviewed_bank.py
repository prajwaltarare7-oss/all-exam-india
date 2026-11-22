#!/usr/bin/env python3
"""Promote only explicitly reviewed records into the eligible bank."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"data"/"reviewed-questions.json"
OUT=ROOT/"data"/"verified-questions.json"
REQUIRED=("exam","year","subject","question","options","correctOption","sourceUrl")
def main():
    src=json.loads(SRC.read_text(encoding="utf-8"))
    good=[]; rejected=[]
    for i,q in enumerate(src.get("questions",[])):
        missing=[k for k in REQUIRED if not q.get(k)]
        if q.get("reviewStatus")!="verified": missing.append("reviewStatus=verified")
        if q.get("answerKeyStatus")!="verified": missing.append("answerKeyStatus=verified")
        if q.get("reusePermission")!="approved": missing.append("reusePermission=approved")
        if not isinstance(q.get("options"),list) or len(q["options"])<2: missing.append("at least 2 options")
        if missing: rejected.append({"index":i,"missing":missing})
        else: good.append(q)
    OUT.write_text(json.dumps({"version":1,"questions":good},ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    report={"approvedCount":len(good),"rejectedCount":len(rejected),"rejected":rejected,
            "note":"Only explicitly reviewed, answer-verified, reuse-approved records enter the eligible question bank."}
    (ROOT/"data"/"bank-validation-report.json").write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"approved":len(good),"rejected":len(rejected)}))
if __name__=="__main__": main()
