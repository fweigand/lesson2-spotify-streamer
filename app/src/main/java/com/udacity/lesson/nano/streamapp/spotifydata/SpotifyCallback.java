package com.udacity.lesson.nano.streamapp.spotifydata;

import java.util.List;

/**
 * Callback interface use to update our View once responses from the Spotify Service have been received
 * @param <T> Type of received items
 */
public interface SpotifyCallback<T extends SpotifyItem> {

    /**
     * Called when a response for a spotify {@code SpotifyService.searchArtists()} or {@code SpotifyService.getArtistTopTrack()}
     * request was received and fully evaluated
     *
     * @param aItems containing the results or an empty list if no artist/track was found or an error occurred
     */
    void onUpdate(List<T> aItems);
}
