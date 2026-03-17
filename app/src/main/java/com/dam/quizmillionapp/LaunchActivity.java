package com.dam.quizmillionapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LaunchActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        androidx.core.splashscreen.SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        SoundManager.getInstance(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_launch);

        try {
            Intent musicIntent = new Intent(LaunchActivity.this, MusicService.class);
            startService(musicIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        View mainLayout = findViewById(R.id.main);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        Button button = findViewById(R.id.LaunchBtn);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                SoundManager.getInstance(LaunchActivity.this).playClick();
                Intent intent = new Intent(LaunchActivity.this,WelcomeActivity.class);
                startActivity(intent);
            }
        });


    }
}