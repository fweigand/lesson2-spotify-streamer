package com.udacity.lesson.nano.streamapp.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItem;

import java.io.IOException;

public class PlayerService extends Service {

    private MediaPlayer mediaPlayer;

    private final PlayerServiceBinder binder = new PlayerServiceBinder();

    public class PlayerServiceBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        // mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setOnCompletionListener(new ServiceOnCompletionListener());
        mediaPlayer.setOnPreparedListener(new ServiceOnPreparedListener());
        mediaPlayer.setOnErrorListener(new ServiceOnErrorListener());
        mediaPlayer.setOnSeekCompleteListener(new ServiceOnSeekCompleteListener());
    }

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//
//        boolean isFinished = false;
//        do {
//            try {
//                Thread.sleep(70);
//            } catch (InterruptedException e) {
//                isFinished = true;
//            }
//            if (!isFinished) {
//                try {
//                    final int positionMs = mediaPlayer.getCurrentPosition();
//                    if (positionMs / 1000 != currentPosition) {
//                        if (doProgressPublish) {
//                            publishProgress(positionMs);
//                            currentPosition = positionMs / 1000;
//                        }
//                    }
//                } catch (IllegalStateException e) {
//                    publishProgress(maxPosition);
//                    isFinished = true;
//                }
//            } else {
//                publishProgress(maxPosition);
//            }
//
//        } while (!isFinished);
//
//
//        return super.onStartCommand(intent, flags, startId);
//    }

    private class ServiceOnPreparedListener implements MediaPlayer.OnPreparedListener {

        @Override
        public void onPrepared(MediaPlayer mp) {
            mp.start();
            notifyStarted(mp.getDuration());
        }
    }

    private class ServiceOnCompletionListener implements MediaPlayer.OnCompletionListener {

        @Override
        public void onCompletion(MediaPlayer mp) {
            notifyFinished();
        }
    }

    private class ServiceOnErrorListener implements MediaPlayer.OnErrorListener {

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            notifyFinished();
            return false;
        }
    }

    private class ServiceOnSeekCompleteListener implements MediaPlayer.OnSeekCompleteListener {

        @Override
        public void onSeekComplete(MediaPlayer mp) {
            mp.start();
            notifyStarted(mp.getDuration());
        }
    }

    private PlayerServiceListener listener;

    public void setListener( PlayerServiceListener aListener ) {
        listener = aListener;
    }

    private void notifyStarted(int aMaxProgress) {
        if( listener != null ) {
            listener.onStarted(track, aMaxProgress);
        }
    }

    private void notifyPaused() {
        if( listener != null ) {
            listener.onPaused();
        }
    }

    private void notifyResumed() {
        if( listener != null ) {
            listener.onResumed();
        }
    }

    private void notifyProgress(int aProgress) {
        if( listener != null ) {
            listener.onProgress(aProgress);
        }
    }

    private void notifyFinished() {
        if( listener != null ) {
            listener.onFinished();
        }
    }

    public void stop() {
        if( mediaPlayer.isPlaying() ) {
            mediaPlayer.stop();
        }
        notifyFinished();
    }

    private SpotifyItem.Track track;

    public void play(SpotifyItem.Track aTrack) {
        track = aTrack;
        stop();
        String url = track.trackUrl;

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e("mediaplayer", "failed to setup the media player: " + e.getMessage());
        }
    }

    public void togglePlay() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            notifyPaused();
        } else {
            mediaPlayer.start();
            notifyResumed();
        }
    }

    public void seekTo(int aProgressMs) {
        mediaPlayer.seekTo(aProgressMs - 1);
    }
}
