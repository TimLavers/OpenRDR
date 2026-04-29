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

SERVER_JAR=$(ls server/openrdr-*.jar 2>/dev/null | head -n1 || true)
if [ -z "$SERVER_JAR" ]; then
    echo "ERROR: could not find server/openrdr-*.jar" >&2
    exit 1
fi

# Locate the UI launcher (Compose createDistributable layout differs per OS).
UI_LAUNCHER=""
for candidate in \
    ui/OpenRDR.app/Contents/MacOS/OpenRDR \
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

echo "Starting OpenRDR server (in-memory mode, port 9090) ..."
java -jar "$SERVER_JAR" InMemory &
SERVER_PID=$!
trap 'kill $SERVER_PID 2>/dev/null || true' EXIT

echo "Waiting for the server to accept connections ..."
for i in $(seq 1 60); do
    if (echo >/dev/tcp/localhost/9090) >/dev/null 2>&1; then
        break
    fi
    sleep 1
done

echo "Launching OpenRDR UI ..."
"$UI_LAUNCHER"
