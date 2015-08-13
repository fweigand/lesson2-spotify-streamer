package com.udacity.lesson.nano.lession1_spotifystreamer;

import android.view.View;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

/**
 * Simple helper class to interface with the Picasso library.
 */
public class ImageLoaderUtils {

    private final static int DEFAULT_THUMBNAIL_ID = R.mipmap.ic_launcher;

    /**
     * Displays an image in the ImageView with {@code aViewId} by first loading it from the provided URL and then
     * asynchronously setting it.
     * If no URL is passed in, the ImageView displays a default image.
     * @param aView root view for looking up aViewId
     * @param aViewId ID of a View that is expected to be an {@code ImageView}
     * @param aUrl URL to load the image from or null
     */
    public static void showInImageView(View aView, int aViewId, String aUrl) {
        ImageView imageView = (ImageView) aView.findViewById(aViewId);
        if (aUrl != null) {
            // I still havent found a good way to let the layouting decide which size to use.
            // Is this supposed to be configured in one of the XML files?
            // with the centerInside() method at least the aspect ratio is kept
            // resize to 100x100 ensures the ImageView is set to reasonable dimensions
            // but 100x100 seems not ok for me, if you think about different devices with different screen resolutions
            Picasso.with(aView.getContext()).load(aUrl).resize(100,100).centerInside().into(imageView);
        } else {
            imageView.setImageResource(DEFAULT_THUMBNAIL_ID);
        }
    }

    private ImageLoaderUtils() {} // cannot instantiate - static method helper class
}
