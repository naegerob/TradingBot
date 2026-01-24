import argparse
import json
from pathlib import Path
from datetime import datetime, timezone

import matplotlib.pyplot as plt


# 

def parse_iso8601(ts: str) -> datetime:
    # Handles "2024-01-03T00:00:00Z" and also "...+00:00"
    if ts.endswith("Z"):
        ts = ts[:-1] + "+00:00"
    return datetime.fromisoformat(ts).astimezone(timezone.utc)


def extract_symbol_and_bars(payload):
    if isinstance(payload, dict) and "bars" in payload and isinstance(payload["bars"], dict):
        bars_map = payload["bars"]
        symbols = list(bars_map.keys())
        if len(symbols) != 1:
            raise ValueError(f"Expected exactly 1 symbol in payload['bars'], found: {symbols}")
        symbol = symbols[0]
        return symbol, bars_map[symbol]

    if isinstance(payload, dict):
        # pseudo-response: first key that isn't next_page_token
        keys = [k for k in payload.keys() if k != "next_page_token"]
        if len(keys) != 1:
            raise ValueError(f"Expected exactly 1 symbol key in payload, found: {keys}")
        symbol = keys[0]
        if not isinstance(payload[symbol], list):
            raise ValueError("Unsupported JSON shape: symbol key is not a list of bars")
        return symbol, payload[symbol]

    if isinstance(payload, list):
        raise ValueError("Unsupported JSON shape: raw array. Please store JSON with a symbol wrapper (Alpaca style).")

    raise ValueError("Unsupported JSON shape")


def iter_json_files(path: Path) -> list[Path]:
    print(path)
    if path.is_dir():
        pattern = "*.json"
        files = sorted(p for p in path.glob(pattern) if p.is_file())
        return files

    if path.is_file() and path.suffix.lower() == ".json":
        return [path]

    raise SystemExit(f"Path is neither a folder nor a .json file: {path.resolve()}")

### How to start this script:
###  <Root path>\scripts> python .\plot_bars.py --show-ohlcv --input ..\src\test\kotlin\com\example\backtestdata\

def main():
    ap = argparse.ArgumentParser(description="Plot Alpaca-style bar JSON fixtures (all on one chart)")
    ap.add_argument(
        "--input",
        help="One or more folders and/or .json files. If a folder is given, all *.json inside are plotted.",
    )
    ap.add_argument("--show-ohlcv", action="store_true", help="Overlay open/high/low/vwap (close always plotted)")
    args = ap.parse_args()
    # Collect & de-duplicate files while keeping stable order
    files: list[Path] = []
    seen: set[str] = set()
    for file in iter_json_files(Path(args.input)):
        key = str(file.resolve()).lower()
        if key not in seen:
            seen.add(key)
            files.append(file)
    if not files:
        raise SystemExit("No .json files found in given input(s).")

    plt.figure(figsize=(12, 6))

    for path in files:
        payload = json.loads(path.read_text(encoding="utf-8"))
        symbol, bars = extract_symbol_and_bars(payload)

        bars_sorted = sorted(bars, key=lambda b: parse_iso8601(b["t"]))
        x = [parse_iso8601(b["t"]) for b in bars_sorted]

        close = [float(b["c"]) for b in bars_sorted]
        plt.plot(x, close, label=f"{symbol} close ({path.name})", linewidth=1.5)

        if args.show_ohlcv:
            open_ = [float(b["o"]) for b in bars_sorted]
            high = [float(b["h"]) for b in bars_sorted]
            low = [float(b["l"]) for b in bars_sorted]
            vwap = [float(b.get("vw", "nan")) for b in bars_sorted]

            plt.plot(x, open_, label=f"{symbol} open ({path.name})", linewidth=1.0, alpha=0.5)
            plt.plot(x, high, label=f"{symbol} high ({path.name})", linewidth=1.0, alpha=0.35)
            plt.plot(x, low, label=f"{symbol} low ({path.name})", linewidth=1.0, alpha=0.35)
            plt.plot(x, vwap, label=f"{symbol} vwap ({path.name})", linewidth=1.0, alpha=0.5)

    plt.title("Bars (all series)")
    plt.xlabel("Time (UTC)")
    plt.ylabel("Price")
    plt.grid(True, alpha=0.25)
    plt.legend()
    plt.tight_layout()
    plt.show()


if __name__ == "__main__":
    main()
