package com.udacity.lesson.nano.streamapp;

import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
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

import org.w3c.dom.Text;

import java.util.List;

import static com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys.ARTIST_NAME;
import static com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys.TOP_TRACKS;
import static com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys.TRACK_NUMBER;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

public class PlayerActivityFragment extends DialogFragment implements PlayerServiceListener {

    private final static String TAG = PlayerActivityFragment.class.getSimpleName();

    private ViewHolder holder;

    private volatile boolean isScrubbing; // true while the user has grabbed the seekbar
    private volatile boolean isWaitingOnNextMediaPlayerAction; // true while the service prepares for next track

    private int trackIndex;
    private List<SpotifyItem.Track> trackList;

    private PlayerService service;
    private ServiceConnection serviceConnection;

    @Override
    public void onStart() {
        Log.i(TAG, "onStart()");
        super.onStart();
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.i(TAG, "onServiceConnected()");
                service = ((PlayerService.PlayerServiceBinder) binder).getService();
                service.setListener(PlayerActivityFragment.this);
                service.play(trackList.get(trackIndex));
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.i(TAG, "onServiceDisconnected()");
            }
        };
        Intent serviceIntent = new Intent(getActivity().getApplicationContext(), PlayerService.class);
        getActivity().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop()");
        getActivity().unbindService(serviceConnection);
        serviceConnection = null;
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(TRACK_NUMBER, trackIndex);
        Log.d(TAG, "onSaveInstanceState()");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Intent intent = getActivity().getIntent();
        Log.i(TAG, "onCreateView()");

        if (savedInstanceState != null) {
            Log.d(TAG, "savedInstanceState != null");
            trackIndex = savedInstanceState.getInt(TRACK_NUMBER, 0);
        } else {
            trackIndex = intent.getIntExtra(TRACK_NUMBER, 0);
        }
        trackList = intent.getParcelableArrayListExtra(TOP_TRACKS);

        View rootView = inflater.inflate(R.layout.fragment_player, container, false);
        holder = new ViewHolder(rootView);

        String artistName = intent.getStringExtra(ARTIST_NAME);
        holder.artistNameTextView.setText(artistName);

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
    public void onStarted(final SpotifyItem.Track track, final int aDurationMs) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                holder.trackNameTextView.setText(track.name);
                holder.albumNameTextView.setText(track.albumName);
                holder.seekBar.setMax(aDurationMs);
                holder.trackLengthTextView.setText(millisToFormattedString(track.durationMs));
                holder.playButton.setImageResource(android.R.drawable.ic_media_pause);
                ImageLoaderUtils.showLargeImageView(holder.albumArtwork, track.largeImageUrl);
                isWaitingOnNextMediaPlayerAction = false;
            }
        });
    }

    @Override
    public void onPaused() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "onPaused()");
                holder.playButton.setImageResource(android.R.drawable.ic_media_play);
            }
        });
    }

    @Override
    public void onResumed() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "onResumed()");
                holder.playButton.setImageResource(android.R.drawable.ic_media_pause);
            }
        });
    }

    @Override
    public void onProgress(final int aProgress) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onProgress() progress=" + aProgress);
                if (!isScrubbing) {
                    holder.seekBar.setProgress(aProgress);
                }
                holder.trackPosition.setText(millisToFormattedString(aProgress));
            }
        });
    }

    @Override
    public void onFinished() {
        Log.d(TAG, "onFinished()");
        int max = holder.seekBar.getMax();
        onProgress(max);
    }

    // ViewHolder Pattern as recommended by the review of Lesson 1:
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