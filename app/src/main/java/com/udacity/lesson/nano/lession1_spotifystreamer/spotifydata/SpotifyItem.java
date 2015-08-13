package com.udacity.lesson.nano.lession1_spotifystreamer.spotifydata;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * POJO holding Spotify data.
 * <p>
 *     The base class holds common data
 */
public  class SpotifyItem {

    public final int popularity;
    public final String imageUrl;
    public final String name;

    public SpotifyItem(String aName, String aImageUrl, int aPopularity) {
        popularity = aPopularity;
        name = aName;
        imageUrl = aImageUrl;
    }

    // holds artist specific data
    public static class Artist extends SpotifyItem {

        public final String id;

        public Artist(String aName, String aImageUrl, int aPopularity, String aId) {
            super(aName, aImageUrl, aPopularity);
            id = aId;
        }
    }

    // holds track specific data
    public static class Track extends SpotifyItem implements Parcelable {

        public final String albumName;

        public Track(String aName, String aImageUrl, int aPopularity, String aAlbumName) {
            super( aName, aImageUrl, aPopularity );
            albumName = aAlbumName;
        }

        @Override public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(popularity);
            dest.writeString(imageUrl);
            dest.writeString(name);
            dest.writeString(albumName);
        }
    }
}
