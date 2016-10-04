/**
 * 
 */
package com.ayansh.hanudroid;

import android.net.Uri;

import com.ayansh.CommandExecuter.Command;
import com.ayansh.CommandExecuter.Invoker;
import com.ayansh.CommandExecuter.ResultObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;


/**
 * @author Varun Verma
 *
 */
public class UpdatePostMetaCommand extends Command {

	/**
	 * @param caller
	 */
	public UpdatePostMetaCommand(Invoker caller) {
		super(caller);
	}

	/* (non-Javadoc)
	 * @see org.varunverma.CommandExecuter.Command#execute(org.varunverma.CommandExecuter.ResultObject)
	 */
	@Override
	protected void execute(ResultObject result) throws Exception {
		// Fetch the Post Meta and update
		
		String postIds = ApplicationDB.getInstance().getPostIds();
		
		String baseUrl = Application.getApplicationInstance().blogURL;
		String postURL = baseUrl + "/wp-content/plugins/hanu-droid/FetchPostMeta.php";

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

			// Get Input Stream Reader.

			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}

			JSONObject postMetaResponse = new JSONObject(builder.toString());
			JSONArray postMetaData = postMetaResponse.getJSONArray("PostMetaData");

			String query;

			ArrayList<String> queries = new ArrayList<String>();

			for (int i = 0; i < postMetaData.length(); i++){

				queries.clear();

				JSONObject metaData = postMetaData.getJSONObject(i);

				String postId = metaData.getString("PostId");
				String metaKey = metaData.getString("MetaKey");
				String metaValue = metaData.getString("MetaValue");

				//Update Users Rated
				query = "UPDATE PostMeta SET MetaValue='" + metaValue + "' WHERE PostId='" + postId + "' AND MetaKey='" + metaKey + "'";
				queries.add(query);

				ApplicationDB.getInstance().executeQueries(queries);

			}
		}
		finally {
			urlConnection.disconnect();
		}

	}
}