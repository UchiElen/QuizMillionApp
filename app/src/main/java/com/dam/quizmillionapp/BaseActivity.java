package com.dam.quizmillionapp;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class BaseActivity extends AppCompatActivity {
    public static boolean isMuted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                View view = getCurrentFocus();
                if (view != null) {
                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

                    if (imm != null && imm.isActive(view)) {
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        view.clearFocus();
                        return;
                    }
                }

                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
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
        if (findViewById(R.id.my_toolbar) != null) {
            getMenuInflater().inflate(R.menu.music_menu, menu);
            MenuItem musicItem = menu.findItem(R.id.action_music);
            if (musicItem != null) updateMenuIcon(musicItem);
            return true;
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicService musicService = MusicService.getInstance();
        if (musicService != null) {
            musicService.pauseMusicForBackground();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicService musicService = MusicService.getInstance();
        if (musicService != null) {
            musicService.resumeMusicFromBackground();
        }
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
        MusicService service = MusicService.getInstance();

        if (service != null) {
            service.setMuted(isMuted);
        }
    }

    private void updateMenuIcon(MenuItem item) {
        if (isMuted) {
            item.setIcon(R.drawable.ic_music_off);
        } else {
            item.setIcon(R.drawable.ic_music_on);
        }
    }


    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (getCurrentFocus() != null) {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        return super.dispatchTouchEvent(ev);
    }
}