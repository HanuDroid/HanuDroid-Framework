package com.ayansh.hanudroid;

import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import android.util.Xml;

import com.ayansh.CommandExecuter.Command;
import com.ayansh.CommandExecuter.Invoker;
import com.ayansh.CommandExecuter.ProgressInfo;
import com.ayansh.CommandExecuter.ResultObject;

public class LoadPostsFromFileCommand extends Command {

	LoadPostsFromFileCommand(Invoker caller) {
		super(caller);
	}

	@Override
	protected void execute(ResultObject result) throws Exception {
		
		try{
			
			PostManager pm;
			
			Context c = Application.getApplicationInstance().context;
			AssetManager assetManager = c.getAssets();
			
			Log.v(Application.TAG, "Loading default posts from file");
			InputStream is = assetManager.open("default_data.xml");
			InputStreamReader isr;
			PostXMLParser xml_parser;

			pm = PostManager.getInstance();

			xml_parser = new PostXMLParser();

			// Get Input Stream Reader.
			isr = new InputStreamReader(is);

			// Parse the input XML.
			Log.v(Application.TAG, "Parsing the post data");
			Xml.parse(isr, xml_parser);

			// Save to DB.
			boolean success = pm.savePostsToDB();

			if (success) {
				result.setResultCode(200);
				ProgressInfo pi = new ProgressInfo("Show UI");
				publishProgress(pi);
			}
			
		}catch (Exception e){
			// We ignore this :D
			Log.e(Application.TAG, e.getMessage(), e);
		}
	}
}