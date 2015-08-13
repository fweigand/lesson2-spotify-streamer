package com.udacity.lesson.nano.lession1_spotifystreamer.spotifydata;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.AlbumSimple;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Although the "Spotify Streamer, Stage 1: Implementation Guide" states the following in
 * Chapter "Task 3: Query the Spotify Web API" under "Programming Notes:"
 * <p/>
 * "Fetch data from Spotify in the background using AsyncTask and The Spotify Web API Wrapper"
 * <p/>
 * I am not using the AsyncTask directly, as the Spotify Wrapper already creates an appropriate  background thread
 * for handling I/O
 * <p/>
 * see also this discussion thread: https://discussions.udacity.com/t/asynctask-vs-callbacks/21223
 * <p/>
 * Downside of this approach, though it is easier to implement is, that the calls to {@code Collection.sort()} and
 * {@code findBestImage()} are executed in the main-UI thread.
 * Changing to AsyncTask, would open the possibility to lets this code run in the background thread too, allowing the
 * main UI thread to spend more time with other instructions.
 */
public class SpotifyRequester {

    private static final String LOG_TAG = SpotifyRequester.class.getSimpleName();
    private final static SpotifyRequester requester = new SpotifyRequester();
    private final SpotifyService mSpotifyService;
    private final Map<String, Object> queryMap;

    private SpotifyRequester() {
        SpotifyApi api = new SpotifyApi();
        mSpotifyService = api.getService();

        // currently hard-coded Country setting
        Map<String, Object> map = new HashMap<>();
        map.put(SpotifyService.COUNTRY, Locale.getDefault().getCountry());
        queryMap = Collections.unmodifiableMap(map);
    }

    // use a single instance for all requests, limiting the amount of threads created
    // since all information is obtained from the same server it should be sufficient to query sequentially.
    // I read from the SpotifyApi source, that instantiating with the default constructor gives us
    // a single thread, that handles the network I/O -> one server - one thread
    // both the DetailActivityFragment and the MainActivityFragment use this instance
    public static SpotifyRequester getInstance() {
        return requester;
    }

    // compares items on based on their popularity
    private final Comparator<SpotifyItem> popularityItemComparator = new Comparator<SpotifyItem>() {
        @Override
        public int compare(SpotifyItem lhs, SpotifyItem rhs) {
            return rhs.popularity - lhs.popularity;
        }
    };

    private String findBestImage(List<Image> images, ImageMatcher aMatcher ) {
        Image image = null;
        for (Image i : images) {
            if (image == null || aMatcher.preferImage(i, image) ) {
                image = i;
            }
        }
        return image == null ? null : image.url;
    }

    // unfortunately no Java8 atm :-(   https://discussions.udacity.com/t/java-8-for-android-development/20578
    private interface ImageMatcher {
        /**
         * @return true, if aImage should be preferred aOverThisImage
         */
        boolean preferImage( Image aImage, Image aOverThisImage );
    }


    public void queryArtist(final String aArtistName, final SpotifyCallback<SpotifyItem.Artist> aCallback) {

        // for the artists we use the smallest image we can find
        final ImageMatcher matcher = new ImageMatcher() {
            @Override
            public boolean preferImage(Image aImage, Image aOverThisImage) {
                // simple size check, we use the smallest one to reduce amount of data transferred over
                // the network. still could be slightly incorrect, if the smaller image had an uncompressed
                // format like BMP while a larger one was jpg or png
                return aImage.width * aImage.height < aOverThisImage.width * aOverThisImage.height;
            }
        };

        mSpotifyService.searchArtists(aArtistName, new Callback<ArtistsPager>() {

            @Override
            public void success(ArtistsPager artistsPager, Response response) {
                Log.d(LOG_TAG, "requesting artist " + aArtistName + " succeeded with: " + response);

                List<SpotifyItem.Artist> list = new ArrayList<>();

                Pager<Artist> artistPager = artistsPager.artists;
                for (Artist artist : artistPager.items) {
                    list.add( new SpotifyItem.Artist(artist.name,
                                       findBestImage(artist.images, matcher),
                                                    artist.popularity,
                                                    artist.id));
                }

                Collections.sort(list, popularityItemComparator);
                aCallback.onUpdate(list);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d(LOG_TAG, "requesting artist " + aArtistName + " failed with: " + error);
                // could define a separate method to pass information about the error and display it to the user
                // but there was nowhere specified, so we treat this condition simply as if no artist was found
                aCallback.onUpdate(Collections.<SpotifyItem.Artist>emptyList());
            }
        });
    }

    public void queryTopTracks(final String aArtistName, final SpotifyCallback<SpotifyItem.Track> aCallback) {

        // for the track list we use a small image, but prefer one with 200x200
        final ImageMatcher matcher = new ImageMatcher() {
            @Override
            public boolean preferImage(Image aImage, Image aOverThisImage) {
                /*
                 * Quote from the "Spotify Streamer, Stage 1: Implementation Guide"
                 *  "Album art thumbnail (large (640px for Now Playing screen) and small (200px for list items)).
                 *   If the image size does not exist in the API response, you are free to choose whatever size is
                 *   available.)"
                 */
                if( aImage.width == 200 && aImage.height == 200 ) {
                    return true;  // this wins if we have a 200x200 at hand
                }
                if( aOverThisImage.width == 200 && aOverThisImage.height == 200 ) {
                    return false; // the other image wins if a 200x200 was already found
                }
                // otherwise use keep the smaller one
                return aImage.width * aImage.height < aOverThisImage.width * aOverThisImage.height;
            }
        };

        mSpotifyService.getArtistTopTrack(aArtistName, queryMap, new Callback<Tracks>() {
            @Override
            public void success(Tracks tracks, Response response) {
                Log.d(LOG_TAG, "requesting tracks for " + aArtistName + " succeeded with: " + response);

                List<SpotifyItem.Track> list = new ArrayList<>();

                for (Track track : tracks.tracks) {
                    AlbumSimple album = track.album;
                    list.add( new SpotifyItem.Track(track.name,
                                      findBestImage(album.images, matcher),
                                                    track.popularity,
                                                    album.name));
                }

                Collections.sort(list, popularityItemComparator);
                final int maxSize = 10; // cap the result list to 10 elements at most
                aCallback.onUpdate(list.size() <= maxSize ? list : list.subList(0, maxSize));
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d(LOG_TAG, "requesting tracks for " + aArtistName + " failed with: " + error +
                                " (reason=" + error.getResponse().getReason() +
                                " (, url=" + error.getResponse().getUrl() + ")" );
                aCallback.onUpdate(Collections.<SpotifyItem.Track>emptyList());
            }
        });
    }
}
