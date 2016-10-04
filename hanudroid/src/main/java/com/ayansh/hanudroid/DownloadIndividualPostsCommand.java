package com.ayansh.hanudroid;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import android.net.Uri;
import android.util.Log;
import android.util.Xml;

import com.ayansh.CommandExecuter.Command;
import com.ayansh.CommandExecuter.Invoker;
import com.ayansh.CommandExecuter.ProgressInfo;
import com.ayansh.CommandExecuter.ResultObject;

public class DownloadIndividualPostsCommand extends Command {

	private String postIds;

	DownloadIndividualPostsCommand(Invoker caller, String postIds) {
		super(caller);
		this.postIds = postIds;
	}

	@Override
	protected void execute(ResultObject result) throws Exception {
		
		PostManager pm;
		
		String baseUrl = Application.getApplicationInstance().blogURL;
		String postURL = baseUrl + "/wp-content/plugins/hanu-droid/FetchPosts.php";

		InputStream is;
		InputStreamReader isr;
		PostXMLParser xml_parser;

		pm = PostManager.getInstance();

		xml_parser = new PostXMLParser();

		URL url = new URL(postURL);
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

		try{

			urlConnection.setDoOutput(true);
			urlConnection.setChunkedStreamingMode(0);
			urlConnection.setRequestMethod("POST");

			Uri.Builder uriBuilder = new Uri.Builder()
					.appendQueryParameter("post_id", postIds);
			String parameterQuery = uriBuilder.build().getEncodedQuery();

			OutputStream os = urlConnection.getOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
			writer.write(parameterQuery);
			writer.flush();
			writer.close();
			os.close();

			urlConnection.connect();

			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			isr = new InputStreamReader(in);

			// Parse the input XML.
			Log.v(Application.TAG, "Parsing the post data");
			Xml.parse(isr, xml_parser);

			// Save to DB.
			boolean success = pm.savePostsToDB();

			if (success) {
				result.setResultCode(200);
			}

		}
		finally {
			urlConnection.disconnect();
		}

	}

}