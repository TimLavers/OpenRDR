# Full cucumber runs — 17 October 2026

- Revision: `42c2a93c` — Add mouse-free backdoor checks for cucumber comment tooltips.
- Command: `gradlew cuT` (the `cucumberTest` task).
- Started: **13:38:34.206 AEDT**, 17 October 2026.
- Result: **green — 230 scenarios passed after three retries**.
- Total elapsed time: **24m 6s**, as reported by Gradle (`BUILD SUCCESSFUL`).
- Finished: approximately **14:02:41 AEDT** (daemon completion timestamp).

The Gradle duration includes build work, server/client startup, scenarios,
automatic retries and cleanup. The original process-exit observation was not
retained, so the duration above comes from the archived Gradle output, not an
inferred sum of scenario times. This run became green after retries; it was
not a failure-free first pass.

Live timing CSVs, completed JUnit reports and any failure artifacts are archived
locally in `build/cucumber-monitor-20261017-133834/` so folder retries cannot
overwrite the first failure's evidence.

## Investigation during the run

The French and Spanish rule scenarios failed on their first attempts. Both
expected `Age < 50`, while their server logs and chat output show the canonical
condition text `Age < 50.0`. The old accessibility lookup accepted prefixes;
the new tooltip backdoor compares complete condition texts. The two feature
expectations were corrected while the chat folder was still running, ready
for its automatic retry. Application behavior and assertion strictness are
unchanged. The completed JUnit report confirms this was the only mismatch in
both assertions. Both scenarios passed on the automatic retry (43.451 seconds
for the two-scenario retry), and the full run continued to the comments folder.

The KB scenario "The option to create a KB is given if there are none" timed
out reading the displayed KB name after adding the demonstration case. Server
logs show creation and selection of Glucose succeeded. It passed on automatic
retry without a code change (4.173 seconds). The retry does not establish the
root cause of the intermittent UI-read timeout. It did not recur in the repeat
run below, which includes the subsequent case-list fetching change.

The full run's final output is archived locally as
`build/cucumber-monitor-20261017-133834/completed-run.log`.

## Repeat run started at 20:06

- Revision: `3cbd029c` — fetch the case list only when the open KB changes.
- Command: `gradlew cuT`.
- Started: **20:06:01.974 AEDT**, 17 October 2026.
- Finished: **20:35:49.269 AEDT**, 17 October 2026.
- Result: **230/230 scenarios passed on their first attempt; zero failures and zero retries**.
- Total elapsed time: **29m 47.294s** (1,787.294 seconds), measured from Gradle wrapper start to exit.
- Gradle reported `BUILD SUCCESSFUL in 29m 46s`; the wrapper measurement also includes its startup and exit overhead.
- Exit code: **0**.
- Evidence directory: `build/cucumber-monitor-20261017-200601/`.

The numeric tooltip expectations are already committed in this revision.
No source changes or corrective reruns were needed during this run. The
previously intermittent empty-KB creation scenario also passed first time.

All 14 folders completed. Their CSVs record **856 Gemini calls**, **56 server
starts**, and **39.540 seconds** spent starting servers. The scenario seconds
below are whole-second durations and sum to 1,610 seconds (26m 50s); they are
not the full elapsed time because they exclude inter-scenario overhead and
discard fractional seconds.

The starts comprise 55 feature starts and one automatic replacement, leaving
174 successful server reuses. Before "The current cornerstone should remain
selected if the user removes a condition", reset timed out after five seconds
with one active request and no connected users. The harness saved diagnostics
and replaced the server; the scenario passed on that same attempt. This was
successful reset recovery, not a scenario failure or retry. The event is
recorded in `build/cucumber-monitor-20261017-200601/daemon.log`.

| Folder         | Scenarios passed | Scenario seconds | Gemini calls |
|----------------|-----------------:|-----------------:|-------------:|
| attributes     |                4 |               15 |            0 |
| cases          |               25 |               61 |           28 |
| chat           |               43 |              387 |          232 |
| comments       |               15 |              177 |           86 |
| conditions     |               16 |              118 |           71 |
| demo           |                1 |               55 |           29 |
| inferencing    |               19 |               64 |           35 |
| interpreter    |                3 |               15 |            7 |
| kb             |               56 |              209 |          132 |
| report         |                2 |               33 |           19 |
| rulebuilding   |               34 |              377 |          217 |
| samples        |                9 |               84 |            0 |
| trial-matching |                1 |               13 |            0 |
| voice          |                2 |                2 |            0 |
| **Total**      |          **230** |        **1,610** |      **856** |

These two wall-clock measurements are not a controlled performance comparison:
the first includes retries and the runs can experience different Gemini and
machine latency.
