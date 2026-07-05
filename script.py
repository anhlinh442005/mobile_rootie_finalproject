import re

# Fix XML first
xml_path = r'd:\ANHLINH\AndroidProjects\ROOTIE\app\src\main\res\layout\fragment_skin_allergy_profile.xml'
with open(xml_path, 'r', encoding='utf-8') as f:
    xml = f.read()

# Revert emojis
xml = xml.replace('✨ Phân tích hiệu quả sản phẩm', 'Phân tích hiệu quả sản phẩm đang dùng')
xml = xml.replace('✨ Lời khuyên từ Rootie AI', 'Lời khuyên từ Rootie AI')

def make_compact_row(id_prefix, title, icon):
    return f'''
                    <!-- {id_prefix} Compact Row -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:layout_marginBottom="10dp"
                        android:background="@drawable/bg_expert_reason_card"
                        android:backgroundTint="#F7F9FA"
                        android:padding="14dp"
                        android:gravity="center_vertical">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_weight="1"
                            android:layout_height="wrap_content"
                            android:fontFamily="@font/lexend"
                            android:text="{icon} {title}"
                            android:textColor="#3E4D44"
                            android:textSize="13sp"
                            android:textStyle="bold" />

                        <LinearLayout
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:orientation="horizontal"
                            android:gravity="center_vertical">
                            <TextView
                                android:id="@+id/skin_compare_{id_prefix}_old"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:fontFamily="@font/lexend"
                                android:text="50%"
                                android:textColor="#7E8A83"
                                android:textSize="13sp" />
                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:layout_marginHorizontal="6dp"
                                android:text="➔"
                                android:textColor="#7E8A83"
                                android:textSize="12sp" />
                            <TextView
                                android:id="@+id/skin_compare_{id_prefix}_new"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:fontFamily="@font/lexend"
                                android:text="50%"
                                android:textColor="#3E4D44"
                                android:textStyle="bold"
                                android:textSize="13sp" />
                        </LinearLayout>

                        <TextView
                            android:id="@+id/skin_compare_{id_prefix}_diff"
                            android:layout_width="100dp"
                            android:layout_height="wrap_content"
                            android:gravity="end"
                            android:fontFamily="@font/lexend"
                            android:text="+0%"
                            android:textColor="#677559"
                            android:textSize="11sp"
                            android:textStyle="bold" />
                    </LinearLayout>
'''

compact_xml = (
    make_compact_row('hydration', 'Độ ẩm', '💧') +
    make_compact_row('sebum', 'Độ dầu', '🧪') +
    make_compact_row('sensitivity', 'Độ nhạy cảm', '🛡️') +
    make_compact_row('elasticity', 'Độ đàn hồi', '✨')
)

start_str = '<!-- Hydration Chart Row -->'
end_str = '<!-- Divider -->'
if start_str in xml and end_str in xml:
    before = xml.split(start_str)[0]
    after = end_str + xml.split(end_str, 1)[1]
    xml = before + compact_xml + '                    ' + after

with open(xml_path, 'w', encoding='utf-8') as f:
    f.write(xml)

# Fix Java code
java_path = r'd:\ANHLINH\AndroidProjects\ROOTIE\app\src\main\java\com\veganbeauty\app\features\profile\SkinAllergyProfileFragment.java'
with open(java_path, 'r', encoding='utf-8') as f:
    java = f.read()

# Remove progress bar setting lines
java = re.sub(r'binding\.skinCompare\w+Progress\w*\.setProgress\(.*?\);\n\s*', '', java)

with open(java_path, 'w', encoding='utf-8') as f:
    f.write(java)

print('Updated XML and Java successfully')
