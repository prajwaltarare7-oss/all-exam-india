#!/usr/bin/env python3
"""Conservatively split OCR text into question candidates; never auto-verify."""
from __future__ import annotations
import json, re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
DRAFTS=ROOT/"data"/"ocr-drafts"
OUT=ROOT/"data"/"question-candidates.json"
QSTART=re.compile(r"^\s*(?:Q(?:uestion)?\s*)?(\d{1,3})\s*[\).:-]\s*(.+)$",re.I)
OPT=re.compile(r"^\s*[\(\[]?([A-Da-d])[\)\].:-]\s*(.+)$")
def main():
    candidates=[]
    for file in sorted(DRAFTS.glob("*.ocr-draft.json")):
        data=json.loads(file.read_text(encoding="utf-8"))
        current=None
        for page in data.get("pages",[]):
            for line in page.get("text","").splitlines():
                line=" ".join(line.split())
                if not line: continue
                q=QSTART.match(line)
                o=OPT.match(line)
                if q:
                    if current: candidates.append(current)
                    current={"exam":data.get("exam"),"year":str(data.get("year")),"sourceUrl":data.get("sourceUrl"),
                             "sourceFile":data.get("sourceFile"),"questionNo":int(q.group(1)),
                             "question":q.group(2),"options":[],"page":page.get("page"),
                             "reviewStatus":"needs_human_review","answerKeyStatus":"not_verified",
                             "reusePermission":data.get("reusePermission","not_checked")}
                elif o and current:
                    current["options"].append({"key":o.group(1).upper(),"text":o.group(2)})
                elif current and not current["options"]:
                    current["question"] += " " + line
                elif current and current["options"]:
                    current["options"][-1]["text"] += " " + line
        if current: candidates.append(current)
    OUT.parent.mkdir(parents=True,exist_ok=True)
    OUT.write_text(json.dumps({"version":1,"count":len(candidates),"questions":candidates},ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(f"Created {len(candidates)} review candidates; none are verified.")
if __name__=="__main__": main()
