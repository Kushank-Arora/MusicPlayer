package com.kushank.olaplaybykushank;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.exoplayer2.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class MainSongsActivity extends AppCompatActivity {

    private static final String TAG = MainSongsActivity.class.getSimpleName();
    private CustomAdapter adapter;
    private ArrayList<SongModel> allSongs;
    private ArrayList<SongModel> defaultSongs;
    private ArrayList<SongModel> toBeDisplayed;
    private LinearLayout llTransition;

    int curPage = 0;
    ArrayList<ImageView> ivTransitions;
    private android.support.v7.app.ActionBar actionBar;
    private SearchView searchView;
    private CustomSwipeRefresh swipeRefresh;
    private GetSongs task;
    private RecyclerView recyclerView;

    private GestureDetector gestureListener;
    private int max_pages=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_of_songs);

        //This Layout is for the bottom bar showing the option for transition to multiple pages.
        llTransition = findViewById(R.id.ll_transition);

        swipeRefresh = findViewById(R.id.swipeRefresh);

        recyclerView = findViewById(R.id.rv_list_songs);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ivTransitions = new ArrayList<>();
        allSongs = new ArrayList<>();
        defaultSongs = new ArrayList<>();
        toBeDisplayed = new ArrayList<>();
        adapter = new CustomAdapter(this, toBeDisplayed, false);
        recyclerView.setAdapter(adapter);

        //This statement starts an ASYNC task for downloading the song details.
        task = new GetSongs();
        task.execute();

        //This is used to maintain the current page number.
        curPage = 0;

        actionBar = getSupportActionBar();

        //Swipe refresh feature is added.
        swipeRefresh.setRefreshing(true);
        swipeRefresh.setOnRefreshListener(new CustomSwipeRefresh.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(task.isComplete()){
                    task = new GetSongs();
                    task.execute();
                }
                swipeRefresh.setRefreshing(true);
            }
        });
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                //Toast.makeText(MainSongsActivity.this, "onTouch Called",Toast.LENGTH_SHORT).show();
                return !gestureListener.onTouchEvent(motionEvent);
            }
        });

        gestureListener = new GestureDetector(this, new GestureListener() {
            @Override
            public void onSwipeLeft() {
                if(curPage+1<max_pages) {
                    curPage++;
                    reInitTrans();
                }
            }

            @Override
            public void onSwipeRight() {
                if(curPage-1>=0) {
                    curPage--;
                    reInitTrans();
                }
            }

            @Override
            public void onSwipeTop() {

            }

            @Override
            public void onSwipeBottom() {

            }
        });

        maybeRequestPermission();
    }


    @TargetApi(23)
    private void maybeRequestPermission() {
        if (requiresPermission()) {
            requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    @TargetApi(23)
    private boolean requiresPermission() {
        return Util.SDK_INT >= 23
                && (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    //the search result for the cur pattern is displayed.
    private void searchPatAndUI(String pat) {

        //the search result for the cur pattern is displayed.
        toBeDisplayed.clear();
        if (!pat.equals(""))
            for (SongModel song : allSongs) {
                if (song.getName().toLowerCase().contains(pat))
                    toBeDisplayed.add(song);
            }
        adapter.notifyDataSetChanged();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.dashboard, menu);

        MenuItem menuItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) menuItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if(task.isComplete()){
                    task = new GetSongs(s.toLowerCase().trim().replace(' ', '+'));
                    task.execute();
                    swipeRefresh.setRefreshing(true);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //searchPatAndUI(s.toLowerCase().trim());
                return true;
            }
        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //llTransition.setVisibility(View.INVISIBLE);
                //searchPatAndUI("");
                allSongs.clear();
                toBeDisplayed.clear();
                adapter.notifyDataSetChanged();
                inflateCircles(1);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                //llTransition.setVisibility(View.VISIBLE);
                allSongs.clear();
                allSongs.addAll(defaultSongs);
                curPage=0;
                dataSetChanged();
                actionBar.setDisplayHomeAsUpEnabled(false);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_about_me:
                startActivity(new Intent(this, Portfolio.class));
                return true;
            case R.id.action_history:
                startActivity(new Intent(this, ListSongsActivity.class).putExtra(ListSongsActivity.PARA_RECENT,true));
                return true;
            case android.R.id.home:
                if (searchView != null)
                    onBackPressed();
                return true;
            case R.id.action_playlist:
                startActivity(new Intent(this, ListSongsActivity.class).putExtra(ListSongsActivity.PARA_RECENT,false));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDownOrInPlaylist();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
        } else {
            super.onBackPressed();
        }
    }

    //This is used to get the song details from the server.
    @SuppressLint("StaticFieldLeak")
    class GetSongs extends AsyncTask<Void, Void, Void> {

        boolean done;
        String query;

        GetSongs() {
            this.query = "rafi";
        }

        GetSongs(String query) {
            this.query = query;
        }

        boolean isComplete(){
            return done;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            done = false;
            allSongs.clear();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            HttpHandler sh = new HttpHandler();
            // Making a request to url and getting response
            String url = getString(R.string.url_songs) + query;
            String jsonStr = sh.makeServiceCall(url);
            Log.d(TAG, "Connecting... ");

            if (jsonStr != null) {
                try {
                    JSONArray songsJson = new JSONArray(jsonStr);
                    Log.d(TAG, songsJson.toString());
                    // looping through All Contacts
                    for (int i = 0; i < songsJson.length(); i++) {
                        JSONObject c = songsJson.getJSONObject(i);
                        SongModel song = new SongModel();
                        try {
                            song.setName(c.getString("song"));
                        } catch (JSONException e) {
                            song.setName("Song Name");
                        }
                        try {
                            song.setUrl(c.getString("url"));
                            if(!song.getUrl().startsWith("http"))
                                continue;
                        } catch (JSONException e) {
                            continue;
                        }
                        try {
                            song.setIcon(c.getString("cover_image"));
                        } catch (JSONException e) {
                            song.setIcon("http://hck.re/U1bRnt");
                        }
                        song.setArtists(new ArrayList<>(Arrays.asList(c.getString("artists").split(","))));
                        allSongs.add(song);
                    }
                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Error Occured",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            done = true;
            swipeRefresh.setRefreshing(false);
            updateDownOrInPlaylist();
            dataSetChanged();
            if(defaultSongs.size()==0)
                defaultSongs.addAll(allSongs);
        }
    }

    //Function to update downloadURL for each song (if any) and isInPlaylist field if it is in playlist.
    private void updateDownOrInPlaylist() {
        PlaylistSQLDB playlistSQLDB = new PlaylistSQLDB(MainSongsActivity.this);
        playlistSQLDB.open();
        for(int i=0;i<allSongs.size(); i++)
            allSongs.get(i).setInPlaylist(playlistSQLDB.isInPlaylist(allSongs.get(i).getUrl()));
        ArrayList<SongModel> downloadedSongs = playlistSQLDB.getData(PlaylistSQLDB.TypeOfData.DOWNLOADED);
        Map<String, SongModel> mapDownloadedSongs = new HashMap<>();

        //make a map of songs for easy search in next step.
        for(SongModel song: downloadedSongs){
            mapDownloadedSongs.put(song.getUrl(), song);
        }

        //Check if the song is downloaded already.
        for(SongModel song: allSongs){
            if(mapDownloadedSongs.containsKey(song.getUrl()))
                song.setDownloadedLoc(mapDownloadedSongs.get(song.getUrl()).getDownloadedLoc());
        }

        playlistSQLDB.close();
    }

    private void dataSetChanged() {
        max_pages = (allSongs.size() - 1) / getListSize() + 1;
        inflateCircles(max_pages);
    }

    // This is used to display the circles for transition to multiple pages.
    private void inflateCircles(int count) {
        //llTransition.setVisibility(View.VISIBLE);
        llTransition.removeAllViews();
        ivTransitions.clear();

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int) getResources().getDimension(R.dimen.width_trans), (int) getResources().getDimension(R.dimen.size_trans));
/*
        This code segment is for future integration when the number of pages exceeds to a very high value.

        ImageView ivLeft = new ImageView(this);
        ivLeft.setLayoutParams(layoutParams);
        ivLeft.setImageResource(R.drawable.left);

        ImageView ivRight = new ImageView(this);
        ivRight.setLayoutParams(layoutParams);
        ivRight.setImageResource(R.drawable.right);
*/
        curPage = 0;

        for (int i = 0; i < count; i++) {
            ImageView ivTrans = new ImageView(this);
            ivTrans.setLayoutParams(layoutParams);
            ivTrans.setPadding((int) getResources().getDimension(R.dimen.margin_trans), 0, (int) getResources().getDimension(R.dimen.margin_trans), 0);
            if (i == curPage)
                ivTrans.setImageResource(R.drawable.dark_circle);
            else
                ivTrans.setImageResource(R.drawable.circle);

            ivTransitions.add(ivTrans);
            llTransition.addView(ivTrans);
        }

        //This is used to set the list according to the cur page
        updateList();
        for (int i = 0; i < count; i++) {
            final int finalI = i;
            ivTransitions.get(i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    //Old page number is lightened and the new one is darkened.
                    ivTransitions.get(curPage).setImageResource(R.drawable.circle);
                    ivTransitions.get(finalI).setImageResource(R.drawable.dark_circle);
                    curPage = finalI;

                    //This is used to set the list according to the cur page
                    updateList();
                }
            });
        }
    }

    //This is to re initialise the transition circles without creating new views.
    void reInitTrans() {
        for (ImageView iv : ivTransitions)
            iv.setImageResource(R.drawable.circle);
        ivTransitions.get(curPage).setImageResource(R.drawable.dark_circle);
        updateList();
    }

    private int getListSize() {
        return recyclerView.getHeight()/(int)getResources().getDimension(R.dimen.list_item_height);
    }

    //This is to update the list according to the current page number.
    private void updateList() {
        int listSize = getListSize();
        int end = min(allSongs.size(), (curPage + 1) * listSize);
        toBeDisplayed.clear();
        adapter.notifyDataSetChanged();
        for (int i = listSize * curPage; i < end; i++) {
            toBeDisplayed.add(allSongs.get(i));
        }
        adapter.notifyDataSetChanged();
    }
}
