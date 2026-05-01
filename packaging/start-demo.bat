@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

if "%API_KEY%"=="" (
    echo.
    echo WARNING: API_KEY environment variable is not set.
    echo   Rule-condition generation via Google Gemini will not work until it is set.
    echo   You can set it for this session with:
    echo       set API_KEY=your-google-gemini-api-key
    echo   Or permanently with:
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
for %%f in ("ui\OpenRDR.exe" "ui\ui.exe") do if exist "%%~f" set "UI_EXE=%%~f"
if not defined UI_EXE (
    echo ERROR: could not find the OpenRDR UI launcher under ui\
    pause
    exit /b 1
)

if not exist logs mkdir logs
echo Starting OpenRDR server (in-memory mode, port 9090, with Demo KB) ...
start "OpenRDR Server" cmd /k "java -DlogFilePath=%CD%\logs\server.log --enable-native-access=ALL-UNNAMED -jar ""!SERVER_JAR!"" InMemory Demo"

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
start "" "!UI_EXE!"

endlocal
