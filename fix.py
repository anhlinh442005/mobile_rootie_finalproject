import re
import os

xml_path = r'd:\ANHLINH\AndroidProjects\ROOTIE\app\src\main\res\layout\fragment_skin_allergy_profile.xml'
with open(xml_path, 'r', encoding='utf-8') as f:
    xml = f.read()

# 1. Update progress bar heights and margins for the first section
xml = xml.replace('android:layout_height="8dp"\n                            android:layout_weight="1"\n                            android:progress="80"\n                            android:progressDrawable="@drawable/progress_sensitivity"',
                  'android:layout_height="12dp"\n                            android:layout_marginStart="8dp"\n                            android:layout_marginEnd="8dp"\n                            android:layout_weight="1"\n                            android:progress="80"\n                            android:progressDrawable="@drawable/progress_sensitivity"')

xml = xml.replace('android:layout_height="8dp"\n                            android:layout_weight="1"\n                            android:progress="60"\n                            android:progressDrawable="@drawable/progress_hydration"',
                  'android:layout_height="12dp"\n                            android:layout_marginStart="8dp"\n                            android:layout_marginEnd="8dp"\n                            android:layout_weight="1"\n                            android:progress="60"\n                            android:progressDrawable="@drawable/progress_hydration"')

xml = xml.replace('android:layout_height="8dp"\n                            android:layout_weight="1"\n                            android:progress="75"\n                            android:progressDrawable="@drawable/progress_elasticity"',
                  'android:layout_height="12dp"\n                            android:layout_marginStart="8dp"\n                            android:layout_marginEnd="8dp"\n                            android:layout_weight="1"\n                            android:progress="75"\n                            android:progressDrawable="@drawable/progress_elasticity"')

xml = xml.replace('android:layout_height="8dp"\n                            android:layout_weight="1"\n                            android:progress="85"\n                            android:progressDrawable="@drawable/progress_oily"',
                  'android:layout_height="12dp"\n                            android:layout_marginStart="8dp"\n                            android:layout_marginEnd="8dp"\n                            android:layout_weight="1"\n                            android:progress="85"\n                            android:progressDrawable="@drawable/progress_oily"')


# 2. Re-write the Comparison section safely
# Let's extract the exact substring for each chart row and replace it with the compact row
def get_compact(id_prefix, title, icon):
    return f'''                    <!-- {id_prefix} Compact Row -->
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
                            android:text="{title}"
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

# Find the start and end of the comparison rows block
start_idx = xml.find('<!-- Hydration Chart Row -->')
end_idx = xml.find('<!-- Divider -->\n                    <View\n                        android:layout_width="match_parent"\n                        android:layout_height="1dp"\n                        android:background="#EAECE3"\n                        android:layout_marginBottom="14dp" />\n\n                    <!-- Product Efficacy Analysis Title -->')

if start_idx != -1 and end_idx != -1:
    before = xml[:start_idx]
    after = xml[end_idx:]
    
    compact_block = (
        get_compact('hydration', '💧 Độ ẩm da', '') +
        get_compact('sebum', '🧪 Độ dầu (Sebum)', '') +
        get_compact('sensitivity', '🛡️ Độ nhạy cảm', '') +
        get_compact('elasticity', '✨ Độ đàn hồi', '')
    )
    xml = before + compact_block + after

# 3. Product Efficacy Analysis Text Box
efficacy_target = '''                    <!-- Product Efficacy Analysis Title -->
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:fontFamily="@font/lexend"
                        android:text="Phân tích hiệu quả sản phẩm đang dùng"
                        android:textColor="#3E4D44"
                        android:textSize="13sp"
                        android:textStyle="bold"
                        android:layout_marginBottom="8dp" />

                    <!-- Efficacy details text -->
                    <TextView
                        android:id="@+id/skin_tv_product_efficacy_analysis"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:fontFamily="@font/lexend"
                        android:text="Chưa có đủ dữ liệu lịch sử để phân tích hiệu quả sản phẩm."
                        android:textColor="#3E4D44"
                        android:textSize="12sp"
                        android:lineSpacingMultiplier="1.3" />'''

efficacy_replacement = '''                    <!-- Product Efficacy Analysis -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:background="@drawable/bg_expert_reason_card"
                        android:backgroundTint="#F2FBF6"
                        android:padding="16dp">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:fontFamily="@font/lexend"
                            android:text="Phân tích hiệu quả sản phẩm đang dùng"
                            android:textColor="#02542D"
                            android:textSize="14sp"
                            android:textStyle="bold"
                            android:layout_marginBottom="8dp" />

                        <!-- Efficacy details text -->
                        <TextView
                            android:id="@+id/skin_tv_product_efficacy_analysis"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:fontFamily="@font/lexend"
                            android:text="Chưa có đủ dữ liệu lịch sử để phân tích hiệu quả sản phẩm."
                            android:textColor="#3E4D44"
                            android:textSize="14sp"
                            android:lineSpacingMultiplier="1.4" />
                    </LinearLayout>'''
xml = xml.replace(efficacy_target, efficacy_replacement)

# 4. AI Recommendation Card
ai_card_target = '''            <!-- Section: AI Recommendations -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                app:cardBackgroundColor="#3E4D44"
                app:cardCornerRadius="20dp"
                app:cardElevation="4dp"
                app:cardUseCompatPadding="true">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="20dp">

                    <LinearLayout
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:layout_marginBottom="10dp">

                        <ImageView
                            android:layout_width="24dp"
                            android:layout_height="24dp"
                            android:layout_marginEnd="8dp"
                            android:src="@drawable/ic_ai_outline"
                            app:tint="@color/nav_icon_color" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:fontFamily="@font/lexend"
                            android:text="Lời khuyên từ Rootie AI"
                            android:textColor="#FFFFFF"
                            android:textSize="18sp"
                            android:textStyle="bold" />
                    </LinearLayout>

                    <TextView
                        android:id="@+id/tv_recommendation"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:fontFamily="@font/lexend"
                        android:text="Với nền da dầu nhạy cảm, ưu tiên hàng đầu của bạn là làm sạch nhẹ nhàng và phục hồi hàng rào bảo vệ da. Tránh các loại sữa rửa mặt tạo bọt quá mạnh hoặc chứa sulfate. Hãy bổ sung tinh chất Rau má để làm dịu các vùng da đỏ và duy trì độ ẩm bằng các loại gel dưỡng mỏng nhẹ không gây bít tắc lỗ chân lông."
                        android:textColor="#FFFFFF"
                        android:textSize="15sp"
                        android:lineSpacingMultiplier="1.45" />
                </LinearLayout>
            </androidx.cardview.widget.CardView>'''

ai_card_replacement = '''            <!-- Section: AI Recommendations -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                app:cardBackgroundColor="#F4F8FB"
                app:cardCornerRadius="20dp"
                app:cardElevation="4dp"
                app:cardUseCompatPadding="true">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="20dp">

                    <LinearLayout
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:layout_marginBottom="10dp">

                        <ImageView
                            android:layout_width="24dp"
                            android:layout_height="24dp"
                            android:layout_marginEnd="8dp"
                            android:src="@drawable/ic_ai_outline"
                            app:tint="#0D6EFD" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:fontFamily="@font/lexend"
                            android:text="Lời khuyên từ Rootie AI"
                            android:textColor="#0D6EFD"
                            android:textSize="18sp"
                            android:textStyle="bold" />
                    </LinearLayout>

                    <TextView
                        android:id="@+id/tv_recommendation"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:fontFamily="@font/lexend"
                        android:text="Với nền da dầu nhạy cảm, ưu tiên hàng đầu của bạn là làm sạch nhẹ nhàng và phục hồi hàng rào bảo vệ da. Tránh các loại sữa rửa mặt tạo bọt quá mạnh hoặc chứa sulfate. Hãy bổ sung tinh chất Rau má để làm dịu các vùng da đỏ và duy trì độ ẩm bằng các loại gel dưỡng mỏng nhẹ không gây bít tắc lỗ chân lông."
                        android:textColor="#1F2937"
                        android:textSize="15sp"
                        android:lineSpacingMultiplier="1.45" />
                </LinearLayout>
            </androidx.cardview.widget.CardView>'''
xml = xml.replace(ai_card_target, ai_card_replacement)

with open(xml_path, 'w', encoding='utf-8') as f:
    f.write(xml)

# Java changes
java_path = r'd:\ANHLINH\AndroidProjects\ROOTIE\app\src\main\java\com\veganbeauty\app\features\profile\SkinAllergyProfileFragment.java'
with open(java_path, 'r', encoding='utf-8') as f:
    java = f.read()

java = re.sub(r'binding\.skinCompare\w+Progress(?:Old)?\.setProgress\(.*?\);\n\s*', '', java)

with open(java_path, 'w', encoding='utf-8') as f:
    f.write(java)
