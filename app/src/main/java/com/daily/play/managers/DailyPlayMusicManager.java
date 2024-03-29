package com.daily.play.managers;

import android.content.ContentResolver;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.util.Log;

import com.daily.play.api.DailyPlayMusicApi;
import com.daily.play.api.MusicApi;
import com.daily.play.api.models.Track;
import com.daily.play.exceptions.NoSpaceException;
import com.daily.play.exceptions.NoWifiException;
import com.daily.play.utils.ConnectionUtils;
import com.daily.play.utils.DailyPlaySharedPrefUtils;
import com.daily.play.utils.StringUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

/**
 * Created by Jordan on 6/3/2014.
 * All functions that make network calls assume that they are being run on a background thread.
 * In general, object should be made not on the UI thread, and continued to be used not on the UI thread,
 * as there are accesses to SharePref/network calls/long loops that can stall.
 */
public class DailyPlayMusicManager {
    public static final int DEF_NUMBER_OF_SONGS = 10;
    public static final int DEF_TIME_OF_PLAY_LIST = 10;

    private static final long ONE_WEEK = DateUtils.WEEK_IN_MILLIS;
    private static final long MEGABYTE = 1024L;
    private static final long TEN_MEGABYTES = 10*MEGABYTE;

    private static DailyPlayMusicManager mInstance;
    private MusicApi mApi;
    private ArrayList<Track> mSongList;
    private ArrayList<Track> mDownloadedFiles;

    private int mSongCount = 0;

    public static class DownloadOptions {
        public static final int SONGS = 0;
        public static final int TIME = 1;
    }

    public static DailyPlayMusicManager getInstance() {
        if (mInstance == null) {
            mInstance = new DailyPlayMusicManager();
        }
        return mInstance;
    }

    private DailyPlayMusicManager() {
        mApi = new DailyPlayMusicApi();
    }

    public void getDailyPlayMusic(Context context) throws Exception {
        if (!ConnectionUtils.isConnectedWifi(context)) {
            throw(new NoWifiException());
        }

        loadSongList();
        Collection<Track> downloadList = getSongList();

        if (!isFreeSpace(downloadList.size())) {
            throw(new NoSpaceException());
        }

        getDownloadedSongs();
        deleteOldDailyPlayList(context);
        Log.i("DailyPlay", "Starting to download songs");
        Collection<Track> songs = mApi.downloadSongs(downloadList, context);
        mDownloadedFiles.addAll(songs);
        Log.i("DailyPlay", "Songs downloaded");
        saveDailyPlayList();
        scanMediaFiles(mDownloadedFiles, context);
        Log.i("DailPlay", "Done downloading list");
    }

    private ArrayList<Track> getSongListForCount(int numberOfSongs) {
        if (numberOfSongs >= mSongList.size()) {
            return mSongList;
        }

        ArrayList<Track> songs = new ArrayList<Track>();
        for (int i = 0; i < numberOfSongs; i++) {
            Random rand = new Random();
            int nextSongIndex = rand.nextInt(mSongCount);
            while(songs.contains(mSongList.get(nextSongIndex))) {
                nextSongIndex = rand.nextInt(mSongCount);
            }
            songs.add(mSongList.get(nextSongIndex));
        }
        return songs;
    }

    private Collection<Track> getSongListForTime(int timeOfPlayListMin) {
        ArrayList<Track> songsToDownload = new ArrayList<Track>();
        while (timeOfPlayListMin > 0 && songsToDownload.size() != mSongList.size()) {
            Random rand = new Random();
            Track songToBeAdded = mSongList.get(rand.nextInt(mSongCount));
            while (songsToDownload.contains(songToBeAdded)) {
                songToBeAdded = mSongList.get(rand.nextInt(mSongCount));
            }
            songsToDownload.add(songToBeAdded);
            timeOfPlayListMin -= (songToBeAdded.getDurationMillis()/(60*1000));
        }
        return songsToDownload;
    }

    /**
     * Adds the newly downloaded music files to the systems, so that music player apps know about the songs.
     * @param downloadFiles The files to be scanned.
     * @param context Context is necessary to get the media scanner.
     */
    private void scanMediaFiles(final Collection<Track> downloadFiles, Context context) {
        ArrayList<String> pathsToAdd = new ArrayList<String>(downloadFiles.size());
        for (Track songFile : downloadFiles) {
            File file = songFile.getFile();
            if (file != null) {
                pathsToAdd.add(file.getAbsolutePath());
            }
        }

        String[] pathsToAddStringArray = pathsToAdd.toArray(new String[pathsToAdd.size()]);

        MediaScannerConnection.scanFile(context, pathsToAddStringArray, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String path, Uri uri) {
                Log.i("DailyPlay", "Scanned file added: " + path);
            }
        });
    }

    /**
     * Deletes the old DailyPlay list from the device.  Uses the loaded list from mDownloadedFiles (which should contain the current list of downloaded files, prior to downloading a new list.
     * It first checks the mDownloadedFiles is not null, to make sure that the list has been loaded from memory.  Then it checks the parameter of whether the list should actually be deleted or not,
     * based on user preferences.  Finally, it goes through and deletes the files, then updates the Audio Media Store content resolver with the fact that the files are deleted, and sets the downloaded
     * song list to empty.
     * @param context Required for using the content resolver.
     */
    public void deleteOldDailyPlayList(Context context) {
        if (DailyPlaySharedPrefUtils.shouldKeepPlaylist()) {
            Log.i("DailyPlay", "tried to delete files, but option was checked to keep them");
            return;
        }

        if (mDownloadedFiles == null) {
            Log.i("DailyPlay", "tried to delete files but the downloaded list was null");
            getDownloadedSongs();
            if (mDownloadedFiles == null) {
                return;
            }
        }

        for(Track downloadedFile : mDownloadedFiles) {
            File file = downloadedFile.getFile();
            if (file.delete()) {
                Log.i("DailyPlay",  "Deleted File: " + file.getName());
            } else {
                Log.i("DailyPlay", "File not deleted: " + file.getName());
            };
            ContentResolver resolver = context.getContentResolver();
            resolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaStore.Audio.Media.DATA + "=?", new String[]{file.getAbsolutePath()});
        }
        mDownloadedFiles.clear();
        DailyPlaySharedPrefUtils.setDownloadedSongList("");
    }

    public Collection<Track> getDownloadedSongs() {
        if (mDownloadedFiles == null) {
            loadCurrentlyDownloadedList();
        }
        return mDownloadedFiles;
    }

    private Collection<Track> getSongList() {
        int downloadOption = DailyPlaySharedPrefUtils.getDownloadOption();
        Collection<Track> songs = null;
        int playListLength = DailyPlaySharedPrefUtils.getLengthOfPlayList();
        switch(downloadOption) {
            case DownloadOptions.SONGS:
                songs = getSongListForCount(playListLength);
                break;
            case DownloadOptions.TIME:
                songs = getSongListForTime(playListLength);
                break;
            case -1:
                throw(new IllegalArgumentException());
        }
        return songs;
    }

    private void loadSongList() throws IOException, URISyntaxException {
        if (mSongList == null || mSongList.size() == 0) {
            Log.i("DailyPlay", "Downloading song list");
            if (songListIsOutDated()) {
                downloadSongList();
            } else {
                loadSongListFromSharedPref();
            }
            Log.i("DailyPlay", "Downloaded song list");
        }

    }

    private void loadCurrentlyDownloadedList() {
        String oldDailyPlayList = DailyPlaySharedPrefUtils.getDownloadedSongList();
        if (StringUtils.isEmptyString(oldDailyPlayList)) {
            mDownloadedFiles = new ArrayList<Track>();
            return;
        }
        Gson gson = new Gson();
        Type type = new TypeToken<Collection<Track>>() {}.getType();
        mDownloadedFiles = gson.fromJson(oldDailyPlayList, type);
    }

    private void downloadSongList() throws IOException, URISyntaxException {
        mSongList = mApi.getAllSongs();
        mSongCount = mSongList.size();
        DailyPlaySharedPrefUtils.setLastSongListSyncTime();
        Gson gson = new Gson();
        DailyPlaySharedPrefUtils.setSongList(gson.toJson(mSongList));
    }

    public void login() {
        mApi.login();
    }

    public void login(String token) {
        mApi.login(token);
    }

    public void logout() {
        mApi.logout();
    }

    private void loadSongListFromSharedPref() throws IOException, URISyntaxException {
        String songListAsString = DailyPlaySharedPrefUtils.getSongList();
        if (StringUtils.isEmptyString(songListAsString)) {
            downloadSongList();
        } else {
            Gson gson = new Gson();
            Type type = new TypeToken<Collection<Track>>(){}.getType();
            mSongList = gson.fromJson(songListAsString, type);
            mSongCount = mSongList.size();
        }
    }

    private boolean songListIsOutDated() {
        long currentTime = System.currentTimeMillis();
        long lastSync = DailyPlaySharedPrefUtils.getLastSongListSyncTime();
        return (currentTime - lastSync) > ONE_WEEK;
    }

    private boolean isFreeSpace(int numberOfSongs) {
        File musicSaveFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        long freeSpace = musicSaveFolder.getFreeSpace();
        long requiredSpace = numberOfSongs * TEN_MEGABYTES;
        return freeSpace > requiredSpace;
    }

    private void saveDailyPlayList() {
        if (mDownloadedFiles == null) {
            return;
        }

        Gson gson = new Gson();
        DailyPlaySharedPrefUtils.setDownloadedSongList(gson.toJson(mDownloadedFiles));
    }
}
