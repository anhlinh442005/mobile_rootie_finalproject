package com.veganbeauty.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.veganbeauty.app.features.profile.AccountProfileFragment;
import com.veganbeauty.app.features.shop.product.list.ShopListFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, new AccountProfileFragment())
                .commit();
        }
    }
}
