#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

if [ -z "${API_KEY:-}" ]; then
    echo
    echo "WARNING: API_KEY environment variable is not set."
    echo "  Rule-condition generation via Google Gemini will not work until it is."
    echo "  export API_KEY=your-google-gemini-api-key"
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
"$UI_LAUNCHER" >logs/ui-console.log 2>&1
