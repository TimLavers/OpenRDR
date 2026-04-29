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
3. A console window will open running the server on localhost:9090.
   Shortly after, the OpenRDR desktop UI will launch.
4. To stop: close the UI window, then close the server console window
   (Ctrl+C on macOS/Linux kills both).

Notes
-----

- This demo uses the server's in-memory persistence mode. Knowledge bases,
  cases, and interpretations are NOT saved between runs.
- For a persistent setup with Postgres, see the project README:
  https://github.com/TimLavers/OpenRDR
