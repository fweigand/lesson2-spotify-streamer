package com.udacity.lesson.nano.streamapp;

import android.content.Intent;
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

import java.util.ArrayList;
import java.util.List;

import static com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys.ARTIST_ID;
import static com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys.ARTIST_NAME;
import static com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys.TOP_TRACKS;
import static com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys.TRACK_NUMBER;

public class DetailActivityFragment extends Fragment implements SpotifyCallback<SpotifyItem.Track> {

    private static final String LOG_TAG = DetailActivityFragment.class.getSimpleName();

    private SpotifyItemAdapter.Track mSpotifyAdapter;

    private ListView mListView;

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

        boolean twoPaneModeParam;
        String artistIdParam;
        String artistNameParam;

        if (savedInstanceState != null) {
            twoPaneModeParam = savedInstanceState.getBoolean("two.pane.mode", false);
            artistIdParam = savedInstanceState.getString(ARTIST_ID);
            artistNameParam = savedInstanceState.getString(ARTIST_NAME);
        } else {
            Bundle bundle = getArguments();
            if (bundle != null) {
                twoPaneModeParam = bundle.getBoolean("two.pane.mode", false);
                artistIdParam = bundle.getString(ARTIST_ID);
                artistNameParam = bundle.getString(ARTIST_NAME);
            } else {
                Intent intent = getActivity().getIntent();
                twoPaneModeParam = intent.getBooleanExtra("two.pane.mode", false);
                artistIdParam = intent.getStringExtra(ARTIST_ID);
                artistNameParam = intent.getStringExtra(ARTIST_NAME);
            }
        }

        final boolean twoPaneMode = twoPaneModeParam;
        final String artistId = artistIdParam;
        final String artistName = artistNameParam;

        if (trackList.isEmpty()) { // if we have nothing in the track list, lets request it from the server
            SpotifyRequester.getInstance().queryTopTracks(artistIdParam, this);
        }

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        mListView = (ListView) rootView.findViewById(R.id.listview_tracks);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                ArrayList<Parcelable> items = asParcelable();
                if( twoPaneMode ) { // launch directly
                    PlayerActivityFragment fragment = new PlayerActivityFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelableArrayList(TOP_TRACKS, items);
                    bundle.putString(ARTIST_NAME, artistName);
                    bundle.putString(ARTIST_ID, artistId);
                    bundle.putInt(TRACK_NUMBER, position);
                    fragment.setArguments(bundle);
                    fragment.show(getActivity().getFragmentManager(), "player");
                } else { // launch via intent
                    Intent intent = new Intent(getActivity(), PlayerActivity.class);
                    intent.putParcelableArrayListExtra(TOP_TRACKS, items);
                    intent.putExtra(ARTIST_NAME, artistName);
                    intent.putExtra(ARTIST_ID, artistId);
                    intent.putExtra(TRACK_NUMBER, position);
                    startActivity(intent);
                }
            }
        });
        mListView.setAdapter(mSpotifyAdapter);
        return rootView;
    }

    // convert the adapter elements to parcelable
    private ArrayList<Parcelable> asParcelable() {
        // why am I getting an error trying to put the list directly?
        // outState.putParcelableList(TOP_TRACKS, mSpotifyAdapter);

        // well. dont bother, just make a new list using an old-school loop:
        ArrayList<Parcelable> items = new ArrayList<>();
        for (int i = 0; i < mSpotifyAdapter.getCount(); i++) {
            items.add(mSpotifyAdapter.getItem(i));
        }
        return items;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mSpotifyAdapter.getCount() > 0) {
            ArrayList<Parcelable> items = asParcelable();
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
            Toast toast = Toast.makeText(getView().getContext(),
                    R.string.no_top_tracks_found_toast, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        mListView.smoothScrollToPosition(0);
        mSpotifyAdapter.clear();
        mSpotifyAdapter.addAll(aItems);
        mSpotifyAdapter.notifyDataSetChanged(); // notify only once
    }
}