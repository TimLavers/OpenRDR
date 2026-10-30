OpenRDR - demo package
======================

This zip is a self-contained demo of OpenRDR. It ships with:

  - server/openrdr-*.jar   the OpenRDR server (Ktor fat jar), run against
                           an in-memory database so nothing is persisted
                           between runs and no Postgres install is required.
  - ui/                    the OpenRDR desktop UI, with a bundled Java
                           runtime that is also used to run the server --
                           no separate Java install is required.
  - start-demo.bat         Windows launcher (starts the server, then the UI).
  - start-demo.sh          macOS / Linux launcher (equivalent).

Prerequisites
-------------

- Windows 10+ / macOS 12+ / modern Linux. Native binaries are for the OS
  that built this zip (see the zip filename).
- Chat authoring, condition translation, AI-derived classifications, reports
  and voice transcription require a Google Gemini API key. Deterministic
  inference over existing non-AI rules does not require it.

  To get a key: https://ai.google.dev/gemini-api/docs/api-key

  Easiest option (all platforms, no admin / env-var setup required):
      Create a plain-text file named api-key.txt next to start-demo.bat /
      start-demo.sh and paste your key into it on a single line. Save.
      The launcher will pick it up automatically on the next run.
      (Lines starting with # are ignored, so you can leave a comment.)

  Or set the API_KEY environment variable instead (it always wins over
  api-key.txt if both are present):

      Windows, per session:        set API_KEY=your-google-gemini-api-key
      Windows, permanent:          setx API_KEY "your-google-gemini-api-key"
      macOS/Linux:                 export API_KEY=your-google-gemini-api-key

Running
-------

1. Unzip anywhere you have write access (the server writes its working
   directory for logs/temp files).
2. Windows:  double-click start-demo.bat
   macOS/Linux:  ./start-demo.sh  from a terminal
3. The server starts on localhost:9090 in in-memory mode, followed by the UI.
   Windows opens a server console; macOS/Linux write server output to logs/.
4. In the chat, type:  open Pathology
   The chat will ask for a name for your own copy (suggest "Clinic").
   Type a name and press Enter. The Pathology demonstration KB is copied
   and opened, pre-populated with the cases used by the three demo
   scenarios below.
5. To stop: close the UI window, then close the server console window
   (Ctrl+C on macOS/Linux kills both).

The Pathology demonstration KB
-----------------------------

The Pathology demonstration KB is seeded with three cases:

  - Lindsay   waiting case, simple lab panel
              (Glucose 5.2 mmol/L [ref < 5.1], Pregnant Y, Age 21).
              Used for the Spanish-language rule-building demo.

  - Jane      cornerstone case, simple lab panel
              Surfaced as a cornerstone when adding a comment to Lindsay.

  - Taylor   waiting case, used for the derived-attributes demo
              (HbA1c 7.8 % [ref 4.0-6.0], Height 1.78 m, Weight 98 kg,
              Age 54, Sex F).

No rules are pre-built; you create them as part of the demo.

Demo scenario 1: build a rule with Spanish conditions
------------------------------------------------------

1. Select the case "Lindsay".
2. In the chat, type:  Add the comment: "La paciente presenta diabetes gestacional."
3. The chatbot will offer condition suggestions and ask for reasons. Reply
   with these three reasons, one at a time:
       El nivel de Glucose es alto
       Menos de 50 anos
       Pregnant es "Y"
4. Decline to add more reasons. The chatbot completes the rule. The
   interpretation should now read:
       "La paciente presenta diabetes gestacional."

5. Hover over the interpretation and you will see the formal conditions
6. The point is that the Spanish-language reasons have been understood and translated by the LLM into the underlying formal-syntax conditions (Glucose is high; Age < 50; Pregnant is "Y").

Demo scenario 2: review a cornerstone case via chat
--------------------------------------------------

1. Select Lindsay and ask: Add the comment: "Oh to be young again."
2. Give the reason: younger than 40.
3. Jane is also younger than 40. When asked whether the change should apply
   to her, enter "allow".
4. Decline further reasons to complete the rule.
5. Hover over the comment to see the condition Age < 40.0.

The expert controls whether a rule's effect on a cornerstone is acceptable.

Demo scenario 3: derived attributes, repeat inference and the AI report
---------------------------------------------------------------------

This sequence follows "Pathology demo script.feature" under
cucumber/src/test/resources/requirements/demo/. It creates two derived
attributes and two comments, then generates a report.

1. Select Taylor. The Derived attributes panel initially has no values.
2. Ask: Add attribute BMI with formula weight / height^2.
   Give the reason "Height is in case", then decline further reasons.
   The panel shows BMI = 30.93. Hover to see the formula and condition.
3. Ask: Add attribute "Diabetes status" with value diabetic.
   Give the suggested reason "HbA1c is high", then decline further reasons.
   The panel shows Diabetes status = diabetic.
4. Ask: Add the comment: "Obesity. BMI {BMI}. Weight reduction."
   Give the reason "BMI > 30", then decline further reasons.
   The rendered comment is "Obesity. BMI 30.93. Weight reduction."
5. Ask: Add the comment: "Diabetic. Dietary review. HbA1c above target. Repeat in 3 months."
   Give both reasons: Diabetes status is "diabetic", and HbA1c is high.
   Decline further reasons. Confirm wording if the chat asks.
6. Open the Report panel. It uses the comments and case values to generate
   readable prose and consolidate their follow-ups. Exact wording can vary;
   the acceptance scenario checks the BMI and recommendation topics.

The BMI comment demonstrates repeat inference and substitution of a derived
value. The report illustrates the difference between fixed rule-authored
comments and a generated narrative that must be reviewed before release.

Notes
-----

- This demo uses the server's in-memory persistence mode. Knowledge bases,
  cases, and interpretations are NOT saved between runs -- restart the
  launcher to start fresh.
- For a persistent setup with Postgres, see the project README:
  https://github.com/TimLavers/OpenRDR
