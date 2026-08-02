from pathlib import Path

LOG_FILE = Path("C:/Users/41786/bot_logs.txt")

HEALTH_KEYWORDS = [
    "health",
    "healthcheck",
    "ping",
    "alive"
]

last_real_entry = None

with open(LOG_FILE, "r", encoding="utf-8") as f:
    for line in f:
        lower = line.lower()

        # ignore health check lines
        if any(keyword in lower for keyword in HEALTH_KEYWORDS):
            continue

        # keep the latest non-health entry
        last_real_entry = line.rstrip()


if last_real_entry:
    print("Last non-health log entry:")
    print(last_real_entry)
else:
    print("No non-health entries found.")