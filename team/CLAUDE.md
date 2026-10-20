> **Note:** in the GitHub repository the project-management files live in `team/` (this file, `STATE.md`, `HUMAN_INPUT.md`, `workflow/`). Paths below such as `workflow/phases/...` mean `team/workflow/phases/...` there; code and docs sit at the repository root.

# CLAUDE.md — Defence Logistics & Supply Chain Management System (DLSCMS)

You are the autonomous builder of this project. Read this file FIRST in every session, then `STATE.md`.
Source of truth for requirements: `Project Brief.docx` (distilled in `docs/requirements.md`).

## Deliverables (all three are mandatory; plus supporting UML class diagrams and a user manual)
1. **Working local model** — Java 17+ app (plain `javac` build, no Maven), runs offline, `java -jar dist/dlscms.jar` one-liner demo.
2. **Research paper** — process, design, datasets, experiments, findings (IEEE-style; .docx + .pdf).
3. **Presentation** — 10–12 slides + speaker notes + demo script (.pptx).

## Team
Vedant Pathak (R045), Dhairya Shah (R057), Kanishak Thakran (R046) — SVKM's NMIMS MPSTME, Mumbai.

## Order of work
Build and prove the **local model first (P0–P5)**. Do NOT start P6 (paper) or P7 (slides) until the human accepts the working model (gate G6). Paper ends with a short AI-assistance disclosure paragraph (last item).

## Hard constraints
- **Budget: $0.** Open-source only (JDK 17+, JUnit 4 + Hamcrest bundled in `lib/`, Python+matplotlib for charts). No paid APIs/services.
- **Time box: 24 h total** (plan = ~20 h + buffer). MVP quality: everything works end-to-end > anything is polished.
- **Data: synthetic only.** Never use real/classified defence data. Base names/locations may be real public cities; stock levels, fleets and requests are generated. State this clearly in the paper.
- **Honesty rule:** never invent citations, numbers, or results. Every number in the paper/slides must trace to a file in `results/` produced by a script in `scripts/`. Every reference must be verified (fetched) before it is cited; otherwise omit it.
- Brief's OOP concepts are the grading spine: classes/objects, encapsulation, inheritance, polymorphism, abstraction, interfaces, collections, exceptions, file handling, generics, packages. Each must be visibly demonstrable in code AND have a section in the paper AND a slide.

## How to run the loop (resume protocol)
1. Read `STATE.md` → find the first phase not marked `DONE`.
2. Open that phase's file in `workflow/phases/`. Follow it step by step.
3. After EACH step: update `STATE.md` (tick the box, add a one-line note, log decisions in the Decision Log).
4. Before marking a phase DONE: run its **Exit checks** (commands are in the phase file). If any fail, fix; don't skip.
5. `git commit` at the end of each phase (`phase-N: <summary>`).
6. Move to the next phase WITHOUT asking permission, unless a **Human Gate** is hit.

## Human gates (the ONLY times to stop and ask)
Listed in `HUMAN_INPUT.md`. Protocol: write the question + your default into `HUMAN_INPUT.md`, continue every task that is not blocked, and ask the human in ONE batched message. If the human is absent, take the stated default, log it in the Decision Log as `ASSUMED`, and keep going.
Do NOT ask about: library choices, class design, file layout, test strategy, wording, chart styling.

## Environment notes
- Project folder lives on the user's computer (reachable via `device_bash`, mounted at `$HOME/mnt/OOP`). That shell has a JRE but **no javac/Maven**.
- The cloud container has JDK 21 + Python + pandoc + LibreOffice (code is compiled with `--release 17`). **Maven Central is blocked there, so the build is plain `javac` via `scripts/build.sh [test]` (Windows: `scripts\build.bat [test]`); do not add dependencies that need downloading.** The PC-side shell has only a Java 11 JRE, so jars cannot be run there; run/test in the cloud. **Default: build in the cloud workdir `/home/claude/dlscms`, then sync to the folder at the end of every phase** (zip the workdir → `device_commit_files` → unzip with `device_bash`). Details in `workflow/phases/P0_setup.md`.
- If a future session has javac on the device, build in place and skip syncing.

## Conventions
- Package root `com.dlscms` with sub-packages: `model`, `model.vehicle`, `service`, `strategy`, `exception`, `persistence`, `ui`, `util`, `sim`.
- Java 17 language level (records, switch expressions, text blocks OK), 4-space indent, Javadoc on every public class, no runtime deps. Tests: JUnit 4 (`org.junit.Test`, `org.junit.Assert`); `com.dlscms.RunAll` auto-discovers `*Test` classes.
- Data files in `data/` (CSV), outputs in `results/`, figures in `paper/figures/`, scripts in `scripts/`.
- Every phase leaves the repo in a **buildable, tests-green** state (`scripts/build.sh test`). Never leave a broken build between phases.

## Definition of Done (project)
- [ ] `scripts/build.sh test` green; every public class in `model`/`service`/`strategy` has a test class (no JaCoCo: Maven Central blocked)
- [ ] Demo scenario "Pune Base needs medical kits" runs end-to-end (request → source check → vehicle → inventory update → delivery → report)
- [ ] Traceability matrix in `docs/requirements.md` fully ticked (feature → class → test → paper section → slide)
- [ ] Paper PDF/DOCX builds, all figures/tables sourced from `results/`, references verified
- [ ] PPTX renders cleanly (convert to PDF and visually check each slide)
- [ ] `README.md` has run instructions that were actually executed from a clean checkout
- [ ] UML class diagrams (docs/img/class_*.png) match the final code; user manual verified by `scripts/verify_manual.sh`
- [ ] Final bundle: `submission/` contains jar, source zip, paper, slides, user manual, UML diagrams, README
