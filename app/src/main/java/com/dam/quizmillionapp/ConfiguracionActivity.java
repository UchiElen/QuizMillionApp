package com.dam.quizmillionapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class ConfiguracionActivity extends BaseActivity {

    private SwitchMaterial switchMusica;
    private SwitchMaterial switchSonido;
    private SwitchMaterial switchVibracion;

    private SharedPreferences prefs;

    public static final String PREFS_NAME = "app_settings";
    public static final String KEY_SOUND = "sound_enabled";
    public static final String KEY_VIBRATION = "vibration_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracion);

        switchMusica = findViewById(R.id.switchMusica);
        switchSonido = findViewById(R.id.switchSonido);
        switchVibracion = findViewById(R.id.switchVibracion);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        syncMusicSwitch();
        switchSonido.setChecked(prefs.getBoolean(KEY_SOUND, true));
        switchVibracion.setChecked(prefs.getBoolean(KEY_VIBRATION, true));

        switchMusica.setOnCheckedChangeListener((buttonView, isChecked) -> {

            boolean newMutedState = !isChecked;

            if (BaseActivity.isMuted != newMutedState) {

                BaseActivity.isMuted = newMutedState;

                MusicService service = MusicService.getInstance();
                if (service != null) {
                    service.setMuted(BaseActivity.isMuted);
                }

                invalidateOptionsMenu();
            }
        });

        switchSonido.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_SOUND, isChecked).apply();

            if (isChecked) {
                SoundManager.getInstance(ConfiguracionActivity.this).playClick();
            }
        });

        switchVibracion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_VIBRATION, isChecked).apply();
            if (isChecked) {
                vibratePhone();
            }
        });
    }

    private void syncMusicSwitch() {
        if (switchMusica != null) {
            switchMusica.setChecked(!BaseActivity.isMuted);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        boolean handled = super.onOptionsItemSelected(item);

        if (item.getItemId() == R.id.action_music) {
            syncMusicSwitch();
        }

        return handled;
    }

    private void vibratePhone() {
        android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(150, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(150);
            }
        }
    }

    @Override
    protected boolean shouldCheckInternetOnResume() {
        return false;
    }


}