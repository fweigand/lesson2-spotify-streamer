package com.udacity.lesson.nano.streamapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyCallback;
import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItem;
import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyRequester;

import java.util.ArrayList;
import java.util.List;

public class MainActivityFragment extends Fragment implements SpotifyCallback<SpotifyItem.Artist> {

    static final String ARTIST_ID = "spotify.artist.id";
    static final String ARTIST_NAME = "spotify.artist.name";
    private static final String LOG_TAG = MainActivityFragment.class.getSimpleName();
    private SpotifyItemAdapter.Artist mSpotifyAdapter;

    private ListView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        List<SpotifyItem.Artist> initialEntry = new ArrayList<>();

        final String enterSearchPhrase = rootView.getContext().getString(R.string.enter_search_phrase);
        SpotifyItem.Artist emptyFlavor = new SpotifyItem.Artist(enterSearchPhrase, null, 0, null);
        initialEntry.add(emptyFlavor);
        mSpotifyAdapter = new SpotifyItemAdapter.Artist(getActivity(), 0, initialEntry);

        mListView = (ListView) rootView.findViewById(R.id.listview_artists);
        mListView.setAdapter(mSpotifyAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                SpotifyItem.Artist flavor = mSpotifyAdapter.getItem(position);
                if (enterSearchPhrase.equals(flavor.name)) {
                    return;
                }

                if (getActivity().findViewById(R.id.artist_detail_container) != null) {
                    // In two-pane mode, show the detail view in this activity by
                    // adding or replacing the detail fragment using a
                    // fragment transaction.
                    Bundle arguments = new Bundle();
                    arguments.putString(ARTIST_ID, flavor.id);
                    arguments.putString(ARTIST_NAME, flavor.name);

                    DetailActivityFragment fragment = new DetailActivityFragment();
                    fragment.setArguments(arguments);
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.artist_detail_container, fragment).commit();

                } else {
                    Intent intent = new Intent(getActivity(), DetailActivity.class)
                            .putExtra(ARTIST_ID, flavor.id).putExtra(ARTIST_NAME, flavor.name);
                    startActivity(intent);
                }

            }
        });

        // we handle updates ourselves
        // In "Lession 2: Connect Sunshine to the Cloud in Chapter "Source Code for ArrayAdapter"
        // of the "Developing Android Apps: Fundamentals" course, we are encouraged to
        // "not to be afraid to try this on our own"
        mSpotifyAdapter.setNotifyOnChange(false);

        EditText editText = (EditText) rootView.findViewById(R.id.artist_search_edittext);

        // although it would be cool to have live queries, while typing text, it is not required by the spec
        // "P1 - Spotify Streamer, Stage 1 Rubric"
        // https://docs.google.com/document/d/1Q7b2dIt18r2pXqDhE_TsgZ5UkfzbybkGlmKfyoqpegc/pub?embedded=true
//        editText.addTextChangedListener(new TextWatcher() {
//            @Override  public void beforeTextChanged(CharSequence s, int start, int count, int after) { Log.d(LOG_TAG, "before text changed: " + s.toString()); }
//            @Override  public void onTextChanged(CharSequence s, int start, int before, int count) { Log.d(LOG_TAG, "on text changed: " + s.toString()); }
//            @Override  public void afterTextChanged(Editable s) {  Log.d(LOG_TAG, "after text changed: " + s.toString()); } });

        // idea of using a search action from: http://developer.android.com/guide/topics/ui/controls/text.html
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = actionId == EditorInfo.IME_ACTION_SEARCH;
                if (handled) {
                    SpotifyRequester.getInstance().queryArtist(v.getText().toString(), MainActivityFragment.this);
                    Log.d(LOG_TAG, "handled: " + v.getText());
                }
                return handled;
            }
        });
        return rootView;
    }

    @Override
    public void onUpdate(List<SpotifyItem.Artist> aItems) {
        if (aItems.isEmpty()) {
            // learnt during "Project 0 - My App Portfolio":
            // display a toast and "outsource" strings in the according XML file
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(mSpotifyAdapter.getContext(), R.string.no_artist_found_toast, duration);
            toast.show();
            return;
        }

        mListView.smoothScrollToPosition(0);
        mSpotifyAdapter.clear();
        mSpotifyAdapter.addAll(aItems);
        mSpotifyAdapter.notifyDataSetChanged(); // notify only once
    }
}
