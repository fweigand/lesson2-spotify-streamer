package com.udacity.lesson.nano.streamapp;

import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.udacity.lesson.nano.streamapp.service.PlayerService;
import com.udacity.lesson.nano.streamapp.service.PlayerServiceListener;
import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItem;

import java.util.ArrayList;

import static com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys.ARTIST_NAME;
import static com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys.TOP_TRACKS;
import static com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys.TRACK_DURATION;
import static com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys.TRACK_NUMBER;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

public class PlayerActivityFragment extends DialogFragment implements PlayerServiceListener {

    private final static String TAG = PlayerActivityFragment.class.getSimpleName();

    private ViewHolder holder;

    private volatile boolean isScrubbing; // true while the user has grabbed the seekbar
    private volatile boolean isWaitingOnNextMediaPlayerAction; // true while the service prepares for next track
    private volatile boolean isStopped;

    private int trackIndex;
    private ArrayList<SpotifyItem.Track> trackList;
    private String artistName;
    private int trackDuration;

    private PlayerService service;
    private ServiceConnection serviceConnection;


    private Intent serviceIntent;

    @Override
    public void onStart() {
        Log.v(TAG, "onStart()");
        super.onStart();
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.v(TAG, "onServiceConnected()");
                service = ((PlayerService.PlayerServiceBinder) binder).getService();
                service.setListener(PlayerActivityFragment.this);
                service.play(trackList.get(trackIndex));
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.v(TAG, "onServiceDisconnected()");
                service.setListener(null);
                serviceConnection = null;
                service = null;
            }
        };

        serviceIntent = new Intent(getActivity().getApplicationContext(), PlayerService.class);
        getActivity().startService(serviceIntent);
        getActivity().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy()");
        super.onDestroy();
    }


    @Override
    public void onStop() {
        Log.v(TAG, "onStop()");
        isStopped = true;
        getActivity().unbindService(serviceConnection);
        super.onStop();
    }

    // https://code.google.com/p/android/issues/detail?id=17423
    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.v(TAG, "onSaveInstanceState()");
        outState.putInt(TRACK_NUMBER, trackIndex);
        outState.putInt(TRACK_DURATION, trackDuration);
        outState.putParcelableArrayList(TOP_TRACKS, trackList);
        outState.putString(ARTIST_NAME, artistName);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView() begin");
        setRetainInstance(true);

        if (savedInstanceState != null) {
            Log.v(TAG, "savedInstanceState != null");
            trackIndex = savedInstanceState.getInt(TRACK_NUMBER, 0);
            trackList = savedInstanceState.getParcelableArrayList(TOP_TRACKS);
            artistName = savedInstanceState.getString(ARTIST_NAME);
            trackDuration = savedInstanceState.getInt(TRACK_DURATION);
        } else {
            Bundle bundle = getArguments();
            if (bundle != null) {
                trackIndex = bundle.getInt(TRACK_NUMBER, 0);
                trackList = bundle.getParcelableArrayList(TOP_TRACKS);
                artistName = bundle.getString(ARTIST_NAME);
            } else {
                Intent intent = getActivity().getIntent();
                trackIndex = intent.getIntExtra(TRACK_NUMBER, 0);
                trackList = intent.getParcelableArrayListExtra(TOP_TRACKS);
                artistName = intent.getStringExtra(ARTIST_NAME);
            }
        }

        View rootView = inflater.inflate(R.layout.fragment_player, container, false);
        holder = new ViewHolder(rootView);

        holder.artistNameTextView.setText(artistName);
        SpotifyItem.Track track = trackList.get(trackIndex);
        setTrackInfos(track);

        final int trackListSize = trackList.size();
        holder.nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isWaitingOnNextMediaPlayerAction) {
                    trackIndex = ++trackIndex % trackListSize;
                    service.play(trackList.get(trackIndex));
                    isWaitingOnNextMediaPlayerAction = true;
                }
            }
        });
        holder.previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isWaitingOnNextMediaPlayerAction) {
                    trackIndex = trackIndex == 0 ? trackListSize - 1 : --trackIndex % trackListSize;
                    service.play(trackList.get(trackIndex));
                    isWaitingOnNextMediaPlayerAction = true;
                }
            }
        });
        holder.playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isWaitingOnNextMediaPlayerAction) {
                    service.togglePlay();
                }
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
                    service.jumpTo(lastProgress);
                }
                isScrubbing = false;
            }
        });
        if (trackDuration != 0) {
            holder.seekBar.setMax(trackDuration);
        }

        Log.v(TAG, "onCreateView() end");
        return rootView;
    }

    private String millisToFormattedString(int aMillis) {
        return String.format("%02d:%02d", MILLISECONDS.toMinutes(aMillis),
                MILLISECONDS.toSeconds(aMillis) - MINUTES.toSeconds(MILLISECONDS.toMinutes(aMillis)));
    }

    private void setTrackInfos(final SpotifyItem.Track track) {
        holder.trackNameTextView.setText(track.name);
        holder.albumNameTextView.setText(track.albumName);
        holder.trackLengthTextView.setText(millisToFormattedString(track.durationMs));
        ImageLoaderUtils.showLargeImageView(holder.albumArtwork, track.largeImageUrl);
    }

    @Override
    public void onStarted(final SpotifyItem.Track track, final int aDurationMs) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "onStarted() duration=" + aDurationMs);
                setTrackInfos(track);
                holder.seekBar.setMax(aDurationMs);
                trackDuration = aDurationMs;
                holder.playButton.setImageResource(android.R.drawable.ic_media_pause);
                isWaitingOnNextMediaPlayerAction = false;
            }
        });
    }

    @Override
    public void onPaused() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "onPaused()");
                holder.playButton.setImageResource(android.R.drawable.ic_media_play);
            }
        });
    }

    @Override
    public void onResumed() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "onResumed()");
                holder.playButton.setImageResource(android.R.drawable.ic_media_pause);
            }
        });
    }

    @Override
    public void onProgress(final int aProgress) {

        if( !isVisible() ) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "onProgress() progress=" + aProgress + " isscrub=" + isScrubbing + " max=" + holder.seekBar.getMax());
                if (!isScrubbing) {
                    holder.seekBar.setProgress(aProgress);
                }
                holder.trackPosition.setText(millisToFormattedString(aProgress));
            }
        });
    }

    @Override
    public void onFinished() {
        Log.v(TAG, "onFinished()");
        int max = holder.seekBar.getMax();
        onProgress(max);
    }

    // ViewHolder Pattern as recommended by the review of Lesson 1: I like it!
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
            artistNameTextView = getTextView(aView, R.id.player_artist_name);
            trackNameTextView = getTextView(aView, R.id.player_track_name);
            albumNameTextView = getTextView(aView, R.id.player_album_name);
            seekBar = get(aView, R.id.player_seek_bar, SeekBar.class);
            trackLengthTextView = getTextView(aView, R.id.player_track_length);
            playButton = getImageButton(aView, R.id.player_play);
            nextButton = getImageButton(aView, R.id.player_next);
            previousButton = getImageButton(aView, R.id.player_previous);
            trackPosition = getTextView(aView, R.id.player_track_position);
            albumArtwork = get(aView, R.id.player_album_artwork, ImageView.class);
        }

        private TextView getTextView(View aView, int aId) {
            return get(aView, aId, TextView.class);
        }

        private ImageButton getImageButton(View aView, int aId) {
            return get(aView, aId, ImageButton.class);
        }

        private <T> T get(View aView, int aId, Class<T> aClass) {
            return aClass.cast(aView.findViewById(aId));
        }
    }
}