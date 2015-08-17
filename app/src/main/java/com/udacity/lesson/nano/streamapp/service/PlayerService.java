package com.udacity.lesson.nano.streamapp.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.udacity.lesson.nano.streamapp.R;
import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItem;

import java.io.IOException;

public class PlayerService extends Service {

    private final static String TAG = PlayerService.class.getSimpleName();

    private final PlayerServiceBinder binder = new PlayerServiceBinder();
    private MediaPlayer mediaPlayer;

    private PlayerServiceListener listener;
    private SpotifyItem.Track currentTrack;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
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

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        // mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        MediaPlayerListener listener = new MediaPlayerListener();
        mediaPlayer.setOnCompletionListener(listener);
        mediaPlayer.setOnPreparedListener(listener);
        mediaPlayer.setOnErrorListener(listener);
    }

    @Override
    public void onDestroy() {
        stop();
        mediaPlayer.release();
    }

    // only support a single listener ATM
    public void setListener(PlayerServiceListener aListener) {
        listener = aListener;
    }

    private void notifyStarted(int aMaxProgress) {
        if (listener != null) {
            listener.onStarted(currentTrack, aMaxProgress);
        }
    }

    private void notifyPaused() {
        if (listener != null) {
            listener.onPaused();
        }
    }

    private void notifyResumed() {
        if (listener != null) {
            listener.onResumed();
        }
    }

    private void notifyProgress(int aProgress) {
        if (listener != null) {
            listener.onProgress(aProgress);
        }
    }

    private void notifyFinished() {
        if (listener != null) {
            listener.onFinished();
        }
    }

    public void stop() {
        if (mediaPlayer.isPlaying()) {
            Log.d(TAG, "stopping playback");
            mediaPlayer.stop();
        }
        notifyFinished();
    }

    public void play(SpotifyItem.Track aTrack) {
        Log.d(TAG, "starting playback");
        if (aTrack != currentTrack) {
            currentTrack = aTrack;
            stop();
            String url = currentTrack.trackUrl;
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(url);
                Log.d(TAG, "prepareAsync()");
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                Log.e(TAG, "media player setup problem: " + e.getMessage());
                Toast.makeText(getApplicationContext(),
                        R.string.media_player_problem, Toast.LENGTH_SHORT).show();
            }
        } else if (mediaPlayer.isPlaying()) { // already playing this currentTrack
            notifyStarted(mediaPlayer.getDuration());
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

    public class PlayerServiceBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    private class MediaPlayerListener implements MediaPlayer.OnPreparedListener,
            MediaPlayer.OnCompletionListener,
            MediaPlayer.OnSeekCompleteListener,
            MediaPlayer.OnErrorListener {
        @Override
        public void onPrepared(MediaPlayer mp) {
            Log.d(TAG, "onPrepared()");
            mp.start();
            notifyStarted(mp.getDuration());
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            Log.d(TAG, "onCompletion()");
            notifyFinished();
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.d(TAG, "onError()");
            notifyFinished();
            Toast.makeText(getApplicationContext(),
                    R.string.media_player_problem, Toast.LENGTH_SHORT).show();
            return true;
        }

        @Override
        public void onSeekComplete(MediaPlayer mp) {
            Log.d(TAG, "onSeekComplete()");
            mp.start();
            notifyStarted(mp.getDuration());
        }
    }
}