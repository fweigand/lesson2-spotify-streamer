package com.udacity.lesson.nano.streamapp;

import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
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

import com.udacity.lesson.nano.streamapp.service.PlayerService;
import com.udacity.lesson.nano.streamapp.service.PlayerServiceListener;
import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItem;
import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

public class PlayerActivityFragment extends DialogFragment implements PlayerServiceListener {

    private View rootView;

    private volatile boolean isScrubbing; // true while the user has grabbed the seekbar

    private int trackIndex;

    private PlayerService service;

    public PlayerActivityFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Intent intent = getActivity().getIntent();

        intent.getParcelableArrayListExtra(SpotifyItemKeys.TOP_TRACKS);

        final ArrayList<SpotifyItem.Track> trackList = intent.getParcelableArrayListExtra(SpotifyItemKeys.TOP_TRACKS);
        trackIndex = intent.getIntExtra(SpotifyItemKeys.TRACK_NUMBER, 0);
        rootView = inflater.inflate(R.layout.fragment_player, container, false);

        Intent serviceIntent = new Intent(getActivity(), PlayerService.class);
         getActivity().bindService(serviceIntent, new ServiceConnection() {
             @Override
             public void onServiceConnected(ComponentName name, IBinder binder) {
                 PlayerService.PlayerServiceBinder pbinder = (PlayerService.PlayerServiceBinder) binder;
                 service = pbinder.getService();
                 service.setListener(PlayerActivityFragment.this);
                 service.play(trackList.get(trackIndex));
             }

             @Override
             public void onServiceDisconnected(ComponentName name) {
             }
        }, Context.BIND_AUTO_CREATE);

        String artistName = intent.getStringExtra(SpotifyItemKeys.ARTIST_NAME);
        TextView artistNameTextView = (TextView) rootView.findViewById(R.id.player_artist_name);
        artistNameTextView.setText(artistName);

        final int trackListSize = trackList.size();
        ImageButton nextButton = (ImageButton) rootView.findViewById(R.id.player_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackIndex = ++trackIndex % trackListSize;
                service.play(trackList.get(trackIndex));
            }
        });

        ImageButton previousButton = (ImageButton) rootView.findViewById(R.id.player_previous);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackIndex = trackIndex == 0 ? trackListSize - 1 : --trackIndex % trackListSize;
                service.play(trackList.get(trackIndex));
            }
        });

        final ImageButton playButton = (ImageButton) rootView.findViewById(R.id.player_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                service.togglePlay();
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
                    service.seekTo( lastProgress );
                    Log.w("touch complete", "pos=" + lastProgress);
                }
                isScrubbing = false;
            }
        });
        return rootView;
    }

    private String millisToFormattedString( int aMillis ) {
        return String.format("%02d:%02d", MILLISECONDS.toMinutes(aMillis),
                MILLISECONDS.toSeconds(aMillis) - MINUTES.toSeconds(MILLISECONDS.toMinutes(aMillis)));
    }

    @Override
    public void onStarted(SpotifyItem.Track track, int aDurationMs) {
        TextView trackNameTextView = (TextView) rootView.findViewById(R.id.player_track_name);
        trackNameTextView.setText(track.name);

        TextView albumNameTextView = (TextView) rootView.findViewById(R.id.player_album_name);
        albumNameTextView.setText(track.albumName);

        final SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.player_seek_bar);
        seekBar.setMax(aDurationMs);
//        seekBar.setMax(track.durationMs);

        TextView trackLengthTextView = (TextView) rootView.findViewById(R.id.player_track_length);
        trackLengthTextView.setText(millisToFormattedString(track.durationMs));

        ImageButton playButton = (ImageButton) rootView.findViewById(R.id.player_play);
        playButton.setImageResource(android.R.drawable.ic_media_pause);

        ImageLoaderUtils.showLargeImageView(rootView, R.id.player_album_artwork, track.largeImageUrl);
    }

    @Override
    public void onPaused() {
        ImageButton playButton = (ImageButton) rootView.findViewById(R.id.player_play);
        playButton.setImageResource(android.R.drawable.ic_media_play);
    }

    @Override
    public void onResumed() {
        ImageButton playButton = (ImageButton) rootView.findViewById(R.id.player_play);
        playButton.setImageResource(android.R.drawable.ic_media_pause);
    }

    @Override
    public void onProgress(int aProgress) {
        if (!isScrubbing) {
            SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.player_seek_bar);
            seekBar.setProgress(aProgress);
        }
        TextView trackPosition = (TextView) rootView.findViewById(R.id.player_track_position);
        trackPosition.setText(millisToFormattedString(aProgress));
    }

    @Override
    public void onFinished() {

    }
}