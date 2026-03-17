package com.dam.quizmillionapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;


public class SoundManager {

    private static SoundManager instance;

    private SoundPool soundPool;
    private int clickSoundId, successSoundId, errorSoundId, moneySoundId;
    private SharedPreferences prefs;


    private SoundManager(Context context) {
        prefs = context.getSharedPreferences(ConfiguracionActivity.PREFS_NAME, Context.MODE_PRIVATE);
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(attrs)
                .build();

        // Cargar sonidos
        clickSoundId = soundPool.load(context, R.raw.click, 1);
        successSoundId = soundPool.load(context, R.raw.success, 1);
        errorSoundId = soundPool.load(context, R.raw.error, 1);
        moneySoundId = soundPool.load(context, R.raw.money, 1);
    }

    public static SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context.getApplicationContext());
        }
        return instance;
    }

    public void playClick() {
        if (prefs.getBoolean(ConfiguracionActivity.KEY_SOUND, true)) {
            soundPool.play(clickSoundId, 1f, 1f, 1, 0, 1f);
        }
    }

    public void playSuccess() {
        if (prefs.getBoolean(ConfiguracionActivity.KEY_SOUND, true)) {
            soundPool.play(successSoundId, 1f, 1f, 1, 0, 1f);
        }
    }

    public void playError() {
        if (prefs.getBoolean(ConfiguracionActivity.KEY_SOUND, true)) {
            soundPool.play(errorSoundId, 1f, 1f, 1, 0, 1f);
        }
    }

    public void playMoney() {
        if (prefs.getBoolean(ConfiguracionActivity.KEY_SOUND, true)) {
            soundPool.play(moneySoundId, 1f, 1f, 1, 0, 1f);
        }
    }
}