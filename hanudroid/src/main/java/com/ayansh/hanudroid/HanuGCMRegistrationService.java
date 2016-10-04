package com.ayansh.hanudroid;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.ayansh.CommandExecuter.CommandExecuter;
import com.ayansh.CommandExecuter.Invoker;
import com.ayansh.CommandExecuter.ProgressInfo;
import com.ayansh.CommandExecuter.ResultObject;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;


public class HanuGCMRegistrationService extends IntentService{

	public HanuGCMRegistrationService() {
		
		super(Application.TAG);
		
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		Application app = Application.getApplicationInstance();
		
		try {
			/*
			 * [START register_for_gcm]
			 * Initially this call goes out to the network to retrieve the token, subsequent calls are local.
			 * 
			 */

            // [START get_token]
			InstanceID instanceID = InstanceID.getInstance(this);
			
			if(app.getOptions().get("InstanceID") == null){
				// If we don't have, then get Instance ID and save.
				String iid = instanceID.getId();
				app.addParameter("InstanceID", iid);
			}
			
            String token = instanceID.getToken(app.getSenderId(),GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);            
            // [END get_token]
            
            // Implement this method to send any registration to your app's servers.
            Log.v(Application.TAG, "Registration with GCM success");
    		Application.getApplicationInstance().addParameter("RegistrationId", token);
    		
    		CommandExecuter ce = new CommandExecuter();
    		
    		SaveRegIdCommand command = new SaveRegIdCommand(new Invoker(){

				@Override
				public void NotifyCommandExecuted(ResultObject arg0) {
					// Nothing to do.
					// Save will happen in the command itself.
				}

				@Override
				public void ProgressUpdate(ProgressInfo arg0) {
					// Nothing to do.
				}}, token);
    		
    		ce.execute(command);


        } catch (Exception e) {
            
        }
		
	}

}