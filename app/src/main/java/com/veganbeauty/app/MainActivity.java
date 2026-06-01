package com.veganbeauty.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.veganbeauty.app.features.home.HomeFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, new HomeFragment())
                .commit();
        }
    }
}
