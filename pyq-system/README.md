# ALL EXAM INDIA — PYQ automation

## What runs automatically
- GitHub Actions runs every 6 hours (and can be started manually).
- The official-source monitor checks configured exam-authority pages and records PDF links from allowed official domains.
- Owner-approved PDFs listed in pyq-system/intake/manifest.json are processed with embedded-text extraction and Tesseract OCR fallback (English, Hindi and Marathi language packs are installed in the workflow).
- Mock drafts are generated automatically for every exam profile marked "verified", but only when enough questions in verified-questions.json have verified answers and approved reuse permission.
- Each run saves source discoveries, OCR drafts, generated mocks and a pipeline report as a downloadable GitHub Actions artifact.

## Important limits (intentional)
- The collector does not bypass logins, CAPTCHAs, access controls or download restrictions.
- It does not automatically download/re-host public PDFs because public availability is not permission to republish.
- OCR produces page-text drafts; it does not reliably split every page into question/option/answer records. A human must check OCR, question boundaries, options, answers and source/reuse rights before adding records to the verified question bank.
- Mock generation does not invent questions and does not run until the exam profile is officially checked and enough eligible questions exist.
- Website/Firebase publishing is not configured. No Firebase secret or secure publishing endpoint is currently available in this repository, so this workflow does not claim to upload questions to the live website. Configure a server-side Firebase credential and reviewed Firestore rules before enabling that separate step.

## Add an approved PDF
See pyq-system/intake/README.md and edit pyq-system/intake/manifest.json. Keep the PDF in pyq-system/intake/approved/.

## Run locally
```bash
python -m pip install -r pyq-system/requirements.txt
python pyq-system/scripts/monitor_sources.py
python pyq-system/scripts/process_intake.py
python pyq-system/scripts/generate_all_mocks.py
```
Local OCR requires Tesseract plus the relevant language packs.
