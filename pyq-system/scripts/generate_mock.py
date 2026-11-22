#!/usr/bin/env python3
"""Build an exam-shaped mock only from verified and reusable question records."""
from __future__ import annotations
import argparse, json, random
from pathlib import Path

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--exam", required=True)
    ap.add_argument("--bank", required=True)
    ap.add_argument("--profiles", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--seed", type=int, default=20261122)
    ap.add_argument("--content-type", choices=("pyq", "original_practice"), default="pyq")
    args = ap.parse_args()

    bank = json.loads(Path(args.bank).read_text(encoding="utf-8"))
    profiles = json.loads(Path(args.profiles).read_text(encoding="utf-8"))
    profile = profiles.get("profiles", {}).get(args.exam)
    if not profile or profile.get("status") != "verified":
        raise SystemExit(f"{args.exam}: exam profile is not verified; refusing to invent a paper pattern.")
    required = ("durationMinutes", "totalQuestions", "sections", "marksCorrect", "marksWrong")
    missing = [k for k in required if k not in profile]
    if missing:
        raise SystemExit("Incomplete exam profile: " + ", ".join(missing))

    def approved_for_lane(q):
        common = (q.get("reviewStatus") == "verified"
                  and q.get("answerKeyStatus") == "verified"
                  and q.get("question") and isinstance(q.get("options"), list)
                  and len(q["options"]) >= 2 and q.get("correctOption") is not None)
        if args.content_type == "pyq":
            return common and q.get("contentType", "pyq") == "pyq" and q.get("reusePermission") == "approved"
        return (common and q.get("contentType") == "original_practice"
                and q.get("authorship") == "original"
                and q.get("sourceUse") == "topic_only_no_question_text"
                and q.get("originalityReviewStatus") == "verified"
                and q.get("reusePermission") == "owned")

    eligible = [q for q in bank.get("questions", [])
                if q.get("exam") == args.exam and approved_for_lane(q)]
    rng = random.Random(args.seed)
    chosen = []
    for section in profile["sections"]:
        count = int(section["count"])
        pool = [q for q in eligible if q.get("subject") == section["subject"]]
        if len(pool) < count:
            raise SystemExit(f"Not enough verified questions for {section['subject']}: need {count}, have {len(pool)}.")
        chosen.extend(rng.sample(pool, count))
    if len(chosen) != int(profile["totalQuestions"]):
        raise SystemExit("Section counts do not equal the verified totalQuestions value.")

    rng.shuffle(chosen)
    payload = {
        "exam": args.exam, "seed": args.seed,
        "testId": f'{args.exam.lower().replace(" ", "-")}-mock-{args.seed}',
        "title": f'{args.exam} Full Mock Test',
        "durationMinutes": profile["durationMinutes"],
        "totalQuestions": profile["totalQuestions"],
        "sections": profile["sections"],
        "marksCorrect": profile["marksCorrect"], "marksWrong": profile["marksWrong"],
        "questions": chosen,
        "contentType": args.content_type,
        "sourcePolicy": ("verified-and-reuse-approved-pyq" if args.content_type == "pyq" else "independently-authored-and-reviewed-practice"),
        "status": "generated_not_yet_published",
        "published": False
    }
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    target = out / f"{args.exam.lower().replace(' ', '-')}-mock-{args.seed}.json"
    target.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Generated review draft: {target}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
