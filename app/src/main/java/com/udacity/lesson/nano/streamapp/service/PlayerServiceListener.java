package com.udacity.lesson.nano.streamapp.service;

import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItem;

public interface PlayerServiceListener {

    void onStarted(SpotifyItem.Track track, int aDurationMs);

    void onPaused();

    void onResumed();

    void onProgress(int aProgress);

    void onFinished();

}
