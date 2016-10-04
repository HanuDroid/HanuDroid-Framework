package com.ayansh.hanudroid;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.ListIterator;

import android.net.Uri;
import android.util.Log;
import android.util.Xml;

import com.ayansh.CommandExecuter.Command;
import com.ayansh.CommandExecuter.Invoker;
import com.ayansh.CommandExecuter.ProgressInfo;
import com.ayansh.CommandExecuter.ResultObject;

public class DownloadPostsCommand extends Command {

	private PostManager pm;
	private String artifactList;
	
	DownloadPostsCommand(Invoker caller) {
		super(caller);
	}

	@Override
	protected void execute(ResultObject result) throws Exception {
		
		int counter, successCounter = 0;
		PostArtifact artifact;
		boolean allSuccess = true;
		Application app = Application.getApplicationInstance();
		ArrayList<String> postTitleList = new ArrayList<String>();
		
		String baseUrl = Application.getApplicationInstance().blogURL;
		String postURL = baseUrl + "/wp-content/plugins/hanu-droid/FetchPosts.php";
		
		InputStreamReader isr;
		PostXMLParser xml_parser;
		
		pm = PostManager.getInstance();
		ListIterator<PostArtifact> iterator;
		
		if(pm.postArtifacts.isEmpty()){
			result.getData().putBoolean("ShowNotification", false);
			return;
		}
		
		for(int i=0; i<pm.postArtifacts.size(); i+=5){
			
			Log.v(Application.TAG, "Downloading posts...");
			artifactList = "";
			iterator = pm.postArtifacts.listIterator(i);
			
			counter = 1;
			while(counter <=5 ){
				
				if(!iterator.hasNext()){
					counter++;
					continue;
				}
				
				artifact = iterator.next();
				artifactList = artifactList + String.valueOf(artifact.postId);
				if(counter < 5 && iterator.hasNext()){
					artifactList = artifactList + ",";
				}
				counter++;
			}
			
			xml_parser = new PostXMLParser();

			URL url = new URL(postURL);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

			try{

				urlConnection.setDoOutput(true);
				urlConnection.setChunkedStreamingMode(0);
				urlConnection.setRequestMethod("POST");

				Uri.Builder uriBuilder = new Uri.Builder()
						.appendQueryParameter("post_id", artifactList);
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
				postTitleList.addAll(pm.getTitlesOfNewPosts());
				boolean success = pm.savePostsToDB();

				if(success){
					successCounter++;
					result.setResultCode(200);
				}

				if(allSuccess){
					allSuccess = success;
				}

				if(successCounter == 1 && app.isThisFirstUse()){
					// We have  downloaded atleast 10 posts.
					// Set to HANU - Epoch
					app.addParameter("LastSyncTime", "1349328720");
					ProgressInfo pi = new ProgressInfo("Show UI");
					publishProgress(pi);
					Log.v(Application.TAG, "10 Posts are downloaded. Now we can show UI");
					Log.v(Application.TAG, "We will continue to download more posts");
				}

				if(!app.isThisFirstUse() || successCounter > 1){
					ProgressInfo pi = new ProgressInfo("Update UI");
					publishProgress(pi);
					Log.v(Application.TAG, "Some more posts are downloaded. UI will be updated");
				}

			}
			finally {
				urlConnection.disconnect();
			}
		}

		if(allSuccess){
			// If All success, then set sync time to now.
			app.addParameter("LastSyncTime", String.valueOf((new Date()).getTime() - 2*60*1000));
			Log.v(Application.TAG, "All posts downloaded successfully...");
		}
		else{
			// Some error occurred !
			Log.w(Application.TAG, "Error occured while downloading some posts !");
			if(app.isThisFirstUse()){
				// If first use, then set to HANU - Epoch
				app.addParameter("LastSyncTime", "1349328720");
			}
			else{
				// leave whatever it was
			}
		}
		
		// Prepare result
		result.getData().putInt("PostsDownloaded", postTitleList.size());
		result.getData().putBoolean("ShowNotification", true);
		result.getData().putStringArrayList("PostTitle", postTitleList);
	}

}