package com.dam.quizmillionapp;



import android.app.Service;

import android.content.Context;

import android.content.Intent;

import android.media.MediaPlayer;

import android.os.IBinder;

public class MusicService extends Service {

    private MediaPlayer mediaPlayer;

    @Override

    public IBinder onBind(Intent intent) { return null; }

    public void onCreate() {
        super.onCreate();
        mediaPlayer = MediaPlayer.create(this, R.raw.backgroundmusic);
        mediaPlayer.setLooping(true);
        mediaPlayer.setVolume(1.0f, 1.0f);
        mediaPlayer.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
        return START_STICKY;
    }

    @Override

    public void onDestroy() {

        mediaPlayer.stop();

        mediaPlayer.release();

    }

}

