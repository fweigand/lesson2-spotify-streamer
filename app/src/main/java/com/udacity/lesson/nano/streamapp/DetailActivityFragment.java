package com.udacity.lesson.nano.streamapp;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyCallback;
import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItem;
import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyRequester;

import java.util.ArrayList;
import java.util.List;

public class DetailActivityFragment extends Fragment implements SpotifyCallback<SpotifyItem.Track> {

    private static final String LOG_TAG = DetailActivityFragment.class.getSimpleName();

    private SpotifyItemAdapter.Track mSpotifyAdapter;

    private ListView mListView;

    private final static String TOP_TRACKS = "top.tracks";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        List<SpotifyItem.Track> trackList = new ArrayList<>();

        if (savedInstanceState != null) {
            ArrayList<SpotifyItem.Track> items = savedInstanceState.getParcelableArrayList(TOP_TRACKS);
            if (items != null || !items.isEmpty()) {
                Log.d(LOG_TAG, "restoring " + track( items ) +" from local cache");
                trackList.addAll(items);
            }
        }

        mSpotifyAdapter = new SpotifyItemAdapter.Track(getActivity(), 0, trackList);

        if (trackList.isEmpty()) { // if we have nothing in the track list, lets request it from the server

            // question do I really have to check this way where the first call is null if not started via an Intent
            // "Two Pane Mode" ?
            String artistId = getActivity().getIntent().getStringExtra(MainActivityFragment.ARTIST_ID);
            if( artistId == null ) {
                artistId = getArguments().getString(MainActivityFragment.ARTIST_ID);
            }

            SpotifyRequester.getInstance().queryTopTracks(artistId, this);
        }

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        mListView = (ListView) rootView.findViewById(R.id.listview_tracks);
        mListView.setAdapter(mSpotifyAdapter);

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mSpotifyAdapter.getCount() > 0) {
            // why am I getting an error trying to call this ?
            // outState.putParcelableList(TOP_TRACKS, mySpotifyAdapterList);
            // well. dont bother, just make a new list using an old-school loop:
            ArrayList<Parcelable> items = new ArrayList<>();
            for (int i = 0; i < mSpotifyAdapter.getCount(); i++) {
                items.add(mSpotifyAdapter.getItem(i));
            }
            outState.putParcelableArrayList(TOP_TRACKS, items);
            Log.d(LOG_TAG, "saving " + track( items ) + " to local cache");
        }
    }

    private String track( List<?> aList ) { return aList.size() +  " track item"+(aList.size()>1?"s":"");  }


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
