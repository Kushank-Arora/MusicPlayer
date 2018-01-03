package com.kushank.olaplaybykushank;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;


/*
* This activity is for playing the song.
 */
public class SongPlayer extends AppCompatActivity {

    public static final String SONG_NAME = "song_name";
    public static final String SONG_ARTIST = "song_artist";
    public static final String SONG_URL = "song_url";
    public static final String SONG_DOWN_URL = "song_down_url";
    private SimpleExoPlayerView playerView;
    private SimpleExoPlayer player;
    private long playbackPosition;
    private int currentWindow;
    private boolean playWhenReady;
    private SongModel song;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_player);

        Bundle data = getIntent().getExtras();
        assert data != null;

        //Data is retrieved using intent.
        song = new SongModel();
        song.setName(data.getString(SONG_NAME));
        song.setUrl(data.getString(SONG_URL));
        song.setArtists(data.getStringArrayList(SONG_ARTIST));
        song.setDownloadedLoc(data.getString(SONG_DOWN_URL));

        playerView = findViewById(R.id.video_view);

        TextView tvName = findViewById(R.id.tv_name);
        //Name of yhe song is displayed.
        tvName.setText(song.getName());

        TextView tvArtists = findViewById(R.id.tv_artists);

        //the name of artists are concatenated.
        StringBuilder artists = new StringBuilder();
        if(song.getArtists().size()>0)
            artists.append(song.getArtists().get(0));
        for(int i=1; i<song.getArtists().size(); i++)
            artists.append(", ").append(song.getArtists().get(i));
        tvArtists.setText(artists.toString());

        //This is done to play the song as the start.
        playWhenReady = true;

        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer();
        }
    }

    //The player is initialised for ExoPlayer API
    private void initializePlayer() {

        player = ExoPlayerFactory.newSimpleInstance(
                new DefaultRenderersFactory(this),
                new DefaultTrackSelector(), new DefaultLoadControl());


        playerView.setPlayer(player);

        playerView.setControllerHideOnTouch(false);
        playerView.setControllerShowTimeoutMs(Integer.MAX_VALUE);

        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);

        //The url of the song is provided.
        Uri uri;

        //if the song downloadLoc is present but the file does'nt exist
        if(song.getDownloadedLoc()!=null && !(new File(song.getDownloadedLoc()).exists()))
        {
            //remove the download url.
            song.setDownloadedLoc(null);

            //modify the tables accordingly.
            PlaylistSQLDB playlistSQLDB = new PlaylistSQLDB(this);
            playlistSQLDB.open();
            playlistSQLDB.updateDownURL(song.getUrl(), null);
            playlistSQLDB.close();
        }

        //if the download URL is not present
        if(song.getDownloadedLoc()==null)
            //Get the Internet downloading URL
            uri = Uri.parse(song.getUrl());
        else
            //Get the Local file URL
            uri = Uri.parse(song.getDownloadedLoc());

        MediaSource mediaSource = buildMediaSource(uri);
        player.prepare(mediaSource, true, false);

        player.addListener(new ExoPlayer.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {

            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

            }

            @Override
            public void onLoadingChanged(boolean isLoading) {

            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

            }

            @Override
            public void onPlayerError(ExoPlaybackException p0) {
                switch (p0.type)
                {
                    case ExoPlaybackException.TYPE_SOURCE:
                        Toast.makeText(SongPlayer.this, "The Song does'nt exist!", Toast.LENGTH_LONG).show();
                        Log.e("SongPlayer", p0.getSourceException().toString());
                        finish();
                        break;

                    default:
                        Toast.makeText(SongPlayer.this, "The Song does'nt exist!", Toast.LENGTH_LONG).show();
                        Log.e("SongPlayer", p0.toString());
                        finish();
                        break;
                }
            }

            @Override
            public void onPositionDiscontinuity() {

            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

            }
        });
    }
    private MediaSource buildMediaSource(Uri uri) {
        return new ExtractorMediaSource(uri,
                createDataSourceFactory(this),
                new DefaultExtractorsFactory(), null, null);
    }

    //This is done to adapt for redirection from http to https
    public static DefaultDataSourceFactory createDataSourceFactory(Context context) {

        // Default parameters, except allowCrossProtocolRedirects is true
        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(
                "ua",
                null,
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                true /* allowCrossProtocolRedirects */
        );

        return new DefaultDataSourceFactory(
                context,
                null,
                httpDataSourceFactory
        );
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }

    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void releasePlayer() {
        if (player != null) {
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            playWhenReady = player.getPlayWhenReady();
            player.release();
            player = null;
        }
    }
}
