#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

# Load API_KEY from api-key.txt next to this script, if present and the
# environment variable isn't already set. First non-blank, non-comment
# line wins; CRs stripped to tolerate files edited on Windows.
if [ -z "${API_KEY:-}" ] && [ -f "./api-key.txt" ]; then
    while IFS= read -r line || [ -n "$line" ]; do
        line=${line%$'\r'}
        case "$line" in
            ''|\#*) continue ;;
            *) API_KEY="$line"; break ;;
        esac
    done < ./api-key.txt
    if [ -n "${API_KEY:-}" ]; then
        export API_KEY
        echo "Loaded API_KEY from api-key.txt"
    fi
fi

if [ -z "${API_KEY:-}" ]; then
    echo
    echo "WARNING: API_KEY is not set."
    echo "  Rule-condition generation via Google Gemini will not work until it is."
    echo "  Easiest option: create a file named api-key.txt next to this script"
    echo "  containing the Gemini key on a single line."
    echo "  Or export it in your shell:"
    echo "      export API_KEY=your-google-gemini-api-key"
    echo
fi

SERVER_JAR=$(ls server/*-all.jar server/openrdr-*.jar 2>/dev/null | head -n1 || true)
if [ -z "$SERVER_JAR" ]; then
    echo "ERROR: could not find server fat jar under server/" >&2
    exit 1
fi

# Locate the UI launcher (Compose createDistributable layout differs per OS).
UI_LAUNCHER=""
for candidate in \
    ui/OpenRDR.app/Contents/MacOS/OpenRDR \
    ui/OpenRDR/bin/OpenRDR \
    ui/bin/OpenRDR \
    ui/bin/ui \
    ui/OpenRDR \
    ui/ui; do
    if [ -x "$candidate" ]; then
        UI_LAUNCHER="$candidate"
        break
    fi
done
if [ -z "$UI_LAUNCHER" ]; then
    echo "ERROR: could not find the OpenRDR UI launcher under ui/" >&2
    exit 1
fi

# Prefer the JRE bundled with the UI distributable; fall back to system java.
JAVA_BIN=""
for candidate in \
    ui/OpenRDR.app/Contents/runtime/Contents/Home/bin/java \
    ui/OpenRDR/runtime/bin/java; do
    if [ -x "$candidate" ]; then
        JAVA_BIN="$candidate"
        break
    fi
done
if [ -z "$JAVA_BIN" ]; then
    JAVA_BIN="java"
fi

mkdir -p logs
echo "Starting OpenRDR server (in-memory mode, port 9090, with Demo KB) ..."
echo "  Server output -> logs/server-console.log"
"$JAVA_BIN" \
    -DlogFilePath="$(pwd)/logs/server.log" \
    --enable-native-access=ALL-UNNAMED \
    -jar "$SERVER_JAR" InMemory Demo \
    >logs/server-console.log 2>&1 &
SERVER_PID=$!
trap 'kill $SERVER_PID 2>/dev/null || true' EXIT

echo "Waiting for the server to accept connections ..."
for i in $(seq 1 60); do
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
        echo "ERROR: server process exited before accepting connections. See logs/server-console.log:" >&2
        echo "---" >&2
        tail -n 80 logs/server-console.log >&2
        exit 1
    fi
    if (echo >/dev/tcp/localhost/9090) >/dev/null 2>&1; then
        break
    fi
    sleep 1
done

echo "Launching OpenRDR UI ..."
echo "  UI output -> logs/ui-console.log"
# If the UI is the macOS .app bundle, launch it via LaunchServices (`open`)
# rather than invoking the inner Mach-O binary directly. Otherwise macOS
# attributes microphone (TCC) requests to the parent terminal instead of to
# OpenRDR.app, and the bundle's NSMicrophoneUsageDescription is ignored - so
# you never get the right consent prompt and the JVM silently records zeros.
#
# JAVA_TOOL_OPTIONS is honoured automatically by the bundled JRE, so we use
# it to pass -DlogFilePath. Without it the shared logback.xml falls back to
# the literal string "logFilePath_IS_UNDEFINED" and tries to open it
# relative to the .app's CWD (which is "/" when launched via `open`),
# producing a Read-only file system error in ui-console.log.
UI_LOG_FILE="$(pwd)/logs/ui.log"
case "$UI_LAUNCHER" in
    ui/OpenRDR.app/Contents/MacOS/OpenRDR)
        open -W \
            --env API_KEY="${API_KEY:-}" \
            --env JAVA_TOOL_OPTIONS="-DlogFilePath=$UI_LOG_FILE" \
            --stdout "$(pwd)/logs/ui-console.log" \
            --stderr "$(pwd)/logs/ui-console.log" \
            ui/OpenRDR.app
        ;;
    *)
        JAVA_TOOL_OPTIONS="-DlogFilePath=$UI_LOG_FILE" \
            "$UI_LAUNCHER" >logs/ui-console.log 2>&1
        ;;
esac
