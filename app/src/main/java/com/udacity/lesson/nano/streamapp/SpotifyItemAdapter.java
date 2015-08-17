package com.udacity.lesson.nano.streamapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItem;

import java.util.List;

/**
 * Holds the common logic that apply for both SpotifyItem.Artist and SpotifyItem.Track, e.g. setting and image
 * in an ImageView.
 * <p/>
 * Track and Artist specifics are implemented in a corresponding sub-class
 *
 * @param <T> type of the Adapter Item "Artist" or "Track"
 */
public abstract class SpotifyItemAdapter<T extends SpotifyItem> extends ArrayAdapter<T> {

    public SpotifyItemAdapter(Context context, int resource, List<T> aItems) {
        super(context, resource, aItems);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(getListLayoutId(), parent, false);
        }
        final T item = getItem(position);
        ImageLoaderUtils.showInImageView(convertView, getImageViewId(), item.imageUrl);

        TextView artistNameTextView = (TextView) convertView.findViewById(getTextViewForNameId());
        artistNameTextView.setText(item.name);

        updateViewImpl(convertView, item);
        return convertView;
    }

    protected abstract int getListLayoutId();

    protected abstract int getImageViewId();

    protected abstract int getTextViewForNameId();

    protected void updateViewImpl(View convertView, T aItem) {}

    // Specialization, that sets Artist related stuff to the appropriate widget elements
    public static class Artist extends SpotifyItemAdapter<SpotifyItem.Artist> {
        public Artist(Context c, int r, List<SpotifyItem.Artist> i) {
            super(c, r, i);
        }
        @Override protected int getListLayoutId() {
            return R.layout.list_item_artist;
        }
        @Override protected int getImageViewId() {
            return R.id.list_item_artist_imageview;
        }
        @Override protected int getTextViewForNameId() {
            return R.id.list_item_artist_name;
        }
    }

    // Specialization, that sets Track related stuff to the appropriate widget elements
    public static class Track extends SpotifyItemAdapter<SpotifyItem.Track> {
        public Track(Context c, int r, List<SpotifyItem.Track> i) {
            super(c, r, i);
        }
        @Override protected int getListLayoutId() {
            return R.layout.list_item_track;
        }
        @Override protected int getImageViewId() {
            return R.id.list_item_track_imageview;
        }
        @Override protected int getTextViewForNameId() {
            return R.id.list_item_track_name;
        }
        protected void updateViewImpl(View convertView, SpotifyItem.Track aItem) {
            TextView albumNameTextView = (TextView) convertView.findViewById(R.id.list_item_album_name);
            albumNameTextView.setText(aItem.albumName);
        }
    }
}