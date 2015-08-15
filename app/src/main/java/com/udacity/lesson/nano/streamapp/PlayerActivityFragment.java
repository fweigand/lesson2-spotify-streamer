package com.udacity.lesson.nano.streamapp;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItem;
import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyRequester;

import java.io.IOException;
import java.util.ArrayList;


/**
 * A placeholder fragment containing a simple view.
 */
public class PlayerActivityFragment extends Fragment {

    final static String TRACK_NUMBER = "top.tracks.number";

    public PlayerActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Intent intent = getActivity().getIntent();

        intent.getParcelableArrayListExtra(DetailActivityFragment.TOP_TRACKS);

        ArrayList<SpotifyItem.Track> trackList =  intent.getParcelableArrayListExtra(DetailActivityFragment.TOP_TRACKS);
        int position = intent.getIntExtra(TRACK_NUMBER, 0);

        SpotifyItem.Track track =  trackList.get(position);
        String url = track.url;
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepare(); // might take long! (for buffering, etc)
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.start();

        View rootView = inflater.inflate(R.layout.fragment_player, container, false);


        return rootView;
    }
}
