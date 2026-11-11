#!/usr/bin/env bash
set -Eeuo pipefail

# Keep the desktop and JavaFX in the same lifecycle as the container.
pids=()
cleanup() {
    trap - EXIT TERM INT
    kill "${pids[@]}" 2>/dev/null || true
    wait || true
}
trap cleanup EXIT
trap 'exit 0' TERM INT

Xvfb "$DISPLAY" -screen 0 1280x800x24 -nolisten tcp &
pids+=("$!")
ready=false
for ((attempt = 0; attempt < 50; attempt++)); do
    if xdpyinfo -display "$DISPLAY" >/dev/null 2>&1; then
        ready=true
        break
    fi
    sleep 0.1
done
if [[ "$ready" != true ]]; then
    echo 'The virtual display did not start.' >&2
    exit 1
fi

openbox --sm-disable &
pids+=("$!")
for ((attempt = 0; attempt < 50; attempt++)); do
    if xprop -root _NET_SUPPORTING_WM_CHECK | grep -q 'window id'; then
        break
    fi
    sleep 0.1
done
# VNC stays internal; only the browser endpoint is published on Mac localhost.
x11vnc -display "$DISPLAY" -forever -shared -nopw -localhost \
    -noxdamage -nowf -rfbport 5900 &
pids+=("$!")
websockify --web=/usr/share/novnc/ 6080 localhost:5900 &
pids+=("$!")
java -Dprism.order=sw \
    "-Dchaflow.host=${CHAFLOW_HOST:-chaflow-server}" \
    "-Dchaflow.port=${CHAFLOW_PORT:-5050}" \
    --module-path /app/lib:/app/chaflow.jar \
    --module com.chaflow.client/com.chaflow.client.ChaFlowApp &
pids+=("$!")

# Restart the entire desktop if any required process exits, including JavaFX.
wait -n "${pids[@]}" || true
exit 1
