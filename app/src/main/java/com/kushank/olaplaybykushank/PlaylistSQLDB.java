package com.kushank.olaplaybykushank;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import static java.util.Collections.reverse;

/*
* Class to manage all SQL Table interactions.
* */

class PlaylistSQLDB {
    private static final String KEY_NAME = "song_name";
    private static final String KEY_URL = "song_url";
    private static final String KEY_ARTISTS = "song_artists";
    private static final String KEY_ICON = "song_icon";
    private static final String KEY_TIME = "time_added";
    private static final String KEY_DOWN_URL = "song_down_url";
    private static final String KEY_IN_PLAYLIST = "song_in_playlist";
    private static final String KEY_LAST_PLAYED = "song_last_played";

    private static final String DATABASE_NAME = "PlaylistSQLDB";
    private static final String DATABASE_TABLE = "playlist";
    private static final int DATABASE_VERSION = 1;

    private DbHelper ourHelper;
    private final Context ourContext;
    private SQLiteDatabase ourDatabase;

    private class DbHelper extends SQLiteOpenHelper {

        DbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + DATABASE_TABLE + " (" + KEY_TIME + " INTEGER, " +
                    KEY_NAME + " TEXT, " + KEY_URL + " TEXT PRIMARY KEY, " + KEY_ARTISTS + " TEXT, " + KEY_LAST_PLAYED + " INTEGER, " + KEY_IN_PLAYLIST + " INTEGER, " + KEY_DOWN_URL + " TEXT, " + KEY_ICON + " TEXT);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            onCreate(db);
        }
    }

    PlaylistSQLDB(Context c) {
        ourContext = c;
    }

    void open() {
        ourHelper = new DbHelper(ourContext);
        ourDatabase = ourHelper.getWritableDatabase();
    }

    void close() {
        ourHelper.close();
    }


    //Entry is created for the table if the song is not present in the DB.
    void createEntry(SongModel song) {
        if (isPresentInDB(song.getUrl()))
            return;
        ContentValues cv = new ContentValues();
        cv.put(KEY_NAME, song.getName());
        cv.put(KEY_URL, song.getUrl());
        cv.put(KEY_ARTISTS, util.arrayListToString(song.getArtists()));
        cv.put(KEY_ICON, song.getIcon());
        cv.put(KEY_TIME, (int) (Calendar.getInstance().getTimeInMillis() / 1000));
        cv.put(KEY_LAST_PLAYED, 0);
        ourDatabase.insert(DATABASE_TABLE, null, cv);
    }


    //Data is retrieved based on the request.
    ArrayList<SongModel> getData(TypeOfData typeOfData) {
        String[] columns = new String[]{KEY_NAME, KEY_URL, KEY_ARTISTS, KEY_ICON, KEY_TIME, KEY_DOWN_URL, KEY_IN_PLAYLIST};
        Cursor c = null;

        switch (typeOfData) {
            case DOWNLOADED:
                c = ourDatabase.query(DATABASE_TABLE, columns, KEY_DOWN_URL + " IS NOT NULL", null, null, null, KEY_TIME);
                break;
            case IN_PLAYLIST:
                c = ourDatabase.query(DATABASE_TABLE, columns, KEY_IN_PLAYLIST + "=1", null, null, null, KEY_TIME);
                break;
            case RECENT:
                c = ourDatabase.query(DATABASE_TABLE, columns, KEY_LAST_PLAYED + "!=0", null, null, null, KEY_LAST_PLAYED);
                break;
        }

        int iName = c.getColumnIndex(KEY_NAME);
        int iURL = c.getColumnIndex(KEY_URL);
        int iArtists = c.getColumnIndex(KEY_ARTISTS);
        int iIcon = c.getColumnIndex(KEY_ICON);
        int iDownURL = c.getColumnIndex(KEY_DOWN_URL);
        int iInPlaylist = c.getColumnIndex(KEY_IN_PLAYLIST);

        ArrayList<SongModel> result = new ArrayList<>();

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            SongModel song = new SongModel();
            song.setName(c.getString(iName));
            song.setUrl(c.getString(iURL));
            song.setIcon(c.getString(iIcon));
            song.setArtists(new ArrayList<>(Arrays.asList(c.getString(iArtists).split(","))));
            song.setInPlaylist(c.getInt(iInPlaylist)==1);
            song.setDownloadedLoc(c.getString(iDownURL));

            result.add(song);
        }

        c.close();

        reverse(result);

        return result;
    }

    //This function returns true if the entry for the given song is present in the DB.
    private boolean isPresentInDB(String url) {
        String[] columns = new String[]{KEY_URL};
        Cursor c = ourDatabase.query(DATABASE_TABLE, columns, KEY_URL + "='" + url + "'", null, null, null, null);
        c.moveToFirst();
        c.close();
        return (!c.isAfterLast());
    }

    //This is used to check if a song is in a playlist.
    boolean isInPlaylist(String url) {
        String[] columns = new String[]{KEY_URL};
        Cursor c = ourDatabase.query(DATABASE_TABLE, columns, KEY_URL + "='" + url + "' AND "+KEY_IN_PLAYLIST+"=1", null, null, null, null);
        c.moveToFirst();
        c.close();
        return (!c.isAfterLast());
    }

    //This is to update the downloadURL for the song.
    //If it is null and the song is not even in playlist or recent songs, then it is deleted.
    void updateDownURL(String url, String downURL) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_DOWN_URL, downURL);
        ourDatabase.update(DATABASE_TABLE, cv, KEY_URL + "='" + url + "'", null);

        //If it is null and the song is not even in playlist or recent songs, then it is deleted.
        if (downURL == null) {
            String[] columns = new String[]{KEY_IN_PLAYLIST, KEY_LAST_PLAYED};
            Cursor c = ourDatabase.query(DATABASE_TABLE, columns, KEY_URL + "='" + url + "'", null, null, null, null);
            if (c != null) {
                c.moveToFirst();

                //If it is null and the song is not even in playlist or recent songs, then it is deleted.
                if (c.getInt(0) == 0 && c.getInt(1) == 0)
                    deleteEntry(url);
                c.close();
            }
        }
    }

    //This is to update the the presence in playlist of a song.
    //If it is removed from playlist and the song is not even downloaded or recent songs, then it is deleted.
    void updateInPlaylist(String url, boolean isInPlaylist) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_IN_PLAYLIST, isInPlaylist ? 1 : 0);
        ourDatabase.update(DATABASE_TABLE, cv, KEY_URL + "='" + url + "'", null);

        //If it is removed from playlist and the song is not even downloaded or recent songs, then it is deleted.
        if (!isInPlaylist) {
            String[] columns = new String[]{KEY_DOWN_URL, KEY_LAST_PLAYED};
            Cursor c = ourDatabase.query(DATABASE_TABLE, columns, KEY_URL + "='" + url + "'", null, null, null, null);
            if (c != null) {
                c.moveToFirst();

                //If it is removed from playlist and the song is not even downloaded or recent songs, then it is deleted.
                if (c.getString(0) == null && c.getInt(1) == 0)
                    deleteEntry(url);
                c.close();
            }
        }
    }

    //The last played time is inserted in for the corresponding song field.
    void updateLastPlayed(String url) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_LAST_PLAYED, (int) (Calendar.getInstance().getTimeInMillis() / 1000));
        ourDatabase.update(DATABASE_TABLE, cv, KEY_URL + "='" + url + "'", null);
    }

    //Entry for deleting the song.
    private void deleteEntry(String url) {
        ourDatabase.delete(DATABASE_TABLE, KEY_URL + "='" + url + "'", null);
    }

    //This class is used to pass messages between Activities to get required data
    enum TypeOfData {
        DOWNLOADED, IN_PLAYLIST, RECENT
    }
}
