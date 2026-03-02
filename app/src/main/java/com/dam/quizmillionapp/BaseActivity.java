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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Buscar el Toolbar en el diseño de la actividad que esté abierta
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            // Quitar el título de la app
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.music_menu, menu);
        MenuItem musicItem = menu.findItem(R.id.action_music);
        updateMenuIcon(musicItem);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_music) {
            isMuted = !isMuted;
            updateMusicStatus();
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (isMuted) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
            } else {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
            }
            updateMenuIcon(item);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateMenuIcon(MenuItem item) {
        if (isMuted) {
            item.setIcon(R.drawable.ic_music_off); // Icono con la raya
        } else {
            item.setIcon(R.drawable.ic_music_on);  // Icono normal
        }
    }

    private void updateMusicStatus() {
    }
}