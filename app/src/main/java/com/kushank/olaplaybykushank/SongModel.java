package com.kushank.olaplaybykushank;

import java.util.ArrayList;

//This model is used to save the details of a particular song.
public class SongModel {
    private boolean isInPlaylist;
    private String name, icon, url;
    private ArrayList<String> artists;
    private String downloadedLoc;

    SongModel(){
        artists = new ArrayList<>();
    }

    String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    String getUrl() {
        return url;
    }

    void setUrl(String url) {
        this.url = url;
    }

    ArrayList<String> getArtists() {
        return artists;
    }

    void setArtists(ArrayList<String> artists) {
        this.artists = artists;
    }

    public void addArtist(String artist){
        this.artists.add(artist);
    }

    public boolean isInPlaylist() {
        return isInPlaylist;
    }

    public void setInPlaylist(boolean inPlaylist) {
        isInPlaylist = inPlaylist;
    }

    public String getDownloadedLoc() {
        return downloadedLoc;
    }

    public void setDownloadedLoc(String downloadedLoc) {
        this.downloadedLoc = downloadedLoc;
    }
}
