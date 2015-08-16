package com.udacity.lesson.nano.streamapp;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItem;

import java.io.IOException;
import java.util.List;

import static com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys.ARTIST_NAME;
import static com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys.TOP_TRACKS;
import static com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys.TRACK_NUMBER;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

public class PlayerActivityFragment extends Fragment {

    private volatile boolean isScrubbing;

    private View rootView;

    private int trackIndex; // for previous and next buttons

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_player, container, false);

        Intent intent = getActivity().getIntent();
        final List<SpotifyItem.Track> tracks = intent.getParcelableArrayListExtra(TOP_TRACKS);
        trackIndex = intent.getIntExtra(TRACK_NUMBER, 0);

        String artistName = intent.getStringExtra(ARTIST_NAME);
        TextView artistNameTextView = (TextView) rootView.findViewById(R.id.player_artist_name);
        artistNameTextView.setText(artistName);

        final PlayerController player = new PlayerController();

        final int trackListSize = tracks.size();
        rootView.findViewById(R.id.player_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackIndex = ++trackIndex % trackListSize;
                SpotifyItem.Track track = tracks.get(trackIndex);
                setupUi(track, 0);
                player.start(track.trackUrl, 0, track.durationMs);

            }
        });
        rootView.findViewById(R.id.player_previous).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackIndex = trackIndex == 0 ? trackListSize - 1 : --trackIndex % trackListSize;
                SpotifyItem.Track track = tracks.get(trackIndex);
                setupUi(track, 0);
                player.start(track.trackUrl, 0, track.durationMs);
            }
        });

        ImageButton playButton = (ImageButton) rootView.findViewById(R.id.player_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.togglePlayback();
            }
        });

        final SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.player_seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int lastProgress = -1;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    lastProgress = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                lastProgress = -1;
                isScrubbing = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (lastProgress != -1) {
                    if (player.isFinished()) {
                        SpotifyItem.Track track = tracks.get(trackIndex);
                        setupUi(track, lastProgress);
                        player.start(track.trackUrl, lastProgress, track.durationMs);
                    } else {
                        player.seekTo(lastProgress);
                    }
                    Log.w("touch complete", "pos=" + lastProgress);
                }
                isScrubbing = false;
            }
        });

        SpotifyItem.Track track = tracks.get(trackIndex);
        setupUi(track, 0);
        player.start(track.trackUrl, 0, track.durationMs);
        return rootView;
    }

    private void updateUi(int position) {
        // boolean isInTouchMode = seekBar.isInTouchMode();
        if (!isScrubbing) {
            final SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.player_seek_bar);
            seekBar.setProgress(position);
        }
        TextView trackPosition = (TextView) rootView.findViewById(R.id.player_track_position);
        trackPosition.setText(millisToFormattedString(position));
    }

    private void updatePlay() {
        ImageButton playButton = (ImageButton) rootView.findViewById(R.id.player_play);
        playButton.setImageResource(android.R.drawable.ic_media_pause);
    }

    private void updatePause() {
        ImageButton playButton = (ImageButton) rootView.findViewById(R.id.player_play);
        playButton.setImageResource(android.R.drawable.ic_media_play);
    }

    private void setupUi(SpotifyItem.Track track, int position) {
        SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.player_seek_bar);
        seekBar.setMax(track.durationMs);
        updateUi(position);

        ImageButton playButton = (ImageButton) rootView.findViewById(R.id.player_play);
        playButton.setImageResource(android.R.drawable.ic_media_pause);

        TextView trackNameTextView = (TextView) rootView.findViewById(R.id.player_track_name);
        trackNameTextView.setText(track.name);

        TextView albumNameTextView = (TextView) rootView.findViewById(R.id.player_album_name);
        albumNameTextView.setText(track.albumName);

        TextView trackLengthTextView = (TextView) rootView.findViewById(R.id.player_track_length);
        trackLengthTextView.setText(millisToFormattedString(track.durationMs));

        ImageLoaderUtils.showLargeImageView(rootView, R.id.player_album_artwork, track.largeImageUrl);
    }

    private static String millisToFormattedString(int aMillis) {
        return String.format("%02d:%02d", MILLISECONDS.toMinutes(aMillis),
                MILLISECONDS.toSeconds(aMillis) - MINUTES.toSeconds(MILLISECONDS.toMinutes(aMillis)));
    }

    private class PlayerController {
        private PlayerTask task;

        void start(String aUrl, int position, int duration) {
            stop();
            task = new PlayerTask(position);
            task.execute(aUrl, String.valueOf(duration));
        }

        void stop() {
            if (task != null) {
                task.stop();
                task = null;
            }
        }

        void seekTo(int aPositionMs) {
            if (task != null) {
                task.seekTo(aPositionMs);
            }
        }

        void togglePlayback() {
            if (task != null) {
                task.togglePlayback();
            }
        }

        public boolean isFinished() {
            return task == null || task.isCancelled();
        }
    }

    private class PlayerTask extends AsyncTask<String, Integer, Boolean> {

        private final String LOG_TAG = PlayerTask.class.getSimpleName();

        private volatile MediaPlayer mediaPlayer;
        private final int initialPosition;

        public PlayerTask(int aInitialPosition) {
            initialPosition = aInitialPosition;
            Log.i(LOG_TAG, "created with position=" + initialPosition);
        }

        void stop() {
            Log.v(LOG_TAG, "stopped");

            if (!isCancelled() && mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    Log.d(LOG_TAG, "stopping playback");
                    mediaPlayer.stop();
                }

                Log.d(LOG_TAG, "releasing mediaplayer");
                mediaPlayer.release();
                mediaPlayer = null;
                cancel(true);
            } else {
                Log.d(LOG_TAG, "stop() already stopped");
            }
        }

        void seekTo(int aPositionMs) {
            Log.v(LOG_TAG, "seeking position=" + aPositionMs);

            if (!isCancelled() && mediaPlayer != null) {
                doProgressPublish = false;
                Log.d(LOG_TAG, "seeking mediaplayer -> doProgress=" + doProgressPublish);

                // i use pause before seek to workaround various problems described at:
                // http://stackoverflow.com/questions/3212688/mediaplayer-seekto-does-not-work-for-unbuffered-position
                // https://code.google.com/p/android/issues/detail?id=9135
                mediaPlayer.pause();
                mediaPlayer.seekTo(aPositionMs);
            } else {
                Log.d(LOG_TAG, "seek() already stopped");
            }
        }

        void togglePlayback() {
            Log.v(LOG_TAG, "toggle playback");

            if (isCancelled() || mediaPlayer == null) {
                Log.d(LOG_TAG, "playback() already stopped");
            }
            if (mediaPlayer.isPlaying()) {
                Log.d(LOG_TAG, "going to pause");
                mediaPlayer.pause();
                updatePause();
            } else {
                Log.d(LOG_TAG, "going to resume");
                mediaPlayer.start();
                updatePlay();
            }
        }


        @Override
        protected void onProgressUpdate(Integer... values) {
            int positionMs = values[0];
            Log.d(LOG_TAG, "onProgressUpdate() position=" + positionMs);
            updateUi(positionMs);
        }

        @Override
        protected void onPreExecute() {
            Log.d(LOG_TAG, "onPreExecute() enter");
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            Log.d(LOG_TAG, "onPreExecute() finish");
        }

        volatile boolean isFinished;
        volatile boolean doProgressPublish = true;


        @Override
        protected Boolean doInBackground(String... params) {
            Log.d(LOG_TAG, "doInBackground() enter");

            final String url = params[0];
            final int maxPosition = Integer.valueOf(params[1]);
            try {
                mediaPlayer.setDataSource(url);

                mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        doProgressPublish = true;
                        isFinished = false;
                        Log.d(LOG_TAG, "seek complete doProgress=" + doProgressPublish);
                        mediaPlayer.start();
                        updatePlay();
                    }
                });
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Log.d(LOG_TAG, "playback completed");
                        // isFinished = true;
                        // stop();
                    }
                });

                mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        int duration = Toast.LENGTH_SHORT;
                        Toast toast = Toast.makeText(getActivity().getApplicationContext(),
                                "Problem with mediaplayer " + "(code= + " + what + " error=" + extra, duration);
                        toast.show();
                        return false;
                    }
                });

                Log.d(LOG_TAG, "preparing");
                mediaPlayer.prepare();
                Log.d(LOG_TAG, "preparing done");

                if (isCancelled()) {
                    Log.d(LOG_TAG, "cancelled after prepare");
                    Log.d(LOG_TAG, "doInBackground() finish");
                    return false;
                }

                if (initialPosition > 0) {
                    Log.d(LOG_TAG, "seeking after prepare");
                    seekTo(initialPosition);
                } else {
                    Log.d(LOG_TAG, "starting after prepare");
                    mediaPlayer.start();
                }
            } catch (IOException e) {

                // Toast.makeText(getApplicationContext(), "Download complete, playing Music", Toast.LENGTH_LONG).show();
                Log.e("mediaplayer", "failed to setup the media player: " + e.getMessage());
                mediaPlayer.release();
                mediaPlayer = null;
                Log.d(LOG_TAG, "doInBackground() finish");
                return false;
            }

            int currentPosition = 0;

            do {
                try {
                    Thread.sleep(70);
                } catch (InterruptedException e) {
                    isFinished = true;
                }
                if (!isFinished) {
                    try {
                        final int positionMs = mediaPlayer.getCurrentPosition();
                        Log.d(LOG_TAG, "run() doProgress=" + doProgressPublish + " pos=" + positionMs + " current=" + currentPosition);

                        if (positionMs / 1000 != currentPosition) {

                            if (doProgressPublish) {
                                publishProgress(positionMs);
                                currentPosition = positionMs / 1000;
                            }
                        }
                    } catch (IllegalStateException e) {
                        publishProgress(maxPosition);
                        isFinished = true;
                    }
                } else {
                    publishProgress(maxPosition);
                }

            } while (!isFinished);

            Log.d(LOG_TAG, "doInBackground() finish");
            return isFinished;
        }
    }
}



