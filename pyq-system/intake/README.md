# Approved PDF intake

1. Put a PDF you own or have explicit permission to reuse in this folder.
2. Add one entry to ../manifest.json with: file, exam, year, sourceUrl, approvedSourceDomains, sourceReviewed=true, reusePermission="approved" and optional ocrLanguage (for example eng+hin+mar).
3. The scheduled pipeline OCRs the PDF into a review draft artifact. It does not automatically convert OCR text into trusted questions or publish the PDF/questions to the public website.

The official-source monitor only collects public PDF links from configured official domains. It intentionally does not auto-download them: a public link is not proof of permission to republish.
