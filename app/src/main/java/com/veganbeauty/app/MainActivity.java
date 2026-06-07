package com.veganbeauty.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.veganbeauty.app.features.quiz.QuizTestIntroFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            android.content.SharedPreferences prefs = getSharedPreferences("RootieQuizPrefs", MODE_PRIVATE);
            String savedSkin = prefs.getString("SAVED_USER_SKIN_TYPE", null);
            androidx.fragment.app.Fragment destination;
            if (savedSkin != null) {
                destination = new com.veganbeauty.app.features.weather.WeatherForecastFragment();
            } else {
                destination = new QuizTestIntroFragment();
            }
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, destination)
                .commit();
        }
    }
}

