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

    private volatile Player player;

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

        final int trackListSize = tracks.size();
        rootView.findViewById(R.id.player_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
                trackIndex = ++trackIndex % trackListSize;
                start(tracks.get(trackIndex), 0);
            }
        });
        rootView.findViewById(R.id.player_previous).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
                trackIndex = trackIndex == 0 ? trackListSize - 1 : --trackIndex % trackListSize;
                start(tracks.get(trackIndex), 0);
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
                    if (player.isFinished) {
                        start(tracks.get(trackIndex), lastProgress);
                    } else {
                        player.seekTo(lastProgress);
                    }
                    Log.w("touch complete", "pos=" + lastProgress);
                }
                isScrubbing = false;
            }
        });

        start(tracks.get(trackIndex), 0);
        return rootView;
    }

    private void updateUi(int aCurrentPositionMs) {
        // boolean isInTouchMode = seekBar.isInTouchMode();
        if (!isScrubbing) {
            final SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.player_seek_bar);
            seekBar.setProgress(aCurrentPositionMs);
        }
        TextView trackPosition = (TextView) rootView.findViewById(R.id.player_track_position);
        trackPosition.setText(millisToFormattedString(aCurrentPositionMs));
    }

    private void updatePlay() {
        ImageButton playButton = (ImageButton) rootView.findViewById(R.id.player_play);
        playButton.setImageResource(android.R.drawable.ic_media_pause);
    }

    private void updatePause() {
        ImageButton playButton = (ImageButton) rootView.findViewById(R.id.player_play);
        playButton.setImageResource(android.R.drawable.ic_media_play);
    }

    private void stop() {
        if (player != null) {
            player.stop();
            player = null;
        }
    }

    private void start(SpotifyItem.Track track, int position) {
        SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.player_seek_bar);
        seekBar.setMax(track.durationMs);
        updateUi(position);

        ImageButton playButton = (ImageButton) rootView.findViewById(R.id.player_play);
        playButton.setImageResource(android.R.drawable.ic_media_pause);

        player = new Player( position );
        player.execute(track.trackUrl, String.valueOf(track.durationMs));

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

    private class Player extends AsyncTask<String, Integer, Boolean> {
        private volatile MediaPlayer mediaPlayer;
        private volatile boolean isFinished;
        private final int initialPosition;

        public Player( int aInitialPosition ) {
            initialPosition = aInitialPosition;
        }

        void stop() {
            if (!isFinished && mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }
            isFinished = true;
            cancel(true);
        }

        void seekTo(int aPositionMs) {
            if (!isFinished && mediaPlayer != null) {
                mediaPlayer.seekTo(aPositionMs);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            int positionMs = values[0];
            updateUi(positionMs);
        }

        @Override
        protected void onPreExecute() {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            final String url = params[0];
            final int maxPosition = Integer.valueOf(params[1]);
            try {
                mediaPlayer.setDataSource(url);
                mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        mediaPlayer.start();
                        updatePlay();
                    }
                });
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        stop();
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

                mediaPlayer.prepare();

                if( initialPosition > 0 ) {
                    seekTo( initialPosition );
                } else {
                    mediaPlayer.start();
                }
            } catch (IOException e) {

                // Toast.makeText(getApplicationContext(), "Download complete, playing Music", Toast.LENGTH_LONG).show();
                Log.e("mediaplayer", "failed to setup the media player: " + e.getMessage());
                mediaPlayer.release();
                mediaPlayer = null;
                return false;
            }

            int currentPosition = 0;
            do {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
                if( !isFinished ) {
                    try {
                        final int positionMs = mediaPlayer.getCurrentPosition();
                        if (positionMs / 1000 != currentPosition) {
                            publishProgress(positionMs);
                            currentPosition = positionMs / 1000;
                        }
                    } catch( IllegalStateException e ) {
                        publishProgress(maxPosition);
                    }
                } else {
                    publishProgress(maxPosition);
                }

            } while (!isFinished);

            return isFinished;
        }

        public void togglePlayback() {
            if (isFinished || mediaPlayer == null) {
                return;
            }
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                updatePause();
            } else {
                mediaPlayer.start();
                updatePlay();
            }
        }
    }
}



