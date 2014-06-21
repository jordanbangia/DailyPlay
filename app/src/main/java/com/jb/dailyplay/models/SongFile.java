package com.jb.dailyplay.models;

import com.jb.dailyplay.GooglePlayMusicApi.model.Song;
import com.jb.dailyplay.GooglePlayMusicApi.model.Tune;

import java.io.File;

/**
 * Created by Jordan on 6/7/2014.
 * A wrapper to hold both the file + song for downloaded files.
 */
public class SongFile extends Object {
    private File mFile;
    private Tune mSong;

    public SongFile(File file, Tune song) {
        mFile = file;
        mSong = song;
    }

    public void setFile(File file) {
        mFile = file;
    }

    public File getFile() {
        return mFile;
    }

    public void setSong(Tune song) {
        mSong = song;
    }

    public Tune getSong() {
        return mSong;
    }
}

