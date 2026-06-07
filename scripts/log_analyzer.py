import json
from datetime import datetime, timedelta
from pathlib import Path

LOG_FILE = Path(__file__).resolve().parent / "../logs/bot.log"

HEALTH_KEYWORDS = ["health", "healthcheck", "ping", "alive"]

GAP_THRESHOLD = timedelta(minutes=10)  # adjust if needed

def parse_time(ts):
    return datetime.fromisoformat(ts.replace("Z", "+00:00"))

events = []

# --- Load + filter health checks ---
with open(LOG_FILE, "r") as f:
    for line in f:
        try:
            entry = json.loads(line)

            msg = entry.get("log", "").lower()
            if any(k in msg for k in HEALTH_KEYWORDS):
                continue

            ts = parse_time(entry["time"])
            events.append((ts, msg.strip()))

        except Exception:
            continue

# --- Sort by time ---
events.sort(key=lambda x: x[0])

if not events:
    print("No valid events found.")
    exit()

# --- Find last real event before silence ---
last_good = events[0]
last_time = events[0][0]

for ts, msg in events[1:]:
    gap = ts - last_time

    # if we suddenly have a big gap -> system likely died AFTER last_good
    if gap > GAP_THRESHOLD:
        print("\n🚨 SYSTEM STOP DETECTED")
        print("Last valid event BEFORE failure:")
        print("Timestamp:", last_good[0])
        print("Message:", last_good[1])
        print("\nFirst event AFTER gap:")
        print("Timestamp:", ts)
        print("Message:", msg)
        break

    last_good = (ts, msg)
    last_time = ts

else:
    print("No failure detected (log is continuous).")
    print("Last event:", events[-1])