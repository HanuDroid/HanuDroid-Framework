package com.ayansh.hanudroid;

import android.util.Log;

import com.ayansh.CommandExecuter.Command;
import com.ayansh.CommandExecuter.Invoker;
import com.ayansh.CommandExecuter.MultiCommand;
import com.ayansh.CommandExecuter.ProgressInfo;
import com.ayansh.CommandExecuter.ResultObject;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

public abstract class HanuFCMMessagingService extends FirebaseMessagingService {
	

	protected ResultObject processMessage(RemoteMessage remoteMessage) {
		// Message received with Intent
		
		// Initialize the application
		Application app = Application.getApplicationInstance();
		app.setContext(getApplicationContext());

		Map<String,String> data = remoteMessage.getData();

		String message = data.get("message");
		//String collapseKey = intent.getExtras().getString("collapse_key");
		
		// User / Pwd is sent by our server. So save it.
		if(message.contentEquals("UserData")){

			String user = data.get("user");
			String pwd = data.get("pwd");
			app.addParameter("WP_User", user);
			app.addParameter("WP_Pwd", pwd);
			return new ResultObject();
		}

		if(message.contentEquals("PerformSync")){

			Log.v(Application.TAG, "Message to Perform Sync received from GCM");

			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			df.setTimeZone(TimeZone.getTimeZone("GMT"));	// Very Important to set this.

			String latest_data_timestamp_string = data.get("latest_data_timestamp");

			try{

				Date lastUpdateTime = df.parse(latest_data_timestamp_string);
				long latestTime = lastUpdateTime.getTime();

				long modTime = Long.valueOf(app.getOptions().get("LastSyncTime"));

				if(modTime < latestTime){
					// Need to sync.
					return performSync();
				}

			}catch(Exception e){
				return performSync();
			}

		}

		if(message.contentEquals("PingMessage")){
			Log.v(Application.TAG, "Ping Message received from GCM");
			return new ResultObject();
		}

		if(message.contentEquals("SyncAll")){
			Log.v(Application.TAG, "Message to Perform Sync-All recieved from GCM");
			app.getOptions().put("LastSyncTime", "1349328720");
			return performSync();
		}

		if(message.contentEquals("SyncPostIds")){
			String postIds = data.get("PostIds");
			DownloadIndividualPostsCommand command = new DownloadIndividualPostsCommand(Command.DUMMY_CALLER,postIds);
			return command.execute();
		}

		if(message.contentEquals("DeletePost")){
			String postIds = data.get("PostIds");
			int postId = Integer.valueOf(postIds);
			if(postId != 0){
				app.deletePost(postId);
			}
		}

		if(message.contentEquals("SetParameter")) {

			String param_name = data.get("ParameterName");
			String param_value = data.get("ParameterValue");
			String overwrite = data.get("Overwrite");

			if(app.getOptions().containsKey(param_name)){
				if(overwrite.contentEquals("X")){
					app.addParameter(param_name,param_value);
				}
			}
			else{
				app.addParameter(param_name,param_value);
			}
		}

		return new ResultObject();
		
	}

	private ResultObject performSync(){

		MultiCommand command = new MultiCommand(Command.DUMMY_CALLER);

		FetchArtifactsCommand fetchArtifacts = new FetchArtifactsCommand(Command.DUMMY_CALLER);
		command.addCommand(fetchArtifacts);

		DownloadPostsCommand downloadPosts = new DownloadPostsCommand(Command.DUMMY_CALLER);
		command.addCommand(downloadPosts);

		// Sync Ratings
		SyncRatingsCommand syncRating = new SyncRatingsCommand(Command.DUMMY_CALLER);
		command.addCommand(syncRating);

		// Update Post Meta
		UpdatePostMetaCommand updatePostMeta = new UpdatePostMetaCommand(Command.DUMMY_CALLER);
		command.addCommand(updatePostMeta);

		return command.execute();
	}

	@Override
	public void onNewToken(String token) {

		// When this is called, refresh tokens.

		Application app = Application.getApplicationInstance();

		app.addParameter("RegistrationStatus", "");
		app.addParameter("RegistrationId", "");

		try {

			FirebaseInstanceId instanceID = FirebaseInstanceId.getInstance();
			String iid = instanceID.getId();
			app.addParameter("InstanceID", iid);

			// Implement this method to send any registration to your app's servers.
			Log.v(Application.TAG, "Registration with FCM success");
			Application.getApplicationInstance().addParameter("RegistrationId", token);

			SaveRegIdCommand command = new SaveRegIdCommand(Command.DUMMY_CALLER, token);
			command.execute();

		} catch (Exception e) {
			// Ignore
		}
	}

}
