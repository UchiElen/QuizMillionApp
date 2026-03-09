package com.dam.quizmillionapp;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class BaseActivity extends AppCompatActivity {
    // Variable estática para mantener el estado global
    public static boolean isMuted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // El diseño se extiende bajo la barra de estado y el toolbar
        EdgeToEdge.enable(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Solo inflar si el Toolbar existe en esta actividad
        if (findViewById(R.id.my_toolbar) != null) {
            getMenuInflater().inflate(R.menu.music_menu, menu);
            MenuItem musicItem = menu.findItem(R.id.action_music);
            if (musicItem != null) updateMenuIcon(musicItem);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_music) {
            isMuted = !isMuted;
            toggleMusic();
            updateMenuIcon(item);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleMusic() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            // Forma segura de mutear/desmutear en versiones modernas
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    isMuted ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE, 0);
        }
    }

    private void updateMenuIcon(MenuItem item) {
        item.setIcon(isMuted ? R.drawable.ic_music_off : R.drawable.ic_music_on);
    }
}