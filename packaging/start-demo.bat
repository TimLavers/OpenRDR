@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

rem --- load API_KEY from api-key.txt next to this script, if present ---
rem An existing environment variable always wins. The file should contain
rem the Gemini API key on a single line; lines starting with "#" are ignored.
if "%API_KEY%"=="" if exist "%~dp0api-key.txt" (
    for /f "usebackq tokens=* eol=#" %%L in ("%~dp0api-key.txt") do (
        if not defined API_KEY if not "%%L"=="" set "API_KEY=%%L"
    )
    if defined API_KEY echo Loaded API_KEY from api-key.txt
)

if "%API_KEY%"=="" (
    echo.
    echo WARNING: API_KEY is not set.
    echo   Rule-condition generation via Google Gemini will not work until it is set.
    echo   Easiest option: create a file named api-key.txt next to this script
    echo   and paste the Gemini key into it on a single line.
    echo   Or set the environment variable for this session:
    echo       set API_KEY=your-google-gemini-api-key
    echo   Or permanently:
    echo       setx API_KEY "your-google-gemini-api-key"
    echo.
)

rem --- locate the server fat jar (version-independent) ---
set "SERVER_JAR="
for %%f in ("server\*-all.jar" "server\openrdr-*.jar") do if not defined SERVER_JAR set "SERVER_JAR=%%f"
if not defined SERVER_JAR (
    echo ERROR: could not find server fat jar under server\
    pause
    exit /b 1
)

rem --- locate the UI launcher ---
set "UI_EXE="
for %%f in ("ui\OpenRDR\OpenRDR.exe" "ui\OpenRDR.exe" "ui\ui.exe") do if exist "%%~f" set "UI_EXE=%%~f"
if not defined UI_EXE (
    echo ERROR: could not find the OpenRDR UI launcher under ui\
    pause
    exit /b 1
)

rem --- prefer the JRE bundled with the UI; fall back to system java ---
set "JAVA_EXE=ui\OpenRDR\runtime\bin\java.exe"
if not exist "%JAVA_EXE%" set "JAVA_EXE=java"

if not exist logs mkdir logs
echo Starting OpenRDR server (in-memory mode, port 9090, with Demo KB) ...
rem -Djavax.net.ssl.trustStoreType=WINDOWS-ROOT makes the JVM trust whatever
rem certificates Windows trusts. This is needed on corporate machines that
rem MITM outbound HTTPS with a private root CA -- without it, calls to the
rem Google Gemini API fail with PKIX path building errors.
start "OpenRDR Server" cmd /k """!JAVA_EXE!"" -DlogFilePath=%CD%\logs\server.log -Djavax.net.ssl.trustStoreType=WINDOWS-ROOT --enable-native-access=ALL-UNNAMED -jar ""!SERVER_JAR!"" InMemory Demo"

echo Waiting for the server to accept connections ...
set /a tries=0
:waitloop
set /a tries+=1
if !tries! gtr 60 (
    echo.
    echo ERROR: the server did not come up within 60 seconds.
    echo Check the server console window for details.
    pause
    exit /b 1
)
powershell -NoProfile -Command "try { $c=New-Object Net.Sockets.TcpClient; $c.Connect('localhost',9090); $c.Close(); exit 0 } catch { exit 1 }" >nul 2>&1
if errorlevel 1 (
    timeout /t 1 /nobreak >nul
    goto waitloop
)

echo Launching OpenRDR UI ...
rem JAVA_TOOL_OPTIONS is honoured automatically by the bundled JRE, so this
rem is how we tell the UI's logback config where to write its log file.
rem Without it the literal "${logFilePath}" placeholder is used and Logback
rem fails to open the file.
set "JAVA_TOOL_OPTIONS=-DlogFilePath=%CD%\logs\ui.log"
start "" "!UI_EXE!"
set "JAVA_TOOL_OPTIONS="

endlocal
