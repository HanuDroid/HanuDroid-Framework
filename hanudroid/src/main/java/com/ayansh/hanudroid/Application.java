package com.ayansh.hanudroid;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.DatabaseUtils;
import android.os.AsyncTask;
import android.util.Log;

import com.ayansh.CommandExecuter.CommandExecuter;
import com.ayansh.CommandExecuter.Invoker;
import com.ayansh.CommandExecuter.MultiCommand;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Application {

	private static Application application;
	public static final int EULA = 1;
	private static final int VersionCode = 30;
	private String SenderId;
	Context context;
	ApplicationDB appDB;
	public static String TAG;
	private static String appName;
	String blogURL;
	File FilesDirectory;
	private CommandExecuter ce;
	private FirebaseAnalytics mFirebaseAnalytics;

	String titleColor, titleFont, pubDateColor, pubDateFont, contentColor, contentFont;
	String taxonomyColor, taxonomyFont;
	
	HashMap<String,String> Options;
	
	public static Application getApplicationInstance(){
		// A singleton class.
		if(application == null){
			// Create a new instance.
			application = new Application();
		}
		
		return application;
		
	}
	
	// Set the context of the application.
	public void setContext(Context context){
		
		if(this.context == null){
			
			this.context = context;
			TAG = "HANU_" + getApplicationName(context);
			initializeAppOptions();
			
			// Initialize DB.
			initializeDB();
			
			// Sender ID
			SenderId = getStringFromResource("gcm_sender_id");
			
			// Initialize Post Style settings.
			titleColor = getStringFromResource("title_color");
			titleFont = getStringFromResource("title_font");
			pubDateColor = getStringFromResource("pub_date_color");
			pubDateFont = getStringFromResource("pub_date_font");
			contentColor = getStringFromResource("content_color");
			contentFont = getStringFromResource("content_font");
			taxonomyColor = getStringFromResource("taxonomy_color");
			taxonomyFont = getStringFromResource("taxonomy_font");

			// Firebase
			mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);

		}

	}
	
	String getStringFromResource(String name){
		// Get Id of the String Resource.
		int id = context.getResources().getIdentifier(name, "string", context.getPackageName());
		// Get Resource by Id
		return context.getResources().getString(id);
	}
	
	private void initializeDB(){
		// Initialize the DB.
		appDB = ApplicationDB.getInstance(context);
		appDB.openDBForWriting();
		appDB.loadOptions();
	}

	// Get the application Name.
	static String getApplicationName(Context context) {
		// Get the application name
		
		if(appName == null){

			ApplicationInfo ai = context.getApplicationInfo();
			PackageManager pm = context.getPackageManager();
			appName = (String) pm.getApplicationLabel(ai);
			appName = appName.replaceAll(" ", "");
			
		}
		
		return appName;
	}
	
	String getApplicationName(){
		return appName;
	}
	
	// Constructor
	private Application(){
		
		Options = new HashMap<String,String>();
		
	}
	
	void initializeAppOptions(){
		
		int id = context.getResources().getIdentifier("BlogURL", "string", context.getPackageName());
		blogURL = context.getResources().getString(id);
		
		FilesDirectory = context.getFilesDir();
		
	}

	public File getFilesDirectory(){
		return  FilesDirectory;
	}

	// Check if this is the first Usage !
	public boolean isThisFirstUse(){
		
		long count = DatabaseUtils.queryNumEntries(appDB.getWritableDatabase(), "Post");
		if(count > 0){
			return false;
		}
		else{
			return true;
		}		
	}

	public FirebaseAnalytics getFirebaseAnalytics(){
		return mFirebaseAnalytics;
	}
	
	// Initialize the app for other usage.
	public void initialize(Invoker caller){
		
		Log.v(TAG, "Initializing Application for regular use");
		
		ce = new CommandExecuter();
		MultiCommand command = new MultiCommand(caller);
		
		ValidateApplicationCommand validate = new ValidateApplicationCommand(caller);
		command.addCommand(validate);
		
		FetchArtifactsCommand fetchArtifacts = new FetchArtifactsCommand(caller);
		command.addCommand(fetchArtifacts);
		
		DownloadPostsCommand downloadPosts = new DownloadPostsCommand(caller);
		command.addCommand(downloadPosts);
		
		String regStatus = Options.get("RegistrationStatus");
		String regId = Options.get("RegistrationId");
		
		if(regId == null || regId.contentEquals("")){
			// Nothing to do.
		}
		else{
			if(regStatus == null || regStatus.contentEquals("")){
				SaveRegIdCommand saveRegId = new SaveRegIdCommand(caller,regId);
				command.addCommand(saveRegId);
			}
		}
		
		// Sync Ratings
		SyncRatingsCommand syncRating = new SyncRatingsCommand(caller);
		command.addCommand(syncRating);
		
		ce.execute(command);
		
	}
	
	// Initialize the app for first Usage.
	public void initializeAppForFirstUse(Invoker caller){
		
		Log.v(TAG, "Initializing Application for first use");
		
		ce = new CommandExecuter();
		MultiCommand command = new MultiCommand(caller);
		
		ValidateApplicationCommand validate = new ValidateApplicationCommand(caller);
		command.addCommand(validate);
		
		LoadPostsFromFileCommand loadFromFile = new LoadPostsFromFileCommand(caller);
		command.addCommand(loadFromFile);
		
		FetchArtifactsCommand fetchArtifacts = new FetchArtifactsCommand(caller);
		command.addCommand(fetchArtifacts);
		
		DownloadPostsCommand downloadPosts = new DownloadPostsCommand(caller);
		command.addCommand(downloadPosts);
		
		ce.execute(command);
				
	}
	
	// Get all Options
	HashMap<String,String> getOptions(){
		return Options;
	}

	public List<Post> getPostList(){
		return PostManager.getInstance().getDBPostList();
	}

	public Post getPostById(int postID){
		return PostManager.getInstance().getPostById(postID);
	}
	
	// Search for Posts
	public List<Post> performSearch(String query){
		PostManager.getInstance().performSearch(query);
		return PostManager.getInstance().getDBPostList();
	}
	
	// Load posts by Category from DB
	public List<Post> loadPostByCategory(String category){
		appDB.loadPost("category",category);	// This will load posts for category
		return PostManager.getInstance().getDBPostList();
	}

	// Load posts by Tag from DB
	public List<Post> loadPostByTag(String tag){
		appDB.loadPost("post_tag",tag);	// This will load posts for tag
		return PostManager.getInstance().getDBPostList();
	}

	// Load Post by Name
	public  Post loadPostByName(String name){
		return appDB.loadPostByName(name);	// This will load a post by its name
	}

	// Load Posts in date range
	public List<Post> loadPostsInDateRange(long fromTime, long toTime){
		appDB.loadPostsInDateRange(fromTime,toTime);	// This will load a post in time range
		return PostManager.getInstance().getDBPostList();
	}

	// Get a Random Post
	public  Post getRandomPostFromDB(){
		return appDB.getRandomPostFromDB();	// This will load a random post
	}

	// Get All Posts from DB
	public List<Post> getAllPosts(){
		appDB.loadPost(null,null);	// This will load all posts.
		return PostManager.getInstance().getDBPostList();
	}

	// Get All Posts
	public List<Post> getFavouritePosts(){
		appDB.loadFavouritePost();	// This will load all favourite posts.
		return PostManager.getInstance().getDBPostList();
	}
	
	// Get Posts by Category
	public List<Post> getPostsByCategory(String category){
		return PostManager.getInstance().filterPostList("category", category);
	}
	
	// Get Posts by Tag.
	public List<Post> getPostsByTag(String tag){
		return PostManager.getInstance().filterPostList("post_tag", tag);
	}
	
	// Get Posts by Author
	public List<Post> getPostsByAuthor(String name) {
		return PostManager.getInstance().filterPostList("author", name);
	}
	
	// Get all categories
	public List<String> getAllCategories(){
		return appDB.getAllCategories();
	}
	
	public List<String> getTagsInCategories(String category){
		return appDB.getTagsInCategories(category);
	}

	// Read Parameter
	public String readParameterValue(String name){

		String value = Options.get(name);
		if(value == null){
			value = "";
		}
		return value;
	}

	// Add parameter
	public boolean addParameter(String paramName, String paramValue){
		
		List<String> queries = new ArrayList<String>();
		String query = "";
		
		if(Options.containsKey(paramName)){
			// Already exists. Update it.
			query = "UPDATE Options SET ParamValue = '" + paramValue + "' WHERE ParamName = '" + paramName + "'";
		}
		else{
			// New entry. Create it
			query = "INSERT INTO Options (ParamName, ParamValue) VALUES ('" + paramName + "','" + paramValue + "')";
		}
		
		queries.add(query);
		boolean success = appDB.executeQueries(queries);
		
		if(success){
			Options.put(paramName, paramValue);
		}
		
		return success;
		
	}
	
	public boolean removeParameter(String paramName){
		
		List<String> queries = new ArrayList<String>();
		
		String query = "DELETE FROM Options WHERE ParamName = '" + paramName + "'";
		queries.add(query);
		boolean success = appDB.executeQueries(queries);
		
		if(success){
			Options.remove(paramName);
		}
		
		return success;
	}
	
	public void close() {
		// Close Application.
		if(ce != null){
			if(ce.getStatus() != AsyncTask.Status.FINISHED){
				// Pending or Running
				ce.cancel(true);
			}
		}
	}

	public int getNewFrameworkVersion() {
		return VersionCode;
	}

	public int getOldFrameworkVersion() {
		String versionCode = Options.get("HanuVersionCode");
		if(versionCode == null || versionCode.contentEquals("")){
			versionCode = "0";
		}
		return Integer.valueOf(versionCode);
	}
	
	public int getOldAppVersion() {
		String versionCode = Options.get("AppVersionCode");
		if(versionCode == null || versionCode.contentEquals("")){
			versionCode = "0";
		}
		return Integer.valueOf(versionCode);
	}

	public void updateVersion() {
		// Update Version
		// Since version is updated. We have to register again !
		addParameter("RegistrationId","");	// Set Reg Id to space.
		addParameter("HanuVersionCode", String.valueOf(VersionCode));
		addParameter("AppVersionCode", String.valueOf(getCurrentAppVersionCode()));
	}
	
	public int getCurrentAppVersionCode(){
		
		int version;
		try {
			version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			version = 0;
			Log.e(TAG, e.getMessage(), e);
		}
		return version;
	}

	public void setEULAResult(boolean result) {
		// Save EULA Result
		addParameter("EULA", String.valueOf(result));
	}
	
	public boolean isEULAAccepted(){
		String eula = Options.get("EULA");
		if(eula == null || eula.contentEquals("")){
			eula = "false";
		}
		return Boolean.valueOf(Options.get("EULA"));
	}

	public void deletePost(int postId) {
		// Delete a Post
		PostManager.getInstance().deletePost(postId);
	}
	
	public void addSyncCategory(String cat){
		
		String oldCat = Options.get("SyncCategory");
		if(oldCat == null || oldCat.contentEquals("")){	
			oldCat = cat;
		}
		else{

			if(oldCat.contains(cat)){
				// Nothing. We already have this category.
			}
			else{
				oldCat = oldCat + "," + cat;
			}

		}
		
		addParameter("SyncCategory", oldCat);
	}
	
	public void removeSyncCategory(String cat){
		
		String oldCat = Options.get("SyncCategory");
		String newCategories = "";
		
		if(oldCat == null || oldCat.contentEquals("")){	}
		else{
			
			String[] oldCategories = oldCat.split(",");
			
			for(int i=0; i<oldCategories.length; i++){
				
				if(cat.contentEquals(oldCategories[i])){ }
				else{
					if(newCategories.contentEquals("")){
						newCategories = oldCategories[i];
					}
					else{
						newCategories = newCategories + "," + oldCategories[i];
					}

				}
			}
		}

		addParameter("SyncCategory", newCategories);
	}

	public String getSenderId() {
		if(SenderId == null){
			SenderId = getStringFromResource("gcm_sender_id");
		}
		return SenderId;
	}

}