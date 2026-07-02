import json
from pathlib import Path

USERS_PATH = Path(__file__).resolve().parents[1] / "app/src/main/assets/users.json"


def norm_email(value):
    return (value or "").strip().lower()


def main():
    with USERS_PATH.open("r", encoding="utf-8") as file:
        users = json.load(file)

    seen_emails = set()
    kept = []
    removed = []

    for user in users:
        user_id = user.get("user_id") or user.get("id") or ""
        email = norm_email(user.get("email"))

        if email and email in seen_emails:
            removed.append(
                {
                    "user_id": user_id,
                    "full_name": user.get("full_name", ""),
                    "email": user.get("email", ""),
                    "phone": user.get("phone", ""),
                }
            )
            continue

        if email:
            seen_emails.add(email)
        kept.append(user)

    with USERS_PATH.open("w", encoding="utf-8") as file:
        json.dump(kept, file, ensure_ascii=False, indent=2)
        file.write("\n")

    print(f"Truoc: {len(users)} user")
    print(f"Sau: {len(kept)} user")
    print(f"Da xoa: {len(removed)} user")
    print()
    for item in removed:
        print(
            f"- {item['user_id']} | {item['full_name']} | "
            f"{item['email']} | {item['phone']}"
        )


if __name__ == "__main__":
    main()
