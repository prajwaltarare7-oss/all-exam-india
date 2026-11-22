# ALL EXAM INDIA — PYQ automation

## Scheduled pipeline
GitHub Actions is configured to run every 6 hours and can also be started manually. It:
1. Checks configured official exam portals for PDF links and records discoveries.
2. Downloads PDFs only when the intake manifest explicitly records source review, reuse approval, and an approved host; then OCRs those approved files.
3. Splits OCR text into question candidates (conservative heuristic; review required).
4. Matches answer keys supplied in `intake/answer-keys/` (matches still require human verification).
5. Marks exact duplicate question candidates.
6. Promotes only records manually entered and approved in `data/reviewed-questions.json` into the eligible central question bank.
7. Generates mock drafts only when a profile is officially verified and enough approved questions exist.
8. Optionally publishes eligible records to Firebase when repository secrets are configured.

Artifacts include source discoveries, OCR drafts, question candidates, answer-key matches, duplicate report and generated mock drafts.

## One-time setup still required
- Add `FIREBASE_SERVICE_ACCOUNT_JSON` as a GitHub Actions repository secret. This must be a restricted server-side service account, never a web API key or personal password.
- Optionally add `FIREBASE_STORAGE_BUCKET` as a secret to enable approved PDF uploads to Firebase Storage.
- Configure Firestore and Storage security rules before publishing. Website read integration and Firebase web configuration must be added separately; this workflow alone does not make the website display the records.
- Add PDFs you own or are explicitly allowed to reuse under `intake/approved/`, and list them in `intake/manifest.json`.
- Add permitted answer keys as JSON files under `intake/answer-keys/`.
- Manually inspect OCR candidates and put approved records in `data/reviewed-questions.json`. Required statuses: `reviewStatus: "verified"`, `answerKeyStatus: "verified"`, `reusePermission: "approved"`.

## Safety and accuracy rules
- Publicly accessible does not mean permission to republish. The source monitor is link-only; it does not bulk-download third-party papers or bypass access controls.
- OCR and question splitting can make mistakes, especially with tables, formulas and bilingual layouts. Candidates are never auto-verified.
- Answer-key matching is a suggestion until checked against the official key.
- Duplicate detection marks exact normalized text duplicates; similar-but-not-identical questions need review.
- No mock is generated from unverified or unlicensed material, and no mock is claimed to be a PYQ.
- The workflow does not guarantee uninterrupted operation; GitHub Actions schedules may be delayed or disabled by platform limits. Firebase rules/config files are in the repository but have not yet been deployed to the Firebase project.

## Local run
```bash
python -m pip install -r pyq-system/requirements.txt
python pyq-system/scripts/monitor_sources.py
python pyq-system/scripts/process_intake.py
python pyq-system/scripts/parse_ocr_drafts.py
python pyq-system/scripts/match_answer_keys.py
python pyq-system/scripts/deduplicate_bank.py
python pyq-system/scripts/validate_reviewed_bank.py
python pyq-system/scripts/generate_all_mocks.py
```
