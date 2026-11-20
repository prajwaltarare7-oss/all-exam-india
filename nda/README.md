# NDA Mock Test Series — ALL EXAM INDIA

This folder is the source of truth for the NDA test series.

## Official written-paper structure

| Paper | Questions | Marks | Duration | Correct | Wrong |
|---|---:|---:|---:|---:|---:|
| Mathematics | 120 | 300 | 150 minutes | +2.5 | -0.833333 |
| General Ability Test (GAT) | 150 | 600 | 150 minutes | +4 | -1.333333 |
| Total | 270 | 900 | 300 minutes | — | — |

Mathematics and GAT are separate papers. GAT includes English and General Knowledge areas: Physics, Chemistry, General Science, History, Geography and Current Affairs.

## 20-paper production plan

Each full mock must contain 120 original Mathematics questions and 150 original GAT questions, four options per question, one correct answer, a worked explanation, and a verified topic tag. No paper may be marked available until all 270 questions and explanations pass validation.

- NDA Mock 01–20: full-length, mixed-difficulty papers.
- Each paper: easy, moderate and difficult questions in a balanced exam-like mix.
- Questions must be newly authored; do not copy Testbook or other commercial question banks.
- Hindi/English selection happens before starting. The selected language alone is displayed.
- Keep Maths and GAT as separate timed papers, with their own marking rules.
- Results must show score, correct, incorrect, unanswered, accuracy and topic-wise performance.
- Review must show the correct answer and step-by-step explanation.

## Data contract

Each question record must include:
- `id`, `paper` (maths/gat), `topic`
- `question` and `options` in Hindi and English
- `answerIndex` (0–3)
- `explanation` in Hindi and English
- `difficulty` (easy/moderate/hard)

## Release gate

Do not publish a paper as a full mock if its question count is incomplete, if any answer key is missing, or if explanations have not been checked. This prevents placeholder tests from being mistaken for real exam papers.
