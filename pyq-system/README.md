# ALL EXAM INDIA — Universal PYQ System

This is the first foundation commit for the 10-part PYQ workflow. The legacy SSC GD and Maharashtra Police Bharti practice/mock pages have been removed from the public homepage while verified content is rebuilt.

## The 10 modules

1. Official source monitor — checks configured exam-authority pages for paper/answer-key links.
2. Bulk PDF intake — accepts only files whose source and reuse conditions have been checked.
3. Text extraction + OCR — extracts embedded PDF text and uses OCR for scanned pages.
4. Answer-key matching — matches only against an identified answer key; uncertain answers stay unverified.
5. Duplicate detection — exact and near-duplicate checks before import.
6. Central question bank — stores exam, year, shift, subject, topic, language, source and review status.
7. Automatic test generator — builds papers only from verified, reusable questions and a checked exam profile.
8. Firebase integration — publishes approved records using credentials stored outside the repository.
9. Website publishing — exposes approved questions/tests only.
10. Scheduled updates — runs source monitoring on a schedule and reports newly discovered links.

## Current implementation status

- Source monitor: initial version included.
- PDF/OCR intake: initial text/OCR extraction script included; outputs drafts, not live questions.
- Question schema: included.
- Mock generator: initial verified-question-only generator included.
- Scheduled source monitor: GitHub Actions workflow included.
- Firebase publishing: not enabled until the owner configures a server-side service credential as a GitHub Actions secret.
- Public question publishing: intentionally blocked until source permission and answer verification are recorded.

## Important content rules

- A public PDF link is not automatically permission to republish the full paper or its answer key.
- The collector records official source URLs; it does not bypass logins, CAPTCHAs, access controls, robots restrictions or download restrictions.
- Do not scrape coaching/test-prep websites or copy their proprietary question banks.
- OCR output is a draft. OCR can confuse option labels, mathematical notation, Marathi/Hindi characters and answer keys.
- Do not label newly authored practice questions as PYQs.
- Each exam profile must be checked against the current official notification before generating a full-length paper.

## Run locally

```bash
python -m pip install -r pyq-system/requirements.txt
python pyq-system/scripts/monitor_sources.py
python pyq-system/scripts/extract_questions.py --input path/to/approved-paper.pdf --exam NDA --year 2024 --source-url https://upsc.gov.in/ --out draft.json
python pyq-system/scripts/generate_mock.py --exam NDA --bank pyq-system/data/verified-questions.json --profiles pyq-system/exam-profiles.json --out generated-mocks/
```

OCR fallback requires the Tesseract executable to be installed on the machine.

## GitHub Actions setup

The scheduled workflow runs the source monitor and saves its discovery report as an Actions artifact. It does not yet publish anything to the live website or Firebase. Firebase publishing will be added only after secure server-side credentials and Firestore rules are configured.
