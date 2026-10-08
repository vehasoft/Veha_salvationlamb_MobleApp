#!/usr/bin/env python3
"""
sync_bug_notes.py — regenerate the generated half of agents/BUG_NOTES.md

Owned by: PROJECT MANAGER (AGENTS.md).

Why this exists
---------------
The known-issue tables live inside each team-lead / module agent doc, because
AGENTS.md says "code and docs ship together" — a module agent updates its own
`.md` when it changes code.  That is the right place for the *source of truth*,
but it means the issues are scattered across 42 files.

This script walks those files, extracts every row of every
"Known issues" / "Team-level known issues" table, assigns a globally unique
BUG-NNN id, and writes the register between the AUTO-GENERATED markers in
agents/BUG_NOTES.md.  Everything outside those markers is hand-written by the
PM and is never touched.

Usage
-----
    python3 agents/tools/sync_bug_notes.py            # rewrite BUG_NOTES.md
    python3 agents/tools/sync_bug_notes.py --check    # exit 1 if stale (CI)

Stable ids
----------
BUG-NNN numbers are allocated by (team, agent, severity, agent-local id) in a
fixed sort order, so re-running the script on an unchanged tree produces
byte-identical output.  Adding an issue to a module doc renumbers the ids after
it; the agent-local id (e.g. LOGIN L5) is the durable reference and is always
shown alongside.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

AGENTS_DIR = Path(__file__).resolve().parent.parent
REPO_ROOT = AGENTS_DIR.parent
BUG_NOTES = AGENTS_DIR / "BUG_NOTES.md"

BEGIN = "<!-- AUTO-GENERATED:BEGIN -- do not edit by hand; run agents/tools/sync_bug_notes.py -->"
END = "<!-- AUTO-GENERATED:END -->"

# Team order used throughout the register.
# BIBLE / NOTIFICATIONS / ANNOUNCEMENTS added by T-026 (new teams on the v1.2.0 baseline).
TEAM_ORDER = [
    "PLATFORM",
    "AUTH",
    "APPSHELL",
    "FEED",
    "PROFILE",
    "MEDIA",
    "SEARCH",
    "BIBLE",
    "NOTIFICATIONS",
    "ANNOUNCEMENTS",
]

# Heading that opens a known-issues table in either template.
ISSUE_HEADING = re.compile(
    r"^#{2,3}\s*\d+\.\s*(Known issues(\s*/\s*tech debt)?|Team-level known issues)\s*$",
    re.IGNORECASE,
)
NEXT_HEADING = re.compile(r"^#{2,3}\s")

# A table row whose first cell is an issue id such as L5, AS-1, MN12, F-10.
ROW = re.compile(r"^\|\s*([A-Za-z]{1,3}-?\d{1,3})\s*\|")

# Split on pipes that are not escaped as \|
SPLIT_PIPE = re.compile(r"(?<!\\)\|")

# Inline code span: `...` or ``...``
CODE_SPAN = re.compile(r"(`+)(.+?)\1")

# Placeholder for a pipe that lives inside a code span and must not split a cell.
PIPE_SENTINEL = "\x00"

SEVERITY_RANK = {
    "Critical": 0,
    "High": 1,
    "Medium": 2,
    "Low": 3,
    "Cosmetic": 4,
    "Rollup": 5,
}
SEVERITY_TO_PRIORITY = {
    "Critical": "P0",
    "High": "P1",
    "Medium": "P2",
    "Low": "P3",
    "Cosmetic": "P3",
    "Rollup": "--",
}
SEVERITY_WORDS = ["Critical", "High", "Medium", "Low", "Cosmetic"]


def normalise_severity(cell: str) -> str:
    """Map a free-text severity cell ('**High (ANR)**') onto one keyword."""
    plain = cell.replace("*", "").strip()
    for word in SEVERITY_WORDS:
        if re.search(rf"\b{word}\b", plain, re.IGNORECASE):
            return word
    return "Medium"  # module tables always carry a severity; default defensively


def natural_key(local_id: str) -> tuple[str, int]:
    m = re.match(r"([A-Za-z]+)-?(\d+)", local_id)
    return (m.group(1), int(m.group(2))) if m else (local_id, 0)


def split_row(line: str) -> list[str]:
    """Split a markdown table row into cells.

    Two kinds of pipe must *not* split a cell:
      * an escaped pipe  ``\\|``  — common in the docs, e.g. ``a \\|\\| b``;
      * a pipe inside an inline code span, e.g. ``` `a || b` ``` — several
        agent docs write the always-true token guard that way, which would
        otherwise shear the row into 8+ cells and push the severity column out
        of position.
    """
    protected = CODE_SPAN.sub(
        lambda m: m.group(0).replace("|", PIPE_SENTINEL), line.strip()
    )
    cells = SPLIT_PIPE.split(protected)
    # A markdown row starts and ends with a pipe, producing empty outer cells.
    if cells and not cells[0].strip():
        cells = cells[1:]
    if cells and not cells[-1].strip():
        cells = cells[:-1]
    return [c.replace(PIPE_SENTINEL, "|").strip() for c in cells]



def parse_doc(path: Path) -> list[dict]:
    """Extract every known-issue row from one agent doc."""
    lines = path.read_text(encoding="utf-8").splitlines()
    issues: list[dict] = []
    header_cells: list[str] = []
    inside = False

    for line in lines:
        if ISSUE_HEADING.match(line):
            inside = True
            header_cells = []
            continue
        if inside and NEXT_HEADING.match(line):
            inside = False
            continue
        if not inside:
            continue
        if line.strip().startswith("|") and not header_cells:
            cells = split_row(line)
            if cells and cells[0] == "#":
                header_cells = cells
            continue
        if not ROW.match(line):
            continue
        cells = split_row(line)
        if len(cells) < 4:
            continue
        local_id, issue, third, fourth = cells[0], cells[1], cells[2], cells[3]
        # Lead docs:   | # | Issue | Module | Risk |
        # Module docs: | # | Issue | Severity | Fix / Suggested fix |
        is_rollup = len(header_cells) >= 3 and header_cells[2].lower() == "module"
        if is_rollup:
            severity, scope, fix = "Rollup", third, fourth
        else:
            severity, scope, fix = normalise_severity(third), "", fourth
        issues.append(
            {
                "local_id": local_id,
                "issue": issue,
                "severity": severity,
                "scope": scope,
                "fix": fix,
            }
        )
    return issues


def collect() -> list[dict]:
    """Walk every team folder and allocate globally unique BUG-NNN ids."""
    records: list[dict] = []
    for team in TEAM_ORDER:
        team_dir = AGENTS_DIR / team
        if not team_dir.is_dir():
            continue
        lead = team_dir / f"{team}_LEAD.md"
        modules = sorted(p for p in team_dir.glob("*.md") if p != lead)
        for path in ([lead] if lead.exists() else []) + modules:
            agent = path.stem
            for item in parse_doc(path):
                item.update(
                    team=team,
                    agent=agent,
                    kind="lead" if agent.endswith("_LEAD") else "module",
                    source=str(path.relative_to(REPO_ROOT)),
                )
                records.append(item)
    records.sort(
        key=lambda r: (
            TEAM_ORDER.index(r["team"]),
            0 if r["kind"] == "lead" else 1,
            r["agent"],
            SEVERITY_RANK[r["severity"]],
            natural_key(r["local_id"]),
        )
    )
    for n, rec in enumerate(records, start=1):
        rec["bug_id"] = f"BUG-{n:03d}"
    return records


def parse_global_issues() -> list[dict]:
    """The G1..G10 register that lives in AGENTS.md section 7."""
    text = (REPO_ROOT / "AGENTS.md").read_text(encoding="utf-8")
    out = []
    for line in text.splitlines():
        if re.match(r"^\|\s*G\d+\s*\|", line):
            cells = split_row(line)
            if len(cells) >= 4:
                out.append(
                    {"id": cells[0], "issue": cells[1], "owner": cells[2], "risk": cells[3]}
                )
    return out


def cell(text: str) -> str:
    """Make a parsed cell safe to re-emit inside a markdown table.

    Some source docs contain an *unescaped* pipe inside an inline code span
    (e.g. the always-true ``||`` token guard). ``split_row`` deliberately keeps
    those pipes with their cell; they must be escaped again on the way out or
    they would shear the generated row.
    """
    return re.sub(r"(?<!\\)\|", r"\\|", text)


def render(records: list[dict], globals_: list[dict]) -> str:
    counts: dict[str, int] = {}
    for rec in records:
        counts[rec["severity"]] = counts.get(rec["severity"], 0) + 1
    real = [r for r in records if r["severity"] != "Rollup"]
    n_docs = len({r["source"] for r in records})

    out: list[str] = [BEGIN, ""]
    out.append(
        f"**{len(records)} tracked entries** extracted from {n_docs} agent docs, "
        f"plus {len(globals_)} PM-level global issues."
    )
    out.append("")
    out.append("| Severity | Count | Priority |")
    out.append("|---|---|---|")
    for sev in SEVERITY_WORDS + ["Rollup"]:
        if counts.get(sev):
            label = sev if sev != "Rollup" else "Rollup (team-lead aggregate)"
            out.append(f"| {label} | {counts[sev]} | {SEVERITY_TO_PRIORITY[sev]} |")
    out.append(f"| **Distinct module-level defects** | **{len(real)}** | |")
    out.append("")

    out.append("### Per-team breakdown")
    out.append("")
    out.append("| Team | Critical | High | Medium | Low | Cosmetic | Rollups | Total |")
    out.append("|---|---|---|---|---|---|---|---|")
    for team in TEAM_ORDER:
        rows = [r for r in records if r["team"] == team]
        if not rows:
            continue
        cells = [
            str(sum(1 for r in rows if r["severity"] == s))
            for s in SEVERITY_WORDS + ["Rollup"]
        ]
        out.append(f"| {team} | " + " | ".join(cells) + f" | **{len(rows)}** |")
    out.append("")

    out.append("### PM-level global issues (`AGENTS.md` §7)")
    out.append("")
    out.append("| # | Issue | Owner | Risk |")
    out.append("|---|---|---|---|")
    for g in globals_:
        out.append(
            f"| {g['id']} | {cell(g['issue'])} | {cell(g['owner'])} | {cell(g['risk'])} |"
        )

    out.append("")
    out.append("---")
    out.append("")

    out.append("## Full register")
    out.append("")
    for team in TEAM_ORDER:
        team_rows = [r for r in records if r["team"] == team]
        if not team_rows:
            continue
        out.append(f"### {team} — {len(team_rows)} entries")
        out.append("")
        seen_agents: list[str] = []
        for rec in team_rows:
            if rec["agent"] not in seen_agents:
                seen_agents.append(rec["agent"])
        for agent in seen_agents:
            rows = [r for r in team_rows if r["agent"] == agent]
            src = rows[0]["source"]
            is_lead = rows[0]["kind"] == "lead"
            kind = "team lead (rollups)" if is_lead else "module"
            out.append(f"#### `{agent}` — {kind} · `{src}` · {len(rows)} entries")
            out.append("")
            if is_lead:
                out.append("| Bug | Agent id | Issue | Affects module(s) | Risk |")
                out.append("|---|---|---|---|---|")
                for r in rows:
                    out.append(
                        f"| {r['bug_id']} | {r['local_id']} | {cell(r['issue'])} | "
                        f"{cell(r['scope'])} | {cell(r['fix'])} |"
                    )
            else:
                out.append("| Bug | Agent id | Pri | Severity | Issue | Suggested fix |")
                out.append("|---|---|---|---|---|---|")
                for r in rows:
                    out.append(
                        f"| {r['bug_id']} | {r['local_id']} | "
                        f"{SEVERITY_TO_PRIORITY[r['severity']]} | {r['severity']} | "
                        f"{cell(r['issue'])} | {cell(r['fix'])} |"
                    )

            out.append("")
    out.append(END)
    return "\n".join(out)


def main() -> int:
    ap = argparse.ArgumentParser(description="Regenerate agents/BUG_NOTES.md")
    ap.add_argument(
        "--check", action="store_true", help="exit 1 if BUG_NOTES.md is out of date"
    )
    args = ap.parse_args()

    records = collect()
    globals_ = parse_global_issues()
    generated = render(records, globals_)

    if not BUG_NOTES.exists():
        print(
            f"error: {BUG_NOTES} does not exist — create the hand-written shell "
            f"with the AUTO-GENERATED markers first",
            file=sys.stderr,
        )
        return 2

    current = BUG_NOTES.read_text(encoding="utf-8")
    if BEGIN not in current or END not in current:
        print(f"error: AUTO-GENERATED markers not found in {BUG_NOTES}", file=sys.stderr)
        return 2

    head, _, rest = current.partition(BEGIN)
    _, _, tail = rest.partition(END)
    updated = head + generated + tail

    if args.check:
        if updated != current:
            print("BUG_NOTES.md is stale — run: python3 agents/tools/sync_bug_notes.py")
            return 1
        print("BUG_NOTES.md is up to date.")
        return 0

    BUG_NOTES.write_text(updated, encoding="utf-8")
    n_docs = len({r["source"] for r in records})
    print(
        f"wrote {BUG_NOTES.relative_to(REPO_ROOT)}: {len(records)} entries "
        f"from {n_docs} agent docs + {len(globals_)} global issues"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
