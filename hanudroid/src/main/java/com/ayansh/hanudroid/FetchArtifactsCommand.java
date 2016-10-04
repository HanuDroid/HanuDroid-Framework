package com.ayansh.hanudroid;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.json.JSONObject;

import android.net.Uri;
import android.util.Log;
import android.util.Xml;

import com.ayansh.CommandExecuter.Command;
import com.ayansh.CommandExecuter.Invoker;
import com.ayansh.CommandExecuter.ResultObject;

public class FetchArtifactsCommand extends Command {

	FetchArtifactsCommand(Invoker caller) {
		super(caller);
	}

	@Override
	protected void execute(ResultObject result) throws Exception {
		// Fetch the post artifacts.
		
		Application app = Application.getApplicationInstance();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		String modTime = Application.getApplicationInstance().getOptions().get("LastSyncTime");
		
		if(modTime == null || modTime.contentEquals("")){
			modTime = "1349328720";	// HANU Epoch
		}
		
		Date date = new Date(Long.valueOf(modTime));
		
		// If last sync was very recent, no need to check again.
		if( ( (new Date()).getTime() - date.getTime() ) < 5*60*1000 ){
			throw new Exception("Last sync done less than 5 min ago...");
			// Last sync was only 15 min ago.
		}
		
		Log.v(Application.TAG, "Trying to fetch post artifacts.");
		
		String lastSyncTime = df.format(date);
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		lastSyncTime = df.format(date);

		ArtifactsXMLParser xml_parser = new ArtifactsXMLParser();

		String baseUrl = Application.getApplicationInstance().blogURL;
		String postURL = baseUrl + "/wp-content/plugins/hanu-droid/PostArtifacts.php";

		URL url = new URL(postURL);
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

		JSONObject syncParams = new JSONObject();

		String syncCats = app.getOptions().get("SyncCategory");
		String syncTags = app.getOptions().get("SyncTags");

		if(syncCats != null && !syncCats.contentEquals("")){
			syncParams.put("category", syncCats);
		}

		if(syncTags != null && !syncTags.contentEquals("")){
			syncParams.put("tag", syncTags);
		}

		try{

			urlConnection.setDoOutput(true);
			urlConnection.setChunkedStreamingMode(0);
			urlConnection.setRequestMethod("POST");

			Uri.Builder uriBuilder = new Uri.Builder()
					.appendQueryParameter("modified_time", lastSyncTime)
					.appendQueryParameter("sync_params", syncParams.toString());
			String parameterQuery = uriBuilder.build().getEncodedQuery();

			OutputStream os = urlConnection.getOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
			writer.write(parameterQuery);
			writer.flush();
			writer.close();
			os.close();

			urlConnection.connect();

			// Get Input Stream Reader.

			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			InputStreamReader isr = new InputStreamReader(in);

			// Parse the input XML.
			Xml.parse(isr, xml_parser);

			// Filter Artifacts for download
			Log.v(Application.TAG, "Artifacts fetched, will filter now...");
			PostManager.getInstance().filterArtifactsForDownload();

		}
		finally {
			urlConnection.disconnect();
		}
	}

}