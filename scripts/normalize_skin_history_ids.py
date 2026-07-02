import json
from datetime import datetime, timedelta, timezone
from pathlib import Path

SKIN_HISTORY_PATH = Path(__file__).resolve().parents[1] / "app/src/main/assets/skin_history.json"
TZ = timezone(timedelta(hours=7))


def to_timestamp_millis(date_value: str, time_value: str) -> int:
    parsed = datetime.strptime(f"{date_value} {time_value}", "%d/%m/%Y %H:%M").replace(tzinfo=TZ)
    return int(parsed.timestamp() * 1000)


def normalize_skin_history_ids() -> None:
    with SKIN_HISTORY_PATH.open("r", encoding="utf-8") as file:
        records = json.load(file)

    used_ids = set()
    changes = []

    for record in records:
        old_id = record.get("id", "")
        timestamp_millis = to_timestamp_millis(record["date"], record["time"])

        while f"sh_{timestamp_millis}" in used_ids:
            timestamp_millis += 1

        new_id = f"sh_{timestamp_millis}"
        used_ids.add(new_id)

        if old_id != new_id:
            changes.append((old_id, new_id, record.get("userId"), record.get("date"), record.get("time")))

        record["id"] = new_id

    with SKIN_HISTORY_PATH.open("w", encoding="utf-8") as file:
        json.dump(records, file, ensure_ascii=False, indent=2)
        file.write("\n")

    print(f"Updated {len(changes)} / {len(records)} records")
    for old_id, new_id, user_id, date_value, time_value in changes:
        print(f"{old_id} -> {new_id} | {user_id} | {date_value} {time_value}")


if __name__ == "__main__":
    normalize_skin_history_ids()
