package com.dam.quizmillionapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class MusicService extends Service {
    private MediaPlayer mediaPlayer;
    private static MusicService instance;
    private boolean isMuted = false;
    private boolean isPausedByBackground = false;
    private int currentMusicResId = R.raw.backgroundmusic;


    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pauseRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                isPausedByBackground = true;
            }
        }
    };

    public static MusicService getInstance() {

        return instance;
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        crearMediaPlayer(R.raw.backgroundmusic);
    }

    private void crearMediaPlayer(int musicResId) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        currentMusicResId = musicResId;
        mediaPlayer = MediaPlayer.create(this, musicResId);

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        mediaPlayer.setAudioAttributes(attrs);
        mediaPlayer.setLooping(true);
        applyMute();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }

        applyMute();
        return START_STICKY;
    }

    public void playMusic(int musicResId) {
        if (currentMusicResId == musicResId && mediaPlayer != null) {
            if (!mediaPlayer.isPlaying() && !isMuted) {
                mediaPlayer.start();
            }
            return;
        }

        crearMediaPlayer(musicResId);

        if (!isMuted) {
            mediaPlayer.start();
        }
    }

    public void setMuted(boolean muted) {
        isMuted = muted;
        applyMute();

        if (isMuted && mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else if (!isMuted && mediaPlayer != null && !mediaPlayer.isPlaying() && !isPausedByBackground) {
            mediaPlayer.start();
        }
    }

    private void applyMute() {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(isMuted ? 0f : 1f, isMuted ? 0f : 1f);
        }
    }


    public void pauseMusicForBackground() {
        handler.postDelayed(pauseRunnable, 300);
    }


    public void resumeMusicFromBackground() {
        handler.removeCallbacks(pauseRunnable);

        if (mediaPlayer != null && isPausedByBackground && !isMuted) {
            mediaPlayer.start();
            isPausedByBackground = false;
        }
    }




    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        instance = null;
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

}
