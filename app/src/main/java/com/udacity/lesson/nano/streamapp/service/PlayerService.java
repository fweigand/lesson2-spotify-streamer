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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service around the MediaPlayer class.
 * <p/>
 * Started as a Service. Implementation is capable of sending events out to a single
 * PlayerServiceListener, notifying about pausing, resuming, playing, finishing and progressing through
 * a song.
 * <p/>
 * Provides a simple public API for controlling the player from outside.
 */
public class PlayerService extends Service {

    private final static String TAG = PlayerService.class.getSimpleName();

    private final PlayerServiceBinder binder = new PlayerServiceBinder();
    private MediaPlayer mediaPlayer;

    private PlayerServiceListener listener; // currently only one listener is supported
    private SpotifyItem.Track currentTrack; // and only one song at a time is kept track of

    // this could be augmented into a full state machine manager
    // MediaPlayer only has a isPlaying() method but no "isSeeking()" or sth. similar
    private volatile boolean isFinished;
    private volatile boolean isPreparing;

    private volatile int position;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // why does MediaPlayer lack a setOnProgressListener(Listener l, int resolutionMs) method?
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Runnable progressTracker = new Runnable() {
        @Override
        public void run() {
            boolean isFinished = false;
            int currentPosition = 0;
            do {
                try {
                    // the sleep time could be implemented to adjust dynamically, so that we hit
                    // almost at 1s intervals. right now we do a little too much checking
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    isFinished = true;
                }
                if (Thread.currentThread().isInterrupted()) {
                    isFinished = true;
                }
                if (!isFinished) {
                    if (mediaPlayer.isPlaying()) {
                        final int positionMs = mediaPlayer.getCurrentPosition();
                        if (positionMs / 1000 != currentPosition) {
                            notifyProgress(positionMs);
                            currentPosition = positionMs / 1000;
                        }
                    }
                }
                if (Thread.currentThread().isInterrupted()) {
                    isFinished = true;
                }
            } while (!isFinished);
        }
    };

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate()");
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        MediaPlayerListener listener = new MediaPlayerListener();
        mediaPlayer.setOnCompletionListener(listener);
        mediaPlayer.setOnPreparedListener(listener);
        mediaPlayer.setOnErrorListener(listener);
        mediaPlayer.setOnSeekCompleteListener(listener);
        executor.execute(progressTracker);
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        stop();
        mediaPlayer.release();
    }

    // only supports a single listener ATM
    public void setListener(PlayerServiceListener aListener) {
        listener = aListener;
    }

    public void stop() {
        if (mediaPlayer.isPlaying()) {
            Log.v(TAG, "stopping playback");
            mediaPlayer.stop();
        }
    }

    public void play(SpotifyItem.Track aTrack) {
        Log.v(TAG, "starting playback");
        if (aTrack != currentTrack) {
            currentTrack = aTrack;
            stop();
            String url = currentTrack.trackUrl;
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(url);
                Log.d(TAG, "prepareAsync()");
                mediaPlayer.prepareAsync();
                isPreparing = true;
            } catch (IOException e) {
                Log.e(TAG, "media player setup problem: " + e.getMessage());
                Toast.makeText(getApplicationContext(),
                        R.string.media_player_problem, Toast.LENGTH_SHORT).show();
            }
        } else { // already bound to this track - either playing or paused
            notifyStarted(mediaPlayer.getDuration());
            notifyProgress(mediaPlayer.getCurrentPosition());
            if (mediaPlayer.isPlaying()) {
                notifyResumed();
            } else {
                notifyPaused();
            }
        }
    }

    /**
     * Pause or unpause depending on the current state
     */
    public void togglePlay() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            notifyPaused();
        } else {
            mediaPlayer.start();
            notifyResumed();
        }
    }

    /**
     * Jumps to a specific position in the track
     * @param aProgressMs
     */
    public void jumpTo(int aProgressMs) {
        Log.v(TAG, "jumpTo() position=" + aProgressMs);

        // if the user drags the seekbar after playback has finished we need to save the
        // position, from where to restart
        if (isFinished) {
            SpotifyItem.Track track = currentTrack;
            currentTrack = null;
            position = aProgressMs;
            play(track);
        } else if (isPreparing) {
            position = aProgressMs;
        } else {
            mediaPlayer.seekTo(aProgressMs);
        }
    }

    // Binder for granting access to the PlayerService to an outside object
    public class PlayerServiceBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    private class MediaPlayerListener implements MediaPlayer.OnPreparedListener,
            MediaPlayer.OnCompletionListener,
            MediaPlayer.OnSeekCompleteListener,
            MediaPlayer.OnErrorListener {

        @Override public void onPrepared(MediaPlayer mp) {
            Log.v(TAG, "onPrepared()");
            isFinished = false;
            isPreparing = false;

            // if this was done as part of a restart after the playback had already finished
            // we do not start right away, but seek to the target position and resume
            // playing only after the seek operation has completed also
            if (position > 0) {
                mediaPlayer.seekTo(position);
                position = 0;
            } else {
                mp.start();
                notifyStarted(mp.getDuration());
            }
        }
        @Override public void onCompletion(MediaPlayer mp) {
            Log.v(TAG, "onCompletion()");
            isFinished = true;
            notifyFinished();
        }
        @Override public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.v(TAG, "onError()");
            notifyFinished();
            Toast.makeText(getApplicationContext(),
                    R.string.media_player_problem, Toast.LENGTH_SHORT).show();
            return true;
        }
        @Override public void onSeekComplete(MediaPlayer mp) {
            Log.v(TAG, "onSeekComplete()");
            mp.start();
            notifyStarted(mp.getDuration());
        }
    }

    // the simple notification methods:
    private void notifyPaused()   { if (listener != null) { listener.onPaused();   } }
    private void notifyResumed()  { if (listener != null) { listener.onResumed();  } }
    private void notifyFinished() { if (listener != null) { listener.onFinished(); } }
    private void notifyProgress(int aProgress)   { if (listener != null) { listener.onProgress(aProgress); } }
    private void notifyStarted(int aMaxProgress) { if (listener != null) { listener.onStarted(currentTrack, aMaxProgress); } }
}