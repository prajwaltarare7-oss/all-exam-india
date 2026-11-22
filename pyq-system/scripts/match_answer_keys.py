#!/usr/bin/env python3
"""Match supplied answer-key JSON to OCR candidates; matches remain review-only."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
CAND=ROOT/"data"/"question-candidates.json"
KEYDIR=ROOT/"intake"/"answer-keys"
OUT=ROOT/"data"/"answer-key-matches.json"
def main():
    data=json.loads(CAND.read_text(encoding="utf-8")) if CAND.exists() else {"questions":[]}
    keys={}
    for f in KEYDIR.glob("*.json"):
        k=json.loads(f.read_text(encoding="utf-8"))
        for qno,answer in k.get("answers",{}).items():
            keys[(k.get("exam"),str(k.get("year")),str(qno))]=str(answer).upper()
    matched=0
    for q in data.get("questions",[]):
        key=(q.get("exam"),str(q.get("year")),str(q.get("questionNo")))
        answer=keys.get(key)
        if answer:
            q["answerKeyMatch"]=answer
            q["answerKeyStatus"]="matched_needs_review"
            matched+=1
    OUT.parent.mkdir(parents=True,exist_ok=True)
    OUT.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(f"Matched {matched} answer keys; human verification is still required.")
if __name__=="__main__": main()
