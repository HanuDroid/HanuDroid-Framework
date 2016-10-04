package com.ayansh.hanudroid;

import android.os.Bundle;
import android.util.Log;

import com.ayansh.CommandExecuter.Invoker;
import com.ayansh.CommandExecuter.MultiCommand;
import com.ayansh.CommandExecuter.ProgressInfo;
import com.ayansh.CommandExecuter.ResultObject;
import com.google.android.gms.gcm.GcmListenerService;

public abstract class HanuGCMListenerService extends GcmListenerService implements Invoker {
	

	protected ResultObject processMessage(String from, Bundle data) {
		// Message received with Intent
		
		// Initialize the application
		Application app = Application.getApplicationInstance();
		app.setContext(getApplicationContext());
		
		String message = data.getString("message");
		//String collapseKey = intent.getExtras().getString("collapse_key");
		
		// Gen Validation
		if(from.contentEquals(app.getSenderId())){
			
			// User / Pwd is sent by our server. So save it.
			if(message.contentEquals("UserData")){
				
				String user = data.getString("user");
				String pwd = data.getString("pwd");
				app.addParameter("WP_User", user);
				app.addParameter("WP_Pwd", pwd);
				return new ResultObject();
			}
			
			if(message.contentEquals("PerformSync")){
				Log.v(Application.TAG, "Message to Perform Sync recieved from GCM");
				return performSync();
			}
			
			if(message.contentEquals("SyncAll")){
				Log.v(Application.TAG, "Message to Perform Sync-All recieved from GCM");
				Application.getApplicationInstance().getOptions().put("LastSyncTime", "1349328720");
				return performSync();
			}
			
			if(message.contentEquals("SyncPostIds")){
				String postIds = data.getString("PostIds");
				DownloadIndividualPostsCommand command = new DownloadIndividualPostsCommand(this,postIds);
				return command.execute();
			}
			
			if(message.contentEquals("DeletePost")){
				String postIds = data.getString("PostIds");
				int postId = Integer.valueOf(postIds);
				if(postId != 0){
					app.deletePost(postId);
				}
			}
			
		}
		
		return new ResultObject();
		
	}
	
	private ResultObject performSync(){
		
		MultiCommand command = new MultiCommand(this);
		
		FetchArtifactsCommand fetchArtifacts = new FetchArtifactsCommand(this);
		command.addCommand(fetchArtifacts);
		
		DownloadPostsCommand downloadPosts = new DownloadPostsCommand(this);
		command.addCommand(downloadPosts);
		
		// Sync Ratings
		SyncRatingsCommand syncRating = new SyncRatingsCommand(this);
		command.addCommand(syncRating);
		
		// Update Post Meta
		UpdatePostMetaCommand updatePostMeta = new UpdatePostMetaCommand(this);
		command.addCommand(updatePostMeta);
		
		return command.execute();
	}

	@Override
	public void NotifyCommandExecuted(ResultObject result) {
		// Nothing to do
	}

	@Override
	public void ProgressUpdate(ProgressInfo result) {
		// Nothing to do
		
	}

}
