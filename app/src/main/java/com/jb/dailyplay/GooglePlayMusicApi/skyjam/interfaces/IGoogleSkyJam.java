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
package com.jb.dailyplay.GooglePlayMusicApi.skyjam.interfaces;

import android.content.Context;

import com.jb.dailyplay.GooglePlayMusicApi.interfaces.IGoogleMusicAPI;
import com.jb.dailyplay.GooglePlayMusicApi.skyjam.model.Playlists;
import com.jb.dailyplay.GooglePlayMusicApi.skyjam.model.Track;
import com.jb.dailyplay.GooglePlayMusicApi.skyjam.model.TrackFeed;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

import org.apache.http.client.ClientProtocolException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

public interface IGoogleSkyJam extends IGoogleMusicAPI
{

	final String HTTPS_WWW_GOOGLEAPIS_COM_SJ_V1BETA1_TRACKS = "https://www.googleapis.com/sj/v1beta1/tracks";
	final String HTTPS_WWW_GOOGLEAPIS_COM_SJ_V1BETA1_TRACKFEED = "https://www.googleapis.com/sj/v1beta1/trackfeed";

	Collection<Track> getAllTracks() throws ClientProtocolException, IOException, URISyntaxException;

	Collection<com.jb.dailyplay.models.SongFile> downloadTracks(Collection<Track> tracks, Context context) throws URISyntaxException, ClientProtocolException, IOException, InvalidDataException, NotSupportedException, UnsupportedTagException;

	com.jb.dailyplay.models.SongFile downloadTrack(Track track, Context context) throws URISyntaxException, ClientProtocolException, IOException, InvalidDataException, NotSupportedException, UnsupportedTagException;

	Playlists getAllSkyJamPlaylists() throws ClientProtocolException, IOException, URISyntaxException;

	TrackFeed getSkyJamPlaylist(String plID) throws ClientProtocolException, IOException, URISyntaxException;

	URI getTrackURL(Track track) throws URISyntaxException, ClientProtocolException, IOException;
}
