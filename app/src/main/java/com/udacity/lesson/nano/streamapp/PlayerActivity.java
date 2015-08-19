package com.udacity.lesson.nano.streamapp;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys;

import static com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys.ARTIST_ID;
import static com.udacity.lesson.nano.streamapp.spotifydata.SpotifyItemKeys.ARTIST_NAME;


public class PlayerActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String prefix = getString(R.string.now_playing);
        setTitle(prefix + " " + getIntent().getStringExtra(ARTIST_NAME));

                setContentView(R.layout.activity_player);
//        boolean twoPane = getIntent().getBooleanExtra("two.pane.mode", false);
//        if( !twoPane ) {
//        } else {
//            // setContentView(R.layout.activity_detail);
//            PlayerActivityFragment fragment = new PlayerActivityFragment();
//            Bundle bundle = new Bundle();
//            String artistId = getIntent().getStringExtra(ARTIST_ID);
//            bundle.putString(ARTIST_ID, artistId);
//            fragment.setArguments(bundle);
//            fragment.show(getFragmentManager(), "player");
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
