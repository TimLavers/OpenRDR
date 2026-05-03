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
- For the rule-condition assistant to work, a Google Gemini API key is
  required. Without it the UI still runs; only rule-condition suggestions
  generated from free-text expressions will fail.

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

The Demo KB is seeded with two cases:

  - Lindsay   waiting case, simple lab panel
              (Glucose 5.2 mmol/L [ref < 5.1], Pregnant Y, Age 21).
              Used for the Spanish-language rule-building demo.

    - Jane    cornerstone case, simple lab panel
              Surfaced as a cornerstone when adding a comment to Lindsay.

No rules are pre-built; you create them as part of the demo.

Demo scenario: build a rule with Spanish conditions
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

5. Hover the mouse of the interpretation and you will see the formal conditions
6. The point is that the Spanish-language reasons have been understood and translated by the LLM into the underlying formal-syntax conditions (Glucose is high; Age < 50; Pregnant is "Y").

Demo scenario 2: review a cornerstone case via chat
---------------------------------------------------

1. Select the case "Lindsay" if not already selected.
2. In the chat, type:  Add the comment: "Oh to be young again."
3. The chatbot will switch into rule-building mode and surface "Jane"
   as a cornerstone case for review.
3. The chatbot will offer condition suggestions and ask for reasons. Reply
   with :
       younger than 40

4. The chatbot will now ask if the report change should also apply to "Jane" as she is also younger than 40.
5. Enter "allow"
6. The chatbot will ask if you want to add any more conditions
7. Enter "no"
8. The chatbot will complete the rule

5. Hover the mouse of the interpretation and you will see the formal condition age < 40.0
6. The point is that user has control over the extent to which the rule change applies to the cornerstone cases.

Notes
-----

- This demo uses the server's in-memory persistence mode. Knowledge bases,
  cases, and interpretations are NOT saved between runs -- restart the
  launcher to start fresh.
- For a persistent setup with Postgres, see the project README:
  https://github.com/TimLavers/OpenRDR
