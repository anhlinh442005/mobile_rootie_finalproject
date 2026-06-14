import re
with open('app/src/main/res/layout/com_fragment_profile.xml', 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('<TextView android:fontFamily=" @font/be_vietnam_pro\\', '<TextView android:fontFamily="@font/be_vietnam_pro"')
with open('app/src/main/res/layout/com_fragment_profile.xml', 'w', encoding='utf-8') as f:
    f.write(content)
