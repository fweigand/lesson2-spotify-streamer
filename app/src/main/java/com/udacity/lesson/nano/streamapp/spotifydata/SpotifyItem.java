package com.udacity.lesson.nano.streamapp.spotifydata;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * POJO holding Spotify data.
 * <p/>
 * The base class holds common data
 */
public class SpotifyItem {

    public final int popularity;
    public final String imageUrl;
    public final String name;

    public SpotifyItem(String aName, String aImageUrl, int aPopularity) {
        popularity = aPopularity;
        name = aName;
        imageUrl = aImageUrl;
    }

    // holds artist specific data
    public static class Artist extends SpotifyItem implements Parcelable {

        public final String id;

        public Artist(String aName, String aImageUrl, int aPopularity, String aId) {
            super(aName, aImageUrl, aPopularity);
            id = aId;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(popularity);
            dest.writeString(imageUrl);
            dest.writeString(name);
            dest.writeString(id);
        }

        // used when passed around as Intent data
        public static final Parcelable.Creator<Artist> CREATOR = new Parcelable.Creator<Artist>() {
            @Override
            public Artist createFromParcel(Parcel in) {
                int popularity = in.readInt();
                String imageUrl = in.readString();
                String name = in.readString();
                String id = in.readString();
                return new Artist(name, imageUrl, popularity, id);
            }

            @Override
            public Artist[] newArray(int size) {
                return new Artist[size];
            }
        };
    }

    // holds track specific data
    public static class Track extends SpotifyItem implements Parcelable {

        public final String albumName;
        public final String trackUrl;
        public final String largeImageUrl;
        public final int durationMs;

        public Track(String aName, String aImageUrl, int aPopularity, String aAlbumName,
                     String aTrackUrl, String aLargeImageUrl, int aDurationMs) {
            super(aName, aImageUrl, aPopularity);
            albumName = aAlbumName;
            trackUrl = aTrackUrl;
            largeImageUrl = aLargeImageUrl;
            durationMs = aDurationMs;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(popularity);
            dest.writeString(imageUrl);
            dest.writeString(name);
            dest.writeString(albumName);
            dest.writeString(trackUrl);
            dest.writeString(largeImageUrl);
            dest.writeInt(durationMs);
        }

        // used when passed around as Intent data
        public static final Parcelable.Creator<Track> CREATOR = new Parcelable.Creator<Track>() {
            @Override
            public Track createFromParcel(Parcel in) {
                int popularity = in.readInt();
                String imageUrl = in.readString();
                String name = in.readString();
                String albumName = in.readString();
                String trackUrl = in.readString();
                String largeImageUrl = in.readString();
                int durationMs = in.readInt();
                return new Track(name, imageUrl, popularity, albumName, trackUrl, largeImageUrl,
                        durationMs);
            }

            @Override
            public Track[] newArray(int size) {
                return new Track[size];
            }
        };
    }
}