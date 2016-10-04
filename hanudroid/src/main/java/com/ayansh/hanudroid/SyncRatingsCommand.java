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
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;


public class SyncRatingsCommand extends Command {

	SyncRatingsCommand(Invoker caller) {
		super(caller);
	}

	@Override
	protected void execute(ResultObject result) throws Exception {
		
		Post post;
		JSONObject postRating;
		JSONArray postRatings = new JSONArray();
		
		List<Integer> postIds = ApplicationDB.getInstance().getSyncData("PostRating");
		
		if(postIds.isEmpty()){
			return;
		}
		
		Iterator<Integer> iterator = postIds.iterator();
		int postId;
		
		while(iterator.hasNext()){
			
			postId = iterator.next();
			post = PostManager.getInstance().getPostById(postId);
			
			if(post!=null){
				
				postRating = new JSONObject();
				postRating.put("PostId", postId);
				postRating.put("Rating", post.metaData.get("my_rating"));
				postRatings.put(postRating);
			}
			
		}
		
		JSONObject input = new JSONObject();
		input.put("PostRatings", postRatings);
		
		String baseUrl = Application.getApplicationInstance().blogURL;
		String postURL = baseUrl + "/wp-content/plugins/hanu-droid/UpdatePostRating.php";

		URL url = new URL(postURL);
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

		try {

			urlConnection.setDoOutput(true);
			urlConnection.setChunkedStreamingMode(0);
			urlConnection.setRequestMethod("POST");

			Uri.Builder uriBuilder = new Uri.Builder()
					.appendQueryParameter("post_ratings", input.toString());
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

			JSONObject updateRatingsResponse = new JSONObject(builder.toString());
			JSONArray updatedPostRatings = updateRatingsResponse.getJSONArray("PostRatings");

			String query;
			ArrayList<String> queries;

			for (int i = 0; i < updatedPostRatings.length(); i++) {

				queries = new ArrayList<String>();
				JSONObject updatedPostRating = updatedPostRatings.getJSONObject(i);

				String post_Id = updatedPostRating.getString("PostId");
				String ratingsUser = updatedPostRating.getString("ratings_users");
				String ratingsScore = updatedPostRating.getString("ratings_score");
				String ratingsAvg = updatedPostRating.getString("ratings_average");

				//Update Users Rated
				query = "UPDATE PostMeta SET MetaValue='" + ratingsUser + "' WHERE PostId='" + post_Id + "' AND MetaKey='ratings_users'";
				queries.add(query);

				//Update Rating Score
				query = "UPDATE PostMeta SET MetaValue='" + ratingsScore + "' WHERE PostId='" + post_Id + "' AND MetaKey='ratings_score'";
				queries.add(query);

				//Update Average Rating
				query = "UPDATE PostMeta SET MetaValue='" + ratingsAvg + "' WHERE PostId='" + post_Id + "' AND MetaKey='ratings_average'";
				queries.add(query);

				// Delete Sync Data
				query = "DELETE FROM SyncStatus WHERE Type='PostRating' AND SyncId='" + post_Id + "'";
				queries.add(query);

				ApplicationDB.getInstance().executeQueries(queries);

			}
		}
		finally {
			urlConnection.disconnect();
		}
	}
}