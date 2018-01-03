package com.kushank.olaplaybykushank;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

    private Context context;
    private ArrayList<SongModel> songs;
    private ProgressDialog progressDialog;
    private DownloadFile curTask;
    private boolean isPlaylist;

    CustomAdapter(Context context, ArrayList<SongModel> songs, boolean isPlaylist) {
        this.context = context;
        this.songs = songs;
        this.isPlaylist = isPlaylist;

        //This progress bar is shown when the user tries to download the song.
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Downloading...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        //The downloading should abort on cancel/dismiss of progress bar.
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if (curTask != null)
                    curTask.cancel(true);
            }
        });
        progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (curTask != null)
                    curTask.cancel(true);
            }
        });

        curTask = null;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_song, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final SongModel song = songs.get(position);
        holder.tvName.setText(song.getName());

        //Appending ',' after every artist name.
        holder.tvArtists.setText(util.arrayListToString(song.getArtists()));

        //Using Picasso to display the icon for the song.
        Picasso.Builder builder = new Picasso.Builder(context);
        builder.downloader(new OkHttpDownloader(context));
        builder.build()
                .load(song.getIcon())
                .centerCrop()
                .resize(100, 100)
                .transform(new CircleTransform())
                .into(holder.ivIcon);

        Picasso.with(context).load(song.getIcon()).centerCrop().resize(100, 100).transform(new CircleTransform()).into(holder.ivIcon);

        //The options menu for each song.
        holder.tvOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(context, holder.tvOptions);
                popup.inflate(R.menu.song_options_menu);
                if (song.isInPlaylist())
                    popup.getMenu().getItem(0).setTitle(context.getString(R.string.remove_from_playlist));
                else
                    popup.getMenu().getItem(0).setTitle(context.getString(R.string.add_to_playlist));

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            //When download is pressed
                            case R.id.download:
                                curTask = new DownloadFile(song);
                                curTask.execute();
                                break;

                            //When add/remove to playlist is pressed.
                            case R.id.modify_playlist:
                                PlaylistSQLDB playlistDB = new PlaylistSQLDB(context);
                                playlistDB.open();

                                //if the song is present in the playlist
                                if (song.isInPlaylist()) {

                                    //remove it from playlist
                                    playlistDB.updateInPlaylist(song.getUrl(), false);

                                    Toast.makeText(context, "Song Removed!", Toast.LENGTH_SHORT).show();
                                    song.setInPlaylist(false);

                                    //if the adapter is for the playlist
                                    if (isPlaylist)
                                        songs.remove(song);
                                    notifyDataSetChanged();
                                } else {

                                    //add song in playlist
                                    song.setInPlaylist(true);

                                    //add the entry in db
                                    playlistDB.createEntry(song);
                                    playlistDB.updateInPlaylist(song.getUrl(), true);

                                    Toast.makeText(context, "Song added!", Toast.LENGTH_SHORT).show();
                                    notifyDataSetChanged();
                                }
                                playlistDB.close();
                                break;
                        }
                        return false;
                    }
                });
                popup.show();
            }
        });

        holder.listItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //add the entry in fb for recent songs
                PlaylistSQLDB playlistSQLDB = new PlaylistSQLDB(context);
                playlistSQLDB.open();
                playlistSQLDB.createEntry(song);
                playlistSQLDB.updateLastPlayed(song.getUrl());
                playlistSQLDB.close();

                Intent intent = new Intent(context, SongPlayer.class);
                Bundle bundle = new Bundle();

                //Data is sent to song player activity to play the song.
                bundle.putString(SongPlayer.SONG_NAME, song.getName());
                bundle.putString(SongPlayer.SONG_URL, song.getUrl());
                bundle.putString(SongPlayer.SONG_DOWN_URL, song.getDownloadedLoc());
                bundle.putStringArrayList(SongPlayer.SONG_ARTIST, song.getArtists());
                intent.putExtras(bundle);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout listItem;
        TextView tvName, tvArtists, tvOptions;
        ImageView ivIcon;

        ViewHolder(View itemView) {
            super(itemView);

            listItem = itemView.findViewById(R.id.rl_list_item);
            tvName = itemView.findViewById(R.id.tv_name);
            tvArtists = itemView.findViewById(R.id.tv_artists);
            ivIcon = itemView.findViewById(R.id.iv_logo);
            tvOptions = itemView.findViewById(R.id.tv_options);
        }
    }



    //The AsyncTask class is used to download the song.
    @SuppressLint("StaticFieldLeak")
    private class DownloadFile extends AsyncTask<Void, Integer, Exception> {
        SongModel song;
        String fileName;

        DownloadFile(SongModel song) {
            this.song = song;

            //path name for the file to be downloaded.
            fileName = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
                    + "/" + song.getName() + ".mp3";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setProgress(0);
            progressDialog.show();
        }

        @Override
        protected Exception doInBackground(Void... voids) {
            int count;
            try {
                URL resourceUrl, base, next;
                HttpURLConnection conn;
                String location;

                String url = song.getUrl();

                //This loop is to counter multiple redirection.
                while (true) {
                    resourceUrl = new URL(url);
                    conn = (HttpURLConnection) resourceUrl.openConnection();

                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    conn.setInstanceFollowRedirects(false);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0...");

                    switch (conn.getResponseCode()) {
                        case HttpURLConnection.HTTP_MOVED_PERM:
                        case HttpURLConnection.HTTP_MOVED_TEMP:
                            location = conn.getHeaderField("Location");
                            location = URLDecoder.decode(location, "UTF-8");
                            base = new URL(url);
                            next = new URL(base, location);  // Deal with relative URLs
                            url = next.toExternalForm();
                            continue;
                    }

                    break;
                }

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                int lenghtOfFile = conn.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(resourceUrl.openStream());

                // Output stream
                OutputStream output = new FileOutputStream(fileName);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress((int) ((total * 100) / lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();
            }catch (Exception e) {
                Log.e("CustomAdapted", e.toString());
                return e;
            }

            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            //If the task is aborted, the file should be deleted.
            new File(fileName).delete();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressDialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Exception aVoid) {
            super.onPostExecute(aVoid);
            curTask = null;
            if (progressDialog.getProgress() == 100) {
                Toast.makeText(context, "Download Complete!", Toast.LENGTH_LONG).show();

                //set the download loc for the file.
                song.setDownloadedLoc(fileName);

                //modify the tables in the memory
                PlaylistSQLDB playlistSQLDB = new PlaylistSQLDB(context);
                playlistSQLDB.open();
                playlistSQLDB.createEntry(song);
                playlistSQLDB.updateDownURL(song.getUrl(), song.getDownloadedLoc());
                playlistSQLDB.close();
            } else if(aVoid.getClass()== FileNotFoundException.class){
                Toast.makeText(context, "Sorry! The file is not present at the server!", Toast.LENGTH_LONG).show();
                //If the task is aborted, the file should be deleted.
                new File(fileName).delete();
            } else{
                Toast.makeText(context, "Terminated due to Error!", Toast.LENGTH_LONG).show();
                //If the task is aborted, the file should be deleted.
                new File(fileName).delete();
            }

            progressDialog.dismiss();

            //this is to let the phone know the existence of a new file.
            //It just refreshes the memory
            MediaScannerConnection.scanFile(context,
                    new String[]{fileName},
                    null,
                    null);
        }
    }
}
