package com.ayansh.hanudroid;

import android.util.Log;

import com.ayansh.CommandExecuter.Command;
import com.ayansh.CommandExecuter.CommandExecuter;
import com.ayansh.CommandExecuter.Invoker;
import com.ayansh.CommandExecuter.ProgressInfo;
import com.ayansh.CommandExecuter.ResultObject;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;


public class HanuInstanceIDService extends FirebaseInstanceIdService {

	@Override
	public void onTokenRefresh() {

		// When this is called, refresh tokens.
		
		Application app = Application.getApplicationInstance();
		
		app.addParameter("RegistrationStatus", "");
		app.addParameter("RegistrationId", "");

		try {

			// [START get_token]
			FirebaseInstanceId instanceID = FirebaseInstanceId.getInstance();

			// If we don't have, then get Instance ID and save.
			String iid = instanceID.getId();
			app.addParameter("InstanceID", iid);

            String token = instanceID.getToken();
            // [END get_token]
            
            // Implement this method to send any registration to your app's servers.
            Log.v(Application.TAG, "Registration with FCM success");
    		Application.getApplicationInstance().addParameter("RegistrationId", token);

    		SaveRegIdCommand command = new SaveRegIdCommand(Command.DUMMY_CALLER, token);
    		command.execute();

        } catch (Exception e) {
            // Ignore.
        }
	}

}