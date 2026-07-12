import re

file_path = 'app/src/main/res/layout/shop_fragment_checkout.xml'
with open(file_path, 'r', encoding='utf-8') as f:
    text = f.read()

# Remove elevation="2dp" from all blocks to make them flat like TikTok
text = text.replace('android:elevation="2dp"', 'android:elevation="0dp"')

# Adjust margins to have clean separations
text = text.replace('android:layout_marginTop="8dp"\n                android:layout_marginBottom="4dp"', 'android:layout_marginTop="8dp"')

bottom_bar_regex = re.compile(r'<!-- Sticky Bottom Bar -->.*?</LinearLayout>', re.DOTALL)
new_bottom_bar = '''<!-- Sticky Bottom Bar -->
    <LinearLayout
        android:id="@+id/clBottomBar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="#FFFFFF"
        android:elevation="8dp"
        android:orientation="vertical"
        android:paddingHorizontal="16dp"
        android:paddingVertical="12dp"
        app:layout_constraintBottom_toBottomOf="parent">

        <RelativeLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="12dp">
            
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:fontFamily="@font/be_vietnam_pro_medium"
                android:text="Tổng (1 mặt hàng)"
                android:textColor="@color/primary"
                android:textSize="16sp"
                android:layout_alignParentStart="true"
                android:layout_centerVertical="true" />
                
            <LinearLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:gravity="end"
                android:layout_alignParentEnd="true">
                
                <TextView
                    android:id="@+id/tvTotalValue"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:fontFamily="@font/be_vietnam_pro_bold"
                    android:textColor="#B4202A"
                    android:textSize="18sp"
                    tools:text="40.650đ" />
                    
                <TextView
                    android:id="@+id/tvSavingsValue"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:fontFamily="@font/be_vietnam_pro_regular"
                    android:textColor="#B4202A"
                    android:textSize="12sp"
                    tools:text="Tiết kiệm 81.000đ" />
            </LinearLayout>
        </RelativeLayout>

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnCheckout"
            android:layout_width="match_parent"
            android:layout_height="48dp"
            android:backgroundTint="@color/primary"
            android:fontFamily="@font/be_vietnam_pro_bold"
            android:text="Đặt hàng"
            android:textAllCaps="false"
            android:textColor="@android:color/white"
            app:cornerRadius="24dp" />

    </LinearLayout>'''

text = bottom_bar_regex.sub(new_bottom_bar, text)

thanh_tien_regex = re.compile(r'<!-- Thành tiền row -->.*?</LinearLayout>', re.DOTALL)
text = thanh_tien_regex.sub('', text)

tiet_kiem_regex = re.compile(r'<!-- Tiết kiệm được row -->.*?</LinearLayout>', re.DOTALL)
text = tiet_kiem_regex.sub('', text)

hide_info_block = '''            <!-- 3.1 Ẩn thông tin sản phẩm khi giao hàng -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:background="#FFFFFF"
                android:elevation="0dp"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:paddingHorizontal="16dp"
                android:paddingVertical="14dp"
                android:layout_marginTop="8dp">

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:fontFamily="@font/be_vietnam_pro_bold"
                    android:text="Ẩn thông tin sản phẩm khi giao hàng"
                    android:textColor="@color/primary"
                    android:textSize="14sp" />

                <FrameLayout
                    android:id="@+id/switchHideProductInfo"
                    android:layout_width="48dp"
                    android:layout_height="30dp"
                    android:background="@drawable/ic_switch_track_off"
                    android:clickable="true"
                    android:focusable="true">

                    <ImageView
                        android:id="@+id/switchHideProductInfoThumb"
                        android:layout_width="26dp"
                        android:layout_height="26dp"
                        android:layout_gravity="center_vertical|start"
                        android:layout_marginStart="2dp"
                        android:src="@drawable/bg_white_rounded_corner_border" />
                </FrameLayout>
            </LinearLayout>
'''
text = text.replace('<!-- 3. Yêu cầu xuất hóa đơn điện tử -->', hide_info_block + '\n            <!-- 3. Yêu cầu xuất hóa đơn điện tử -->')

text = text.replace('Thông tin thanh toán', 'Tóm tắt đơn hàng')
text = text.replace('Tổng tiền', 'Tổng phụ sản phẩm')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(text)
