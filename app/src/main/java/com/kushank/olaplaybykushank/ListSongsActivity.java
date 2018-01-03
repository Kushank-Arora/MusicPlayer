package com.kushank.olaplaybykushank;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;

import java.util.ArrayList;

/*
* The class is used to display song results for 'Recent' as well as 'My Playlist'
 */

public class ListSongsActivity extends AppCompatActivity {

    public static final String PARA_RECENT = "para_history";
    private ArrayList<SongModel> toBeDisplayed;
    private CustomAdapter adapter;
    private boolean isRecentActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_playlist);

        //check if activity is opened as recent or playlist
        isRecentActivity = getIntent().getBooleanExtra(PARA_RECENT, false);

        RecyclerView recyclerView = findViewById(R.id.rv_list_songs);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        toBeDisplayed = new ArrayList<>();
        adapter = new CustomAdapter(this, toBeDisplayed, !isRecentActivity);
        recyclerView.setAdapter(adapter);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        if(isRecentActivity)
            actionBar.setTitle("Recently Played");
        else
            actionBar.setTitle("My Playlist");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onResume() {
        super.onResume();
        toBeDisplayed.clear();
        ArrayList<SongModel> songs;

        PlaylistSQLDB playlistDB = new PlaylistSQLDB(this);
        playlistDB.open();

        //based upon the activity opened, the data is fetched from the DB
        if(isRecentActivity)
            songs = playlistDB.getData(PlaylistSQLDB.TypeOfData.RECENT);
        else
            songs = playlistDB.getData(PlaylistSQLDB.TypeOfData.IN_PLAYLIST);
        playlistDB.close();

        //the fetched songs are added in the list to be displayed.
        toBeDisplayed.addAll(songs);
        adapter.notifyDataSetChanged();
    }
}
