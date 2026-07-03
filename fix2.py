import re

xml_path = r'd:\ANHLINH\AndroidProjects\ROOTIE\app\src\main\res\layout\fragment_skin_allergy_profile.xml'
with open(xml_path, 'r', encoding='utf-8') as f:
    xml = f.read()

# 1. Update Comparison rows to use drawableStart instead of emojis
xml = xml.replace('android:text="💧 Độ ẩm da"', 'android:text="Độ ẩm da"\n                            android:drawableStart="@drawable/ic_skin_moisture"\n                            android:drawablePadding="8dp"\n                            android:drawableTint="#3E4D44"')

xml = xml.replace('android:text="🧪 Độ dầu (Sebum)"', 'android:text="Độ dầu (Sebum)"\n                            android:drawableStart="@drawable/quiz_ic_option_oily"\n                            android:drawablePadding="8dp"\n                            android:drawableTint="#3E4D44"')

xml = xml.replace('android:text="🛡️ Độ nhạy cảm"', 'android:text="Độ nhạy cảm"\n                            android:drawableStart="@drawable/ic_skin_sensitivity"\n                            android:drawablePadding="8dp"\n                            android:drawableTint="#3E4D44"')

xml = xml.replace('android:text="✨ Độ đàn hồi"', 'android:text="Độ đàn hồi"\n                            android:drawableStart="@drawable/ic_skin_star"\n                            android:drawablePadding="8dp"\n                            android:drawableTint="#3E4D44"')

# 2. Re-adjust AI Card
ai_card_target = '''            <!-- Section: AI Recommendations -->
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

ai_card_replacement = '''            <!-- Section: AI Recommendations -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                app:cardBackgroundColor="#3E4D44"
                app:cardCornerRadius="20dp"
                app:cardElevation="6dp"
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
                        android:layout_marginBottom="12dp">

                        <ImageView
                            android:layout_width="26dp"
                            android:layout_height="26dp"
                            android:layout_marginEnd="10dp"
                            android:src="@drawable/ic_skin_ai_sparkles"
                            app:tint="#EAECE3" />

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
                        android:textColor="#F7F9FA"
                        android:textSize="15sp"
                        android:lineSpacingMultiplier="1.4" />
                </LinearLayout>
            </androidx.cardview.widget.CardView>'''

xml = xml.replace(ai_card_target, ai_card_replacement)

with open(xml_path, 'w', encoding='utf-8') as f:
    f.write(xml)

print('Updated correctly.')
