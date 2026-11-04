# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

The guide for every coding agent in this repo is AGENTS.md, imported below. Keep repo-wide guidance there
(GEMINI.md imports it too, and `tests/test_agents_md.py` checks that this file keeps the import).

@AGENTS.md

## On this Windows checkout

- The venv's interpreter is `tools/venv/Scripts/python` (not `bin/`). Full suite:
  `tools/venv/Scripts/python -m unittest discover -s tests -v`. Suites that drive `tools/*.sh` skip here; CI runs them.
- One test file: `tools/venv/Scripts/python -m unittest discover -s tests -p "test_agents_md.py" -v`.
  One test by name: add `-k test_claude_and_gemini_import_agents_md`.
- Launcher build from `launcher/`: `./gradlew.bat --no-daemon testReleaseUnitTest lintRelease assembleRelease`.
- The `adb.exe`, `fastboot.exe` and other platform-tools files in the repo root are local and untracked: never commit them.
