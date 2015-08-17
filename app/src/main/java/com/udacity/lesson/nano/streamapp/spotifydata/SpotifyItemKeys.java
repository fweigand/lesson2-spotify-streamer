package com.udacity.lesson.nano.streamapp.spotifydata;

/**
 * Constants class holding predefined keys for accessing various data from the SpotifyItems, e.g.
 * when used as Intent and/or Parcelable data.
 */
public class SpotifyItemKeys {

    public static final String ARTIST_ID = "artist.id";

    public static final String ARTIST_QUERY_RESULT = "artist.query.result";

    public static final String ARTIST_NAME = "artist.name";

    public static final String TOP_TRACKS = "top.tracks";

    public final static String TRACK_NUMBER = "top.tracks.number";

    private SpotifyItemKeys() {}
}