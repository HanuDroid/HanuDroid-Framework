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

import org.json.JSONObject;

public class CreateNewPostCommand extends Command {

	private String title, content, name;

	public CreateNewPostCommand(Invoker caller, String title, String content,
								String name) {
		super(caller);
		this.title = title;
		this.content = content;
		this.name = name;
	}

	@Override
	protected void execute(ResultObject result) throws Exception {

		Application app = Application.getApplicationInstance();
		String baseUrl = app.blogURL;
		String postURL = baseUrl + "/wp-content/plugins/hanu-droid/CreateNewPost.php";
		
		// Format output
		title = title.replace("&", "and");
		content = content.replace("&", "and");

		// Prepare Data
		/*
		JSONObject input = new JSONObject();
		input.put("title", title);
		input.put("content", content);
		input.put("name", name);
		input.put("user", app.getOptions().get("WP_User"));
		input.put("pwd", app.getOptions().get("WP_Pwd"));
		input.put("iid", app.getOptions().get("InstanceID"));
		*/

		URL url = new URL(postURL);
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

		try {

			urlConnection.setDoOutput(true);
			urlConnection.setChunkedStreamingMode(0);
			urlConnection.setRequestMethod("POST");

			Uri.Builder uriBuilder = new Uri.Builder()
					.appendQueryParameter("title", title)
					.appendQueryParameter("content", content)
					.appendQueryParameter("name", name)
					.appendQueryParameter("iid", app.getOptions().get("InstanceID"));

			String parameterQuery = uriBuilder.build().getEncodedQuery();

			OutputStream os = urlConnection.getOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
			writer.write(parameterQuery);
			writer.flush();
			writer.close();
			os.close();

			urlConnection.connect();

			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}

			JSONObject output = new JSONObject(builder.toString());
			int postId = output.getInt("post_id");

			if(postId > 0){
				// Success
				result.setResultCode(200);
			}
			else{
				// Failure
				result.setResultCode(400);
			}

		}
		finally {
			urlConnection.disconnect();
		}

	}

}