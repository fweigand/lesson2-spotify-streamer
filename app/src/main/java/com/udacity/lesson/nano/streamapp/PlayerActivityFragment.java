package com.udacity.lesson.nano.streamapp;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItem;
import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys;

import java.io.IOException;
import java.util.ArrayList;


/**
 * A placeholder fragment containing a simple view.
 */
public class PlayerActivityFragment extends Fragment {

    public PlayerActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Intent intent = getActivity().getIntent();

        intent.getParcelableArrayListExtra(SpotifyItemKeys.TOP_TRACKS);

        ArrayList<SpotifyItem.Track> trackList =  intent.getParcelableArrayListExtra(SpotifyItemKeys.TOP_TRACKS);
        int position = intent.getIntExtra(SpotifyItemKeys.TRACK_NUMBER, 0);


        SpotifyItem.Track track =  trackList.get(position);
        String url = track.trackUrl;
//        MediaPlayer mediaPlayer = new MediaPlayer();
//        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//        try {
//            mediaPlayer.setDataSource(url);
//            mediaPlayer.prepare(); // might take long! (for buffering, etc)
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        mediaPlayer.start();

        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        String artistName = intent.getStringExtra(SpotifyItemKeys.ARTIST_NAME);
        TextView artistNameTextView = (TextView) rootView.findViewById(R.id.player_artist_name);
        artistNameTextView.setText( artistName );

        TextView trackNameTextView = (TextView) rootView.findViewById(R.id.player_track_name);
        trackNameTextView.setText( track.name );

        TextView albumNameTextView = (TextView) rootView.findViewById(R.id.player_album_name);
        albumNameTextView.setText(track.albumName);

        SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.player_seek_bar);
        seekBar.setMax( (int)track.durationMs );

        ImageLoaderUtils.showLargeImageView( rootView, R.id.player_album_artwork, track.largeImageUrl);

        return rootView;
    }
}
