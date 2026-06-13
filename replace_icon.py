import os

res_dir = r"d:\ANHLINH\AndroidProjects\Rootie\app\src\main\res\layout"

for root, dirs, files in os.walk(res_dir):
    for file in files:
        if file.endswith(".xml"):
            file_path = os.path.join(root, file)
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()
            if "@drawable/ic_back" in content:
                content = content.replace("@drawable/ic_back", "@drawable/ic_chevron_left")
                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(content)
                print("Updated", file)
