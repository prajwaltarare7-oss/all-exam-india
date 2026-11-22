#!/usr/bin/env python3
"""Download only PDFs explicitly approved for reuse in the intake manifest."""
import json, re, sys
from pathlib import Path
from urllib.parse import urlparse
import requests
ROOT=Path(__file__).resolve().parents[1]
MANIFEST=ROOT/"intake"/"manifest.json"
DEST=ROOT/"intake"/"approved"
MAX_BYTES=50*1024*1024
def host_allowed(url,domains):
    host=(urlparse(url).hostname or "").lower()
    return any(host==d.lower() or host.endswith("."+d.lower()) for d in domains)
def main():
    data=json.loads(MANIFEST.read_text(encoding="utf-8"))
    DEST.mkdir(parents=True,exist_ok=True)
    downloaded=[]; skipped=[]; errors=[]
    for item in data.get("files",[]):
        url=item.get("downloadUrl","")
        if not url:
            skipped.append({"file":item.get("file"),"reason":"no downloadUrl"}); continue
        if item.get("reusePermission")!="approved" or item.get("sourceReviewed") is not True:
            skipped.append({"file":item.get("file"),"reason":"reuse/source approval missing"}); continue
        domains=item.get("approvedSourceDomains",[])
        if not host_allowed(url,domains):
            errors.append({"file":item.get("file"),"error":"download host not approved"}); continue
        name=Path(item.get("file","")).name
        if not name or not name.lower().endswith(".pdf"):
            errors.append({"file":item.get("file"),"error":"file must be a .pdf filename"}); continue
        target=DEST/name
        try:
            with requests.get(url,stream=True,timeout=35,headers={"User-Agent":"ALL-EXAM-INDIA-approved-PDF-intake/1.0"}) as resp:
                resp.raise_for_status()
                final=resp.url
                if not host_allowed(final,domains): raise ValueError("redirected outside approved domains")
                ctype=resp.headers.get("Content-Type","").lower()
                if "pdf" not in ctype and not final.lower().split("?")[0].endswith(".pdf"):
                    raise ValueError("response does not appear to be a PDF")
                total=0
                with target.open("wb") as out:
                    for chunk in resp.iter_content(1024*128):
                        if not chunk: continue
                        total+=len(chunk)
                        if total>MAX_BYTES: raise ValueError("PDF exceeds 50 MB limit")
                        out.write(chunk)
                if total<5 or target.read_bytes()[:5]!=b"%PDF-":
                    target.unlink(missing_ok=True); raise ValueError("downloaded file is not a valid PDF header")
            downloaded.append({"file":name,"bytes":total})
        except Exception as exc:
            target.unlink(missing_ok=True)
            errors.append({"file":name,"error":str(exc)[:240]})
    report={"downloaded":downloaded,"skipped":skipped,"errors":errors}
    (ROOT/"data"/"download-report.json").write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"downloaded":len(downloaded),"skipped":len(skipped),"errors":errors},ensure_ascii=False))
if __name__=="__main__": main()
