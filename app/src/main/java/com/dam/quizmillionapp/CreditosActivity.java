package com.dam.quizmillionapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.preference.PreferenceManager;
import androidx.activity.OnBackPressedCallback;


public class CreditosActivity extends BaseActivity {

    private ScrollView scrollView;
    private LinearLayout layoutCreditos;
    private Button btnVolverMenu;

    private Handler handler = new Handler();
    private int velocidad = 5;

    private Runnable scrollRunnable = new Runnable() {
        @Override
        public void run() {
            int y = scrollView.getScrollY();
            int maxScroll = scrollView.getChildAt(0).getHeight() - scrollView.getHeight();

            if (y < maxScroll) {
                scrollView.scrollBy(0, velocidad);
                handler.postDelayed(this, 30);
            } else {
                handler.postDelayed(() -> mostrarBoton(), 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creditos);

        scrollView = findViewById(R.id.scrollCreditos);
        layoutCreditos = findViewById(R.id.layoutCreditos); // Necesitamos el layout para el truco del padding
        btnVolverMenu = findViewById(R.id.btnVolverMenu);

        gestionarMusica();

        btnVolverMenu.setOnClickListener(v -> volverMenu());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                volverMenu();
            }
        });
        scrollView.post(() -> prepararYArrancarScroll());
    }

    private void prepararYArrancarScroll() {
               int screenHeight = scrollView.getHeight();
        layoutCreditos.setPadding(0, screenHeight, 0, screenHeight);

        handler.postDelayed(scrollRunnable, 1000);
    }

    private void mostrarBoton() {
        btnVolverMenu.setVisibility(View.VISIBLE);
    }

    private void volverMenu() {
        SoundManager.getInstance(CreditosActivity.this).playClick();
        handler.removeCallbacksAndMessages(null);

        MusicService musicService = MusicService.getInstance();
        if (musicService != null) {
            musicService.playMusic(R.raw.backgroundmusic);
        }

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void gestionarMusica() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean musicaActiva = prefs.getBoolean("music_enabled", true);

        if (musicaActiva) {
            MusicService musicService = MusicService.getInstance();

            if (musicService != null) {
                musicService.playMusic(R.raw.creditos);
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}