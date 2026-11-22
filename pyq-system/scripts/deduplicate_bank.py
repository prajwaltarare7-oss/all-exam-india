#!/usr/bin/env python3
"""Mark exact normalized duplicate candidates; retain first occurrence."""
import json,re,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"data"/"answer-key-matches.json"
OUT=ROOT/"data"/"deduplicated-question-candidates.json"
def norm(s): return re.sub(r"\W+","",str(s).casefold(),flags=re.UNICODE)
def main():
    data=json.loads(SRC.read_text(encoding="utf-8")) if SRC.exists() else {"questions":[]}
    seen={}
    for q in data.get("questions",[]):
        key=(q.get("exam"),norm(q.get("question","")))
        digest=hashlib.sha256(repr(key).encode()).hexdigest()
        q["duplicateOf"]=seen.get(key)
        q["duplicateStatus"]="duplicate" if key in seen else "unique"
        q["candidateId"]=digest[:16]
        if key not in seen: seen[key]=q["candidateId"]
    OUT.parent.mkdir(parents=True,exist_ok=True)
    OUT.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(f"Checked {len(data.get('questions',[]))} candidates.")
if __name__=="__main__": main()
