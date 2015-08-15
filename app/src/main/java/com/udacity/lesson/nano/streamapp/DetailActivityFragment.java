package com.udacity.lesson.nano.streamapp;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyCallback;
import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItem;
import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyRequester;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DetailActivityFragment extends Fragment implements SpotifyCallback<SpotifyItem.Track> {

    private static final String LOG_TAG = DetailActivityFragment.class.getSimpleName();

    private SpotifyItemAdapter.Track mSpotifyAdapter;

    private ListView mListView;

    private final static String TOP_TRACKS = "top.tracks";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final List<SpotifyItem.Track> trackList = new ArrayList<>();

        if (savedInstanceState != null) {
            ArrayList<SpotifyItem.Track> items = savedInstanceState.getParcelableArrayList(TOP_TRACKS);
            if (items != null || !items.isEmpty()) {
                Log.d(LOG_TAG, "restoring " + track(items) + " from local cache");
                trackList.addAll(items);
            }
        }

        mSpotifyAdapter = new SpotifyItemAdapter.Track(getActivity(), 0, trackList);

        if (trackList.isEmpty()) { // if we have nothing in the track list, lets request it from the server

            // Do I really have to check this way to differentiate between single and two pane mode?

            // first call: started via intent gives us the artistId
            String artistId = getActivity().getIntent().getStringExtra(MainActivityFragment.ARTIST_ID);
            if (artistId == null) {
                // second call: fragment was launched directly
                artistId = getArguments().getString(MainActivityFragment.ARTIST_ID);
            }

            SpotifyRequester.getInstance().queryTopTracks(artistId, this);
        }

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        mListView = (ListView) rootView.findViewById(R.id.listview_tracks);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
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

                //

            }
        });

        mListView.setAdapter(mSpotifyAdapter);

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mSpotifyAdapter.getCount() > 0) {

            // why am I getting an error trying to put the list directly?
            // outState.putParcelableList(TOP_TRACKS, mSpotifyAdapter);


            // well. dont bother, just make a new list using an old-school loop:
            ArrayList<Parcelable> items = new ArrayList<>();
            for (int i = 0; i < mSpotifyAdapter.getCount(); i++) {
                items.add(mSpotifyAdapter.getItem(i));
            }
            outState.putParcelableArrayList(TOP_TRACKS, items);
            Log.d(LOG_TAG, "saving " + track(items) + " to local cache");
        }
    }

    private String track(List<?> aList) {
        return aList.size() + " track item" + (aList.size() > 1 ? "s" : "");
    }


    @Override
    public void onUpdate(List<SpotifyItem.Track> aItems) {
        if (aItems.isEmpty()) {
            // learnt during "Project 0 - My App Portfolio":
            // display a toast and "outsource" strings in the according XML file
            Toast toast = Toast.makeText(getView().getContext(), R.string.no_top_tracks_found_toast, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        mListView.smoothScrollToPosition(0);
        mSpotifyAdapter.clear();
        mSpotifyAdapter.addAll(aItems);
        mSpotifyAdapter.notifyDataSetChanged(); // notify only once
    }
}
