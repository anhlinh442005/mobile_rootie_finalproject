import json
from collections import defaultdict
from pathlib import Path

USERS_PATH = Path(__file__).resolve().parents[1] / "app/src/main/assets/users.json"


def norm_email(value):
    return (value or "").strip().lower()


def norm_phone(value):
    digits = "".join(ch for ch in (value or "") if ch.isdigit())
    if digits.startswith("84") and len(digits) > 9:
        digits = "0" + digits[2:]
    return digits


def main():
    with USERS_PATH.open("r", encoding="utf-8") as file:
        users = json.load(file)

    users_by_id = {}
    email_map = defaultdict(list)
    phone_map = defaultdict(list)

    for user in users:
        user_id = user.get("user_id") or user.get("id") or ""
        users_by_id[user_id] = user

        email = norm_email(user.get("email"))
        phone = norm_phone(user.get("phone"))

        if email:
            email_map[email].append(user_id)
        if phone:
            phone_map[phone].append(user_id)

    email_dups = {key: sorted(set(values)) for key, values in email_map.items() if len(set(values)) > 1}
    phone_dups = {key: sorted(set(values)) for key, values in phone_map.items() if len(set(values)) > 1}

    print("=== TONG QUAN ===")
    print(f"Tong so user: {len(users)}")
    print(f"So email unique: {len(email_map)}")
    print(f"So phone unique: {len(phone_map)}")
    print(f"Email trung (>=2 userId): {len(email_dups)}")
    print(f"Phone trung (>=2 userId): {len(phone_dups)}")
    print()

    if email_dups:
        print("=== EMAIL TRUNG ===")
        for email, user_ids in sorted(email_dups.items(), key=lambda item: (-len(item[1]), item[0])):
            print(f"Email: {email}")
            print(f"  userIds ({len(user_ids)}): {user_ids}")
            for user_id in user_ids:
                user = users_by_id.get(user_id, {})
                print(
                    f"    - {user_id} | {user.get('full_name', '')} | "
                    f"phone={user.get('phone', '')} | username={user.get('username', '')}"
                )
            print()

    if phone_dups:
        print("=== SO DIEN THOAI TRUNG ===")
        for phone, user_ids in sorted(phone_dups.items(), key=lambda item: (-len(item[1]), item[0])):
            print(f"Phone: {phone}")
            print(f"  userIds ({len(user_ids)}): {user_ids}")
            for user_id in user_ids:
                user = users_by_id.get(user_id, {})
                print(
                    f"    - {user_id} | {user.get('full_name', '')} | "
                    f"email={user.get('email', '')} | username={user.get('username', '')}"
                )
            print()

    duplicate_user_ids = set()
    for user_ids in email_dups.values():
        duplicate_user_ids.update(user_ids)
    for user_ids in phone_dups.values():
        duplicate_user_ids.update(user_ids)

    print("=== DANH SACH USER BI TRUNG ===")
    for user_id in sorted(duplicate_user_ids):
        user = users_by_id.get(user_id, {})
        email = norm_email(user.get("email"))
        phone = norm_phone(user.get("phone"))
        print(
            f"{user_id} | {user.get('full_name', '')} | email={user.get('email', '')} | phone={user.get('phone', '')}"
        )
        if email in email_dups and len(email_dups[email]) > 1:
            others = [uid for uid in email_dups[email] if uid != user_id]
            if others:
                print(f"  Trung email voi: {others}")
        if phone in phone_dups and len(phone_dups[phone]) > 1:
            others = [uid for uid in phone_dups[phone] if uid != user_id]
            if others:
                print(f"  Trung phone voi: {others}")


if __name__ == "__main__":
    main()
