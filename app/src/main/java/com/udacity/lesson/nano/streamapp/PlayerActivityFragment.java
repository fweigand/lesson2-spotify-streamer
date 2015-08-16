package com.udacity.lesson.nano.streamapp;

import android.app.DialogFragment;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
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
import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

public class PlayerActivityFragment extends DialogFragment {

    private MediaPlayer mediaPlayer;
    private Timer progressUpdater;
    private View rootView;

    private volatile boolean isScrubbing; // true while the user has grabbed the seekbar
    private int trackIndex;

    public PlayerActivityFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Intent intent = getActivity().getIntent();

        intent.getParcelableArrayListExtra(SpotifyItemKeys.TOP_TRACKS);

        final ArrayList<SpotifyItem.Track> trackList = intent.getParcelableArrayListExtra(SpotifyItemKeys.TOP_TRACKS);
        trackIndex = intent.getIntExtra(SpotifyItemKeys.TRACK_NUMBER, 0);
        rootView = inflater.inflate(R.layout.fragment_player, container, false);
        play(trackList.get( trackIndex ));

        String artistName = intent.getStringExtra(SpotifyItemKeys.ARTIST_NAME);
        TextView artistNameTextView = (TextView) rootView.findViewById(R.id.player_artist_name);
        artistNameTextView.setText(artistName);

        final int trackListSize = trackList.size();
        ImageButton nextButton = (ImageButton) rootView.findViewById(R.id.player_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackIndex = ++trackIndex % trackListSize;
                play(trackList.get( trackIndex ));
            }
        });

        ImageButton previousButton = (ImageButton) rootView.findViewById(R.id.player_previous);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackIndex = trackIndex == 0 ? trackListSize - 1 : --trackIndex % trackListSize;
                play(trackList.get( trackIndex ));
            }
        });

        final ImageButton playButton = (ImageButton) rootView.findViewById(R.id.player_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying()) {
                    playButton.setImageResource(android.R.drawable.ic_media_play);
                    mediaPlayer.pause();
                } else {
                    playButton.setImageResource(android.R.drawable.ic_media_pause);
                    mediaPlayer.start();
                }
            }
        });

        return rootView;
    }

    private void stop() {
        if( progressUpdater != null ) {
            progressUpdater.cancel();
            progressUpdater.purge();
            progressUpdater = null;
        }

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
        }
    }

    private void play(SpotifyItem.Track track) {
        stop();
        String url = track.trackUrl;

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(final MediaPlayer mp) {
                    mp.start();
                    ImageButton playButton = (ImageButton) rootView.findViewById(R.id.player_play);
                    playButton.setImageResource(android.R.drawable.ic_media_pause);

                    SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.player_seek_bar);
                    seekBar.setMax(mp.getDuration());

                    progressUpdater = new Timer();
                    progressUpdater.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            rootView.post(new Runnable() {
                                @Override
                                public void run() {
                                    int positionMs = mediaPlayer.getCurrentPosition();
                                    if (!isScrubbing) {
                                        SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.player_seek_bar);
                                        seekBar.setProgress(positionMs);
                                    }
                                    TextView trackPosition = (TextView) rootView.findViewById(R.id.player_track_position);
                                    trackPosition.setText(millisToFormattedString(positionMs));
                                }
                            });
                        }
                    }, 70, 70);
                }
            });

            mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    final ImageButton playButton = (ImageButton) rootView.findViewById(R.id.player_play);
                    playButton.setImageResource(android.R.drawable.ic_media_pause);
                    mp.start();
                    Log.w("seek complete", "pos=" + mp.getCurrentPosition() + " length=" +  mp.getDuration() );

                }
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e("mediaplayer", "failed to setup the media player: " + e.getMessage());
            mediaPlayer.release();
            mediaPlayer = null;
        }

        TextView trackNameTextView = (TextView) rootView.findViewById(R.id.player_track_name);
        trackNameTextView.setText(track.name);

        TextView albumNameTextView = (TextView) rootView.findViewById(R.id.player_album_name);
        albumNameTextView.setText(track.albumName);

        final SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.player_seek_bar);
        seekBar.setMax(track.durationMs);

        TextView trackLengthTextView = (TextView) rootView.findViewById(R.id.player_track_length);
        trackLengthTextView.setText(millisToFormattedString(track.durationMs));

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
                    mediaPlayer.seekTo(lastProgress - 1);
                    Log.w("touch complete", "pos=" +lastProgress);
                }
                isScrubbing = false;
            }
        });

        ImageLoaderUtils.showLargeImageView(rootView, R.id.player_album_artwork, track.largeImageUrl);
    }

    private String millisToFormattedString( int aMillis ) {
        return String.format("%02d:%02d", MILLISECONDS.toMinutes(aMillis),
                MILLISECONDS.toSeconds(aMillis) - MINUTES.toSeconds(MILLISECONDS.toMinutes(aMillis)));
    }
}