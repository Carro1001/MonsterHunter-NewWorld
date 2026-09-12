# AGENTS.md

**The guidance for this repository lives in [`CLAUDE.md`](CLAUDE.md). Read that file. It applies to
every coding agent working here, not just Claude Code.**

This file used to be a near-verbatim copy of it. Keeping two 240-line manuals hand-synchronised did
not work: by the time R0b landed, this copy still described an attack timeline for "Great Izuchi
only" (Rathian's measured bite timeline had shipped in P4), still had no R1a habitat section, and
had missed the R0b presentation work entirely — so an agent reading this file was being told things
about the codebase that had been untrue for two packets. A Codex review of PR #5 caught the third
divergence, which was the point at which duplicating the manual stopped being worth defending.

So there is now one manual and one pointer. If you are about to add repository guidance, add it to
`CLAUDE.md`; don't restore a copy here.

Where to go from `CLAUDE.md`:

- `docs/ROADMAP.md` scopes the current work and hands out the numbered packets.
- `docs/TEST_PLAN.md` is the round-by-round history, the current GameTest count, and everything
  still waiting on a human at a screen.
- `docs/DEFERRED.md` is everything consciously postponed, with enough context to pick it up cold.
