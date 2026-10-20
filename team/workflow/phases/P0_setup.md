# P0 — Environment & scaffolding (≈1 h)
**Goal:** a buildable empty Java project (plain javac, no Maven), git repo, sync mechanism proven.
**Needs human:** G1 answers (else use STATE.md assumptions).

## Steps
- [x] 0.1 Verify tools in cloud: `javac -version` (21), `python3 -c "import matplotlib"`, `pandoc -v`, `libreoffice --version`. Install missing via pip/apt if allowed.
- [x] 0.2 Create workdir `/home/claude/dlscms`; `git init`.
- [x] 0.3 (CHANGED, Decision 5) `scripts/build.sh` / `build.bat` + `lib/` JUnit4 jars + `com.dlscms.RunAll`.
- [x] 0.4 Create dirs: `src/main/java/com/dlscms/{model/vehicle,service,strategy,exception,persistence,ui,util,sim}`, `src/test/java/...`, `data/`, `results/`, `scripts/`, `paper/figures/`, `slides/`, `docs/`.
- [x] 0.5 Hello-world `Main` + one passing test; run `scripts/build.sh test`.
- [x] 0.6 Prove sync: `zip -r /mnt/user-data/outputs/dlscms_snapshot.zip . -x 'target/*' '.git/*'` → `device_commit_files` to `C:\Vedant\NMIMS\projects\OOP\_sync\dlscms_snapshot.zip` → `device_bash`: `unzip -oq` into `$HOME/mnt/OOP/dlscms/`. Document the exact commands in `scripts/sync.md`.
- [x] 0.7 Copy `.gitignore` (target/, *.class, results/tmp).

## Exit checks
`scripts/build.sh test` green · `java -jar dist/dlscms.jar` prints banner · `$HOME/mnt/OOP/dlscms/scripts/build.sh` exists on device.
