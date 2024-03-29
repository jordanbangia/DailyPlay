package com.daily.play.utils;

import android.content.Context;

import com.daily.play.managers.DailyPlayMusicManager;

/**
 * Created by Jordan on 7/12/2014.
 */
public class DailyPlaySharedPrefUtils {
    public static final String INIT = "DailyPlay";

    public static final String LAST_SONG_LIST_SYNC = "last_sync";
    public static final String SONG_LIST = "song_list";
    public static final String DOWNLOADED_SONG_LIST = "downloaded_song_list";
    public static final String DOWNLOAD_OPTION = "download_option";
    public static final String NUMBER_OF_SONGS_TO_DOWNLOAD = "number_of_songs_to_download";
    public static final String TIME_OF_SONGS_TO_DOWNLOAD = "time_of_songs_to_download";
    public static final String SHOW_NOTIFICATIONS = "show_notifications";
    public static final String KEEP_PLAYLIST = "keep_playlist";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String SALT = "salt";
    public static final String FIRST_OPEN = "first_open";
    public static final String TOKEN = "token";

    private DailyPlaySharedPrefUtils() {
    }

    public static void init(Context context) {
        SharedPref.initSharedPref(context, INIT);
    }

    public static void saveDownloadOption(int downloadOption) {
        SharedPref.setInt(DOWNLOAD_OPTION, downloadOption);
    }

    public static int getDownloadOption() {
        return SharedPref.getInt(DOWNLOAD_OPTION, DailyPlayMusicManager.DownloadOptions.SONGS);
    }

    public static void saveLengthOfPlayList(String playListLength, int downloadOption) {
        if (StringUtils.isEmptyString(playListLength)) {
            return;
        }

        switch (downloadOption) {
            case DailyPlayMusicManager.DownloadOptions.SONGS:
                SharedPref.setInt(NUMBER_OF_SONGS_TO_DOWNLOAD, Integer.parseInt(playListLength));
                break;
            case DailyPlayMusicManager.DownloadOptions.TIME:
                SharedPref.setInt(TIME_OF_SONGS_TO_DOWNLOAD, Integer.parseInt(playListLength));
                break;
        }
    }

    public static int getLengthOfPlayList() {
        int downloadOption = getDownloadOption();
        return getLengthOfPlayListForDownloadOption(downloadOption);
    }

    public static int getLengthOfPlayListForDownloadOption(int option) {
        switch (option) {
            case DailyPlayMusicManager.DownloadOptions.TIME:
                return SharedPref.getInt(TIME_OF_SONGS_TO_DOWNLOAD, DailyPlayMusicManager.DEF_TIME_OF_PLAY_LIST);
            case DailyPlayMusicManager.DownloadOptions.SONGS:
            default:
                return SharedPref.getInt(NUMBER_OF_SONGS_TO_DOWNLOAD, DailyPlayMusicManager.DEF_NUMBER_OF_SONGS);
        }
    }

    public static void saveShowNotifications(boolean showNotifications) {
        SharedPref.setBoolean(SHOW_NOTIFICATIONS, showNotifications);
    }

    public static void saveKeepPlayList(boolean keepPlayList) {
        SharedPref.setBoolean(KEEP_PLAYLIST, keepPlayList);
    }

    public static String getDownloadedSongList() {
        return SharedPref.getString(DailyPlaySharedPrefUtils.DOWNLOADED_SONG_LIST);
    }

    public static void setDownloadedSongList(String downloadedSongList) {
        SharedPref.setString(DailyPlaySharedPrefUtils.DOWNLOADED_SONG_LIST, downloadedSongList);
    }

    public static void setLastSongListSyncTime() {
        SharedPref.setLong(LAST_SONG_LIST_SYNC, System.currentTimeMillis());
    }

    public static long getLastSongListSyncTime() {
        return SharedPref.getLong(LAST_SONG_LIST_SYNC, 0);
    }

    public static void setSongList(String songList) {
        SharedPref.setString(SONG_LIST, songList);
    }

    public static String getSongList() {
        return SharedPref.getString(SONG_LIST);
    }

    public static boolean shouldShowNotifications() {
        return SharedPref.getBoolean(SHOW_NOTIFICATIONS, true);
    }

    public static boolean shouldKeepPlaylist() {
        return SharedPref.getBoolean(KEEP_PLAYLIST, false);
    }

    public static boolean doesUserInformationExist() {
        String user = SharedPref.getString(USERNAME);
        String pass = SharedPref.getString(PASSWORD);

        if (StringUtils.isEmptyString(user) || StringUtils.isEmptyString(pass)) {
            return false;
        }
        return true;
    }

    public static void saveLengthOfPlayListTime(String playListLengthTime) {
        saveLengthOfPlayList(playListLengthTime, DailyPlayMusicManager.DownloadOptions.TIME);
    }

    public static void saveLengthOfPlayListNumber(String playListLengthNumber) {
        saveLengthOfPlayList(playListLengthNumber, DailyPlayMusicManager.DownloadOptions.SONGS);
    }

    public static void setLoginInformation(String password, String username) {
        try {
            AesCbcWithIntegrity.SecretKeys key;
            String salt = AesCbcWithIntegrity.saltString(AesCbcWithIntegrity.generateSalt());
            key = AesCbcWithIntegrity.generateKeyFromPassword(username, salt);
            AesCbcWithIntegrity.CipherTextIvMac civ = AesCbcWithIntegrity.encrypt(password, key);
            SharedPref.setString(DailyPlaySharedPrefUtils.PASSWORD, civ.toString());
            SharedPref.setString(DailyPlaySharedPrefUtils.SALT, salt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        SharedPref.setString(DailyPlaySharedPrefUtils.USERNAME, username);
    }

    public static String getUsername() {
        return SharedPref.getString(DailyPlaySharedPrefUtils.USERNAME);
    }

    public static String getPassword() {
        String password = SharedPref.getString(DailyPlaySharedPrefUtils.PASSWORD);
        String username = getUsername();
        String salt = SharedPref.getString(DailyPlaySharedPrefUtils.SALT);
        try {
            AesCbcWithIntegrity.SecretKeys key = AesCbcWithIntegrity.generateKeyFromPassword(username, salt);
            return AesCbcWithIntegrity.decryptString(new AesCbcWithIntegrity.CipherTextIvMac(password), key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean isFirstOpen() {
        return SharedPref.getBoolean(FIRST_OPEN, true);
    }

    public static void setIsFirstOpen() {
        SharedPref.setBoolean(FIRST_OPEN, false);
    }

    public static void saveToken(String token) {
        SharedPref.setString(TOKEN, token);
    }

    public static String getToken() {
        return SharedPref.getString(TOKEN, "");
    }
}
