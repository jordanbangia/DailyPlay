/*******************************************************************************
 * Copyright (c) 2012 Jens Kristian Villadsen.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors:
 *     Jens Kristian Villadsen - initial API and implementation
 ******************************************************************************/
package com.daily.play.GooglePlayMusicApi.impl;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.daily.play.GooglePlayMusicApi.comm.FormBuilder;
import com.daily.play.GooglePlayMusicApi.comm.HttpUrlConnector;
import com.daily.play.GooglePlayMusicApi.comm.JSON;
import com.daily.play.GooglePlayMusicApi.interfaces.IGoogleHttpClient;
import com.daily.play.GooglePlayMusicApi.interfaces.IGoogleMusicAPI;
import com.daily.play.GooglePlayMusicApi.interfaces.IJsonDeserializer;
import com.daily.play.GooglePlayMusicApi.model.AddPlaylist;
import com.daily.play.GooglePlayMusicApi.model.DeletePlaylist;
import com.daily.play.GooglePlayMusicApi.model.Playlist;
import com.daily.play.GooglePlayMusicApi.model.Playlists;
import com.daily.play.GooglePlayMusicApi.model.QueryResponse;
import com.daily.play.GooglePlayMusicApi.model.Song;
import com.daily.play.GooglePlayMusicApi.model.SongUrl;
import com.daily.play.GooglePlayMusicApi.model.Tune;
import com.daily.play.models.SongFile;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v1Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleMusicAPI implements IGoogleMusicAPI {
    private static final String MP3 = ".mp3";
    private static final String TEMP = "temp.mp3";
    protected final IGoogleHttpClient client;
    protected final IJsonDeserializer deserializer;
    protected final File storageDirectory;

    public GoogleMusicAPI() {
        this(new HttpUrlConnector(), new JSON(), new File("."));
    }

    public GoogleMusicAPI(final IGoogleHttpClient httpClient, final IJsonDeserializer jsonDeserializer, final File file) {
        client = httpClient;
        deserializer = jsonDeserializer;
        storageDirectory = file;
    }

    @Override
    public final void login(final String email, final String password) throws IOException, URISyntaxException, InvalidCredentialsException {
        final Map<String, String> fields = new HashMap<String, String>();
        fields.put("service", "sj");
        fields.put("Email", email);
        fields.put("Passwd", password);

        final FormBuilder form = new FormBuilder();
        form.addFields(fields);
        form.close();

        try {
            client.dispatchPost(new URI(HTTPS_WWW_GOOGLE_COM_ACCOUNTS_CLIENT_LOGIN), form);
        } catch (final IllegalStateException ise) {
            throw new InvalidCredentialsException(ise, "Provided credentials: '" + email + "' and '" + password + "' where insufficient");
        }
    }

    @Override
    public final ArrayList<Song> getAllSongs() throws IOException, URISyntaxException {
        final ArrayList<Song> chunkedCollection = new ArrayList<Song>();

        // Map<String, String> fields = new HashMap<String, String>();
        // fields.put("json", "{\"continuationToken\":\"" + continuationToken +
        // "\"}");

        final FormBuilder form = new FormBuilder();
        // form.addFields(fields);
        form.close();

        final String response = client.dispatchPost(new URI(HTTPS_PLAY_GOOGLE_COM_MUSIC_SERVICES_LOADALLTRACKS), form);

        final List<String> jsSongCollectionWrappers = getJsSongCollectionWrappers(response);

        final Gson gson = new Gson();
        final JsonParser parser = new JsonParser();

        for (final String songCollectionWrapperJson : jsSongCollectionWrappers) {
            final JsonArray songCollectionWrapper = parser.parse(new StringReader(songCollectionWrapperJson))
                    .getAsJsonArray();

            // the song collection is the first element of the "wrapper"
            final JsonArray songCollection = songCollectionWrapper.get(0)
                    .getAsJsonArray();

            // each element of the songCollection is an array of song values
            for (final JsonElement songValues : songCollection) {
                // retrieve the songValues as an Array for parsing to a Song
                // object
                final JsonArray values = songValues.getAsJsonArray();

                final Song s = new Song();
                s.setId(gson.fromJson(values.get(0), String.class));
                s.setTitle(gson.fromJson(values.get(1), String.class));
                s.setName(gson.fromJson(values.get(1), String.class));
                if (!Strings.isNullOrEmpty(gson.fromJson(values.get(2),
                        String.class))) {
                    s.setAlbumArtUrl("https:"
                            + gson.fromJson(values.get(2), String.class));
                }
                s.setArtist(gson.fromJson(values.get(3), String.class));
                s.setAlbum(gson.fromJson(values.get(4), String.class));
                s.setAlbumArtist(gson.fromJson(values.get(5), String.class));
                s.setGenre(gson.fromJson(values.get(11), String.class));
                s.setDurationMillis(gson.fromJson(values.get(13), Long.class));
                s.setType(gson.fromJson(values.get(16), Integer.class));
                s.setYear(gson.fromJson(values.get(18), Integer.class));
                s.setPlaycount(gson.fromJson(values.get(22), Integer.class));
                s.setRating(gson.fromJson(values.get(23), String.class));

                if (!Strings.isNullOrEmpty(gson.fromJson(values.get(24), String.class))) {
                    s.setCreationDate(gson.fromJson(values.get(24), Float.class) / 1000);
                }
                if (!Strings.isNullOrEmpty(gson.fromJson(values.get(36), String.class))) {
                    s.setUrl("https:" + gson.fromJson(values.get(36), String.class));
                }
                chunkedCollection.add(s);
            }
        }

        return chunkedCollection;
    }

    @Override
    public final AddPlaylist addPlaylist(final String playlistName) throws Exception {
        final Map<String, String> fields = new HashMap<String, String>();
        fields.put("json", "{\"title\":\"" + playlistName + "\"}");

        final FormBuilder form = new FormBuilder();
        form.addFields(fields);
        form.close();

        return deserializer.deserialize(client.dispatchPost(new URI(HTTPS_PLAY_GOOGLE_COM_MUSIC_SERVICES_ADDPLAYLIST), form), AddPlaylist.class);
    }

    @Override
    public final Playlists getAllPlaylists() throws IOException,  URISyntaxException {
        return deserializer.deserialize(getPlaylistAssist("{}"), Playlists.class);
    }

    @Override
    public final Playlist getPlaylist(final String plID) throws IOException, URISyntaxException {
        return deserializer.deserialize(getPlaylistAssist("{\"id\":\"" + plID + "\"}"), Playlist.class);
    }

    protected final URI getTuneURL(final Tune tune) throws URISyntaxException, IOException {
        return new URI(deserializer.deserialize(client.dispatchGet(new URI(String.format(HTTPS_PLAY_GOOGLE_COM_MUSIC_PLAY_SONGID, tune.getId()))), SongUrl.class).getUrl());
    }

    @Override
    public URI getSongURL(final Song song) throws URISyntaxException, IOException {
        return getTuneURL(song);
    }

    @Override
    public final DeletePlaylist deletePlaylist(final String id) throws Exception {
        final Map<String, String> fields = new HashMap<String, String>();
        fields.put("json", "{\"id\":\"" + id + "\"}");

        final FormBuilder form = new FormBuilder();
        form.addFields(fields);
        form.close();

        return deserializer.deserialize(client.dispatchPost(new URI(HTTPS_PLAY_GOOGLE_COM_MUSIC_SERVICES_DELETEPLAYLIST), form), DeletePlaylist.class);
    }

    private final String getPlaylistAssist(final String jsonString) throws IOException, URISyntaxException {
        final Map<String, String> fields = new HashMap<String, String>();
        fields.put("json", jsonString);

        final FormBuilder builder = new FormBuilder();
        builder.addFields(fields);
        builder.close();
        return client.dispatchPost(new URI(HTTPS_PLAY_GOOGLE_COM_MUSIC_SERVICES_LOADPLAYLIST), builder);
    }

    /**
     * Locate each "window.parent['slat_process']" statement and extract the
     * JSON representing a song collection. For a provided response, there may
     * be one or more separate song collections.
     *
     * @param response the HTML response from LOADALLTRACKS
     * @return a "wrapper" JSON object with a list of song collection JSON
     * objects as its first element
     */
    private List<String> getJsSongCollectionWrappers(final String response) {
        final List<String> songCollectionList = new ArrayList<String>();

        // locate the contents of: window.parent['slat_process']( );
        // where the song collection JSON is between the parentheses
        final Pattern p = Pattern.compile("window.parent\\['slat_process'\\]\\((.*?)\\);", Pattern.DOTALL);
        final Matcher m = p.matcher(response);
        while (m.find()) {
            final String songCollectionWrapperJson = m.group(1);
            songCollectionList.add(songCollectionWrapperJson);
        }

        return songCollectionList;
    }

    @Override
    public ArrayList<SongFile> downloadSongs(final Collection<Song> songs, final Context context) throws MalformedURLException, IOException, URISyntaxException, InvalidDataException {
        final ArrayList<SongFile> files = new ArrayList<SongFile>();
        for (final Song song : songs) {
            files.add(downloadSong(song, context));
        }
        return files;
    }

    @Override
    public SongFile downloadSong(final Song song, final Context context) throws MalformedURLException, IOException, URISyntaxException, InvalidDataException {
        return downloadTune(song, context);
    }

    @Override
    public QueryResponse search(final String query) throws IOException, URISyntaxException {
        if (Strings.isNullOrEmpty(query)) {
            throw new IllegalArgumentException("query is null or empty");
        }

        final Map<String, String> fields = new HashMap<String, String>();
        fields.put("json", "{\"q\":\"" + query + "\"}");

        final FormBuilder form = new FormBuilder();
        form.addFields(fields);
        form.close();

        final String response = client.dispatchPost(new URI(HTTPS_PLAY_GOOGLE_COM_MUSIC_SERVICES_SEARCH), form);

        return deserializer.deserialize(response, QueryResponse.class);
    }

    protected SongFile downloadTune(final Tune song, final Context context) throws MalformedURLException, IOException, URISyntaxException, InvalidDataException {
        String fileName = song.getTitle() + MP3;
        String fullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + "/" + fileName;

        File retFile = new File(fullPath);
        if (retFile.exists()) {
            return new SongFile(retFile, (Song) song);
        }

        String fullTempPath = context.getFilesDir().getPath() + "/" + TEMP;
        URL url = getTuneURL(song).toURL();
        URLConnection connection = url.openConnection();
        connection.connect();
        InputStream input = new BufferedInputStream(url.openStream());
        OutputStream output = new FileOutputStream(fullTempPath);
        int count;
        byte data[] = new byte[1024];
        long total = 0;
        while ((count = input.read(data)) != -1) {
            total += count;
            output.write(data, 0, count);
        }
        output.flush();
        output.close();
        input.close();

        Mp3File file = null;
        try {
            file = new Mp3File(fullTempPath);
            if (!file.hasId3v1Tag()) {
                ID3v1 tags = new ID3v1Tag();
                file.setId3v1Tag(tags);
                tags.setArtist(song.getArtist());
                tags.setAlbum(song.getAlbum());
                tags.setTitle(song.getTitle());
                file.save(fullPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.e("DailyPlay - Song Downloaded", fileName);
        return new SongFile(retFile, (Song) song);

    }

    @Override
    public void uploadSong(final File song) {

    }
}