#!/usr/bin/env python3
"""Publish only verified/reuse-approved question records to Firebase."""
import json, os, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def main():
    raw=os.environ.get("FIREBASE_SERVICE_ACCOUNT_JSON")
    if not raw:
        print("Firebase publishing skipped: FIREBASE_SERVICE_ACCOUNT_JSON secret is not configured.")
        return 0
    try:
        import firebase_admin
        from firebase_admin import credentials, firestore, storage
    except Exception as exc:
        raise SystemExit("Firebase Admin SDK unavailable: "+str(exc))
    service=json.loads(raw)
    bucket_name=os.environ.get("FIREBASE_STORAGE_BUCKET","").strip()
    options={"storageBucket":bucket_name} if bucket_name else None
    if not firebase_admin._apps:
        firebase_admin.initialize_app(credentials.Certificate(service), options)
    db=firestore.client()
    bank=json.loads((ROOT/"data"/"verified-questions.json").read_text(encoding="utf-8"))
    batch=db.batch(); count=0
    for q in bank.get("questions",[]):
        if q.get("reviewStatus")!="verified" or q.get("answerKeyStatus")!="verified" or q.get("reusePermission")!="approved":
            continue
        import re
        exam=re.sub(r"[^a-z0-9]+","-",str(q["exam"]).strip().lower()).strip("-")
        qid=str(q.get("id") or q.get("candidateId") or f'{q["year"]}-{q.get("questionNo","q")}-{count}')
        ref=db.collection("questionBank").document(exam).collection("questions").document(qid)
        payload=dict(q); payload["published"]=True
        batch.set(ref,payload,merge=True); count+=1
        if count%400==0: batch.commit(); batch=db.batch()
    if count%400: batch.commit()
    print(f"Published {count} verified questions to Firestore.")
    # Publish independently authored practice items to a separate collection; never label them as PYQs.
    original_path=ROOT/"data"/"verified-original-questions.json"
    original_count=0
    if original_path.exists():
        originals=json.loads(original_path.read_text(encoding="utf-8"))
        for q in originals.get("questions",[]):
            if (q.get("contentType")!="original_practice" or q.get("authorship")!="original"
                or q.get("originalityReviewStatus")!="verified" or q.get("reviewStatus")!="verified"
                or q.get("answerKeyStatus")!="verified" or q.get("reusePermission")!="owned"):
                continue
            import re
            exam=re.sub(r"[^a-z0-9]+","-",str(q["exam"]).strip().lower()).strip("-")
            qid=str(q.get("id") or q.get("candidateId") or f'original-{original_count}')
            ref=db.collection("practiceBank").document(exam).collection("questions").document(qid)
            payload=dict(q); payload["published"]=True; payload["contentType"]="original_practice"
            ref.set(payload,merge=True); original_count+=1
    print(f"Published {original_count} independently authored practice questions to practiceBank (not PYQ).")
    # Publish only complete mock papers produced from verified profiles and verified questions.
    mocks_dir=ROOT/"data"/"generated-mocks"
    test_count=0
    if mocks_dir.exists():
        for file in sorted(mocks_dir.glob("*.json")):
            try:
                test=json.loads(file.read_text(encoding="utf-8"))
                if test.get("status")!="generated_not_yet_published" or not test.get("testId"):
                    continue
                exam=str(test.get("exam",""))
                questions=test.get("questions",[])
                if not exam or not questions or len(questions)!=int(test.get("totalQuestions",0)):
                    continue
                if any(q.get("reviewStatus")!="verified" or q.get("answerKeyStatus")!="verified" or q.get("reusePermission")!="approved" for q in questions):
                    continue
                import re
                exam_id=re.sub(r"[^a-z0-9]+","-",exam.strip().lower()).strip("-")
                ref=db.collection("questionBank").document(exam_id).collection("tests").document(str(test["testId"]))
                payload=dict(test); payload["published"]=True; payload["publishedAt"]=firestore.SERVER_TIMESTAMP
                ref.set(payload,merge=True); test_count+=1
            except (ValueError, KeyError, TypeError) as exc:
                print(f"Skipping invalid generated test {file.name}: {exc}")
    print(f"Published {test_count} complete verified mock tests to Firestore.")
    if bucket_name:
        manifest=json.loads((ROOT/"intake"/"manifest.json").read_text(encoding="utf-8"))
        bucket=storage.bucket()
        uploaded=0
        for item in manifest.get("files",[]):
            if item.get("reusePermission")!="approved" or item.get("sourceReviewed") is not True: continue
            path=ROOT/"intake"/"approved"/Path(item["file"]).name
            if path.is_file() and path.suffix.lower()==".pdf":
                blob=bucket.blob("approved-papers/"+Path(item["file"]).name)
                blob.upload_from_filename(str(path),content_type="application/pdf")
                uploaded+=1
        print(f"Uploaded {uploaded} approved PDFs to Firebase Storage.")
    else:
        print("PDF Storage upload skipped: FIREBASE_STORAGE_BUCKET secret is not configured.")
    return 0
if __name__=="__main__": sys.exit(main())
