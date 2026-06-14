import os
import shutil

src_path = r"C:\Users\user\.gemini\antigravity\brain\c40fec65-7779-4c00-88bb-ba45460d2914\media__1781252017913.jpg"
dest_path = r"app\src\main\res\drawable\mascot_message.png"

# Let's delete the old mascot_message.png first to make sure there's no conflict
if os.path.exists(dest_path):
    os.remove(dest_path)

try:
    from PIL import Image
    im = Image.open(src_path)
    im.save(dest_path, "PNG")
    print("Successfully converted and saved avatar as PNG.")
except Exception as e:
    print(f"PIL not available or failed: {e}. Falling back to copying file directly.")
    shutil.copy(src_path, dest_path)
    print("Successfully copied avatar directly to mascot_message.png.")
