package com.dam.quizmillionapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

public class MusicService extends Service {
    private MediaPlayer mediaPlayer;
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = MediaPlayer.create(this, R.raw.backgroundmusic);
        mediaPlayer.setLooping(true);
        mediaPlayer.setVolume(1.0f, 1.0f);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();

            if (action.equals(ACTION_PLAY)) {
                if (!mediaPlayer.isPlaying()) mediaPlayer.start();
            } else if (action.equals(ACTION_PAUSE)) {
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
            }
        } else {
            // Por defecto, si se inicia sin acción, suena
            if (!mediaPlayer.isPlaying()) mediaPlayer.start();
        }
        return START_STICKY;
    }



    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        super.onDestroy();
    }
}

