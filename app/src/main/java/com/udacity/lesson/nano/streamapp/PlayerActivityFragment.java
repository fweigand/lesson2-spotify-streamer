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
import android.widget.ImageView;
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

    private ViewHolder holder;

    private volatile boolean isScrubbing; // true while the user has grabbed the seekbar

    private int trackIndex;

    private PlayerService service;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Intent intent = getActivity().getIntent();

        final ArrayList<SpotifyItem.Track> trackList = intent.getParcelableArrayListExtra(SpotifyItemKeys.TOP_TRACKS);
        trackIndex = intent.getIntExtra(SpotifyItemKeys.TRACK_NUMBER, 0);

        View rootView = inflater.inflate(R.layout.fragment_player, container, false);
        holder = new ViewHolder(rootView);

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
        holder.artistNameTextView.setText(artistName);

        final int trackListSize = trackList.size();
        holder.nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackIndex = ++trackIndex % trackListSize;
                service.play(trackList.get(trackIndex));
            }
        });

        holder.previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackIndex = trackIndex == 0 ? trackListSize - 1 : --trackIndex % trackListSize;
                service.play(trackList.get(trackIndex));
            }
        });

        holder.playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                service.togglePlay();
            }
        });

        holder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                    service.seekTo(lastProgress);
                    Log.w("touch complete", "pos=" + lastProgress);
                }
                isScrubbing = false;
            }
        });
        return rootView;
    }

    private String millisToFormattedString(int aMillis) {
        return String.format("%02d:%02d", MILLISECONDS.toMinutes(aMillis),
                MILLISECONDS.toSeconds(aMillis) - MINUTES.toSeconds(MILLISECONDS.toMinutes(aMillis)));
    }

    @Override
    public void onStarted(SpotifyItem.Track track, int aDurationMs) {
        holder.trackNameTextView.setText(track.name);
        holder.albumNameTextView.setText(track.albumName);
        holder.seekBar.setMax(aDurationMs);
//        seekBar.setMax(track.durationMs);
        holder.trackLengthTextView.setText(millisToFormattedString(track.durationMs));
        holder.playButton.setImageResource(android.R.drawable.ic_media_pause);
        ImageLoaderUtils.showLargeImageView(holder.albumArtwork, track.largeImageUrl);
    }

    @Override
    public void onPaused() {
        holder.playButton.setImageResource(android.R.drawable.ic_media_play);
    }

    @Override
    public void onResumed() {
        holder.playButton.setImageResource(android.R.drawable.ic_media_pause);
    }

    @Override
    public void onProgress(int aProgress) {
        if (!isScrubbing) {
            holder.seekBar.setProgress(aProgress);
        }
        holder.trackPosition.setText(millisToFormattedString(aProgress));
    }

    @Override
    public void onFinished() {

    }

    private static class ViewHolder {
        final TextView trackNameTextView;
        final TextView albumNameTextView;
        final SeekBar seekBar;
        final TextView trackLengthTextView;
        final ImageButton playButton;
        final ImageButton nextButton;
        final ImageButton previousButton;
        final TextView artistNameTextView;
        final TextView trackPosition;
        final ImageView albumArtwork;

        public ViewHolder(View aView) {
//            artistNameTextView = find(aView, R.id.player_artist_name, TextView.class);
            artistNameTextView = (TextView) aView.findViewById( R.id.player_artist_name);
            trackNameTextView = (TextView) aView.findViewById(R.id.player_track_name);
            albumNameTextView = (TextView) aView.findViewById(R.id.player_album_name);
            seekBar = (SeekBar) aView.findViewById(R.id.player_seek_bar);
            trackLengthTextView = (TextView) aView.findViewById(R.id.player_track_length);
            playButton = (ImageButton) aView.findViewById(R.id.player_play);
            nextButton = (ImageButton) aView.findViewById(R.id.player_next);
            previousButton = (ImageButton) aView.findViewById(R.id.player_previous);
            trackPosition = (TextView) aView.findViewById(R.id.player_track_position);
            albumArtwork = (ImageView)aView.findViewById(R.id.player_album_artwork);
        }
//        private <T> T find(View aView, int id, Class<T> aClass) {return aClass.cast(aView.findViewById(id)); }
    }
}