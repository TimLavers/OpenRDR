OpenRDR - demo package
======================

This zip is a self-contained demo of OpenRDR. It ships with:

  - server/openrdr-*.jar   the OpenRDR server (Ktor fat jar), run against
                           an in-memory database so nothing is persisted
                           between runs and no Postgres install is required.
  - ui/                    the OpenRDR desktop UI, with a bundled Java
                           runtime -- no Java install is required.
  - start-demo.bat         Windows launcher (starts the server, then the UI).
  - start-demo.sh          macOS / Linux launcher (equivalent).

Prerequisites
-------------

- Windows 10+ / macOS 12+ / modern Linux. Native binaries are for the OS
  that built this zip (see the zip filename).
- For the rule-condition assistant to work, a Google Gemini API key must be
  available in the environment variable API_KEY. Without it the UI still
  runs; only rule-condition suggestions generated from free-text expressions
  will fail.

  To get a key: https://ai.google.dev/gemini-api/docs/api-key

  Set it (Windows, per session):
      set API_KEY=your-google-gemini-api-key

  Set it (Windows, permanent -- new shells only):
      setx API_KEY "your-google-gemini-api-key"

  Set it (macOS/Linux):
      export API_KEY=your-google-gemini-api-key

Running
-------

1. Unzip anywhere you have write access (the server writes its working
   directory for logs/temp files).
2. Windows:  double-click start-demo.bat
   macOS/Linux:  ./start-demo.sh  from a terminal
3. A console window will open running the server on localhost:9090. The
   server boots in in-memory mode AND auto-creates a knowledge base named
   "Demo" pre-populated with the cases used by the two demo scenarios
   below. Shortly after, the OpenRDR desktop UI will launch.
4. In the UI, open the KB selector and choose "Demo" if it is not already
   the active KB.
5. To stop: close the UI window, then close the server console window
   (Ctrl+C on macOS/Linux kills both).

The Demo KB
-----------

The Demo KB is seeded with three cases:

  - Lindsay   waiting case, simple lab panel
              (Glucose 5.2 mmol/L [ref < 5.1], Pregnant Y, Age 21).
              Used for the Spanish-language rule-building demo.

  - Einstein  waiting case, full pathology panel with ~30 attributes.
              Used for the cornerstone-review demo.

  - Planck    cornerstone case, full pathology panel.
              Surfaced as a cornerstone when adding a comment to Einstein.

No rules are pre-built; you create them as part of the demo.

Demo scenario 1: build a rule with a Spanish condition
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
5. The Spanish-language reasons get translated by the LLM into the
   underlying conditions (Glucose is high; Age < 50; Pregnant is "Y").

Demo scenario 2: review a cornerstone case via chat
---------------------------------------------------

1. Select the case "Einstein". You will see a long list of pathology
   attributes -- great for showing the targeted-suggestions feature.
2. In the chat, type:  Add the comment: "Abnormal haemoglobin"
3. The chatbot will switch into rule-building mode and surface "Planck"
   as a cornerstone case for review. The cornerstone index ("1 of 1")
   and the cornerstone case name should appear in the chat.
4. Provide the reason:  haemoglobin is abnormal
5. The chatbot completes the rule and the report on Einstein should now
   read "Abnormal haemoglobin".

Notes
-----

- This demo uses the server's in-memory persistence mode. Knowledge bases,
  cases, and interpretations are NOT saved between runs -- restart the
  launcher to start fresh.
- For a persistent setup with Postgres, see the project README:
  https://github.com/TimLavers/OpenRDR
