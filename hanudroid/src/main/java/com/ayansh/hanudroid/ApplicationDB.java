package com.ayansh.hanudroid;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ApplicationDB extends SQLiteOpenHelper {

	private static ApplicationDB appDB;
	private static int DBVersion;
	private static String DBName;
	
	private Application app;
	private SQLiteDatabase data_base;
	
	static ApplicationDB getInstance(Context context){
		
		DBVersion = 3;
		String appName = Application.getApplicationName(context);
		DBName = "HANU_" + appName + "_DB";
		
		if(appDB == null){
			appDB = new ApplicationDB(context);
		}
		
		return appDB;
	}
	
	static ApplicationDB getInstance(){
		return appDB;
	}
	
	private ApplicationDB(Context context) {
		
		super(context, DBName, null, DBVersion);
		app = Application.getApplicationInstance();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// Create Database Tables here
		
		String createPostsTable = createPostsTable();
		
		String createPostMetaTable = "CREATE TABLE PostMeta (" +
				"Id INTEGER PRIMARY KEY AUTOINCREMENT, " +		// Primary Key
				"PostId INTEGER, " +	 						// Id of the Post
				"MetaKey VARCHAR(20), " + 						// Meta Key
				"MetaValue VARCHAR(20)" +						// Meta Value
				")";
		
		String createCommentsTable = createCommentsTable();
		
		String createTermsTable = "CREATE TABLE Terms (" + 
				"Id INTEGER PRIMARY KEY AUTOINCREMENT, " +		// Primary Key
				"PostId INTEGER, " +	 						// Id of the Post
				"Taxonomy VARCHAR(10), " + 						// Taxonomy
				"Name VARCHAR(20)" +							// Name
				")";
		
		String createOptionsTable = "CREATE TABLE Options (" + 
				"ParamName VARCHAR(20), " + 		// Parameter Name
				"ParamValue VARCHAR(20)" + 			// Parameter Value
				")";
		
		String createSyncStatusTable = "CREATE TABLE SyncStatus (" + 
				"Id INTEGER PRIMARY KEY AUTOINCREMENT, " +		// Primary Key
				"Type VARCHAR(20), " + 							// Type: Comment / Rating
				"SyncId INTEGER" + 								// Comment Id / Rating Id
				")";
		
		// Create View also.
		String createPostTermView = "CREATE VIEW PostTerm AS " +
				"SELECT a.*, b.* FROM Post As a, Terms as b WHERE a.Id = b.PostId";
		
		// create a new table - if not existing
		try {
			// Create Tables.
			Log.i(Application.TAG, "Creating Tables for Version:" + String.valueOf(DBVersion));
			db.execSQL(createPostsTable);
			db.execSQL(createPostMetaTable);
			db.execSQL(createCommentsTable);
			db.execSQL(createTermsTable);
			db.execSQL(createOptionsTable);
			db.execSQL(createSyncStatusTable);
			db.execSQL(createPostTermView);
			
			createFTSTables(db);
			
			Log.i(Application.TAG, "Tables created successfully");
						
		} catch (SQLException e) {
			Log.e(Application.TAG, e.getMessage(), e);
		}

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Nothing to do as of now.

		switch (oldVersion){

			case 2:
				updatePostTable(db);
				break;	// Add for now. Remove later on

			default:
				break;

		}
	}

	private void updatePostTable(SQLiteDatabase db){

		try {
			// Create Tables.
			Log.i(Application.TAG, "Upgrading Tables from Version: 2 to version 3");

			String addFavColumn = "ALTER TABLE Post ADD COLUMN IsFav INTEGER DEFAULT 0";
			String addViewCountColumn = "ALTER TABLE Post ADD COLUMN ViewCount INTEGER DEFAULT 0";

			db.execSQL(addFavColumn);
			db.execSQL(addViewCountColumn);

			Log.i(Application.TAG, "Upgrade of tables successfully");

		} catch (SQLException e) {
			Log.e(Application.TAG, e.getMessage(), e);
		}
	}

	private void createFTSTables(SQLiteDatabase db) throws SQLException{
		
		/*
		 * Create FTS3 tables for efficient searching. 
		 * We will need 2 tables - for Post and Comment
		 * 
		 * We will also need triggers so that both content and Index tables are in sync
		 * 
		 * We will be using External content FTS4 tables. 
		 * For more info, please read: http://www.sqlite.org/fts3.html#section_6_2
		 */
		String createPostFTSTable = "CREATE VIRTUAL TABLE PostIndex USING fts3 (" +
				"content=\"Post\", " +
				"Title, " +
				"PostContent" +
				")";
		
		String createCommentFTSTable = "CREATE VIRTUAL TABLE CommentsIndex USING fts3 (" +
				"content=\"Comments\", " +
				"PostId, " +
				"CommentsContent" +
				")";
		
		// Creating Triggers
		String post_bu = "CREATE TRIGGER Post_bu BEFORE UPDATE ON Post BEGIN DELETE FROM PostIndex WHERE docid=old.Id; END;";
		String post_bd = "CREATE TRIGGER Post_bd BEFORE DELETE ON Post BEGIN DELETE FROM PostIndex WHERE docid=old.Id; END;";
		String post_au = "CREATE TRIGGER Post_au AFTER UPDATE ON Post BEGIN " +
							"INSERT INTO PostIndex(docid, Title, PostContent) VALUES(new.Id, new.Title, new.PostContent); END;";
		String post_ai = "CREATE TRIGGER Post_ai AFTER INSERT ON Post BEGIN" +
							" INSERT INTO PostIndex(docid, Title, PostContent) VALUES(new.Id, new.Title, new.PostContent); END;";
				

		String comments_bu = "CREATE TRIGGER Comments_bu BEFORE UPDATE ON Comments BEGIN DELETE FROM CommentsIndex WHERE docid=old.CommentId; END;";
		String comments_bd = "CREATE TRIGGER Comments_bd BEFORE DELETE ON Comments BEGIN DELETE FROM CommentsIndex WHERE docid=old.CommentId; END;";
		String comments_au = "CREATE TRIGGER Comments_au AFTER UPDATE ON Comments BEGIN" +
								" INSERT INTO CommentsIndex(docid, PostId, CommentsContent) VALUES(new.CommentId, new.PostId, new.CommentsContent); END;";
		String comments_ai = "CREATE TRIGGER Comments_ai AFTER INSERT ON Comments BEGIN" +
								" INSERT INTO CommentsIndex(docid, PostId, CommentsContent) VALUES(new.CommentId, new.PostId, new.CommentsContent); END;";
							
		db.execSQL(createPostFTSTable);
		db.execSQL(createCommentFTSTable);
		Log.i(Application.TAG, "FTS Tables created successfully");
		
		db.execSQL(post_bu);
		db.execSQL(post_bd);
		db.execSQL(post_au);
		db.execSQL(post_ai);
		db.execSQL(comments_bu);
		db.execSQL(comments_bd);
		db.execSQL(comments_au);
		db.execSQL(comments_ai);
		Log.i(Application.TAG, "Triggers for FTS tables created successfully");
	}
	
	private String createPostsTable(){
		
		String createPostsTable = "CREATE TABLE Post (" + 
				"Id INTEGER PRIMARY KEY, " +	// Id
				"PubDate INTEGER, " + 			// PublishDate	Time in MilliSec
				"ModDate INTEGER, " + 			// Modified Date
				"Author VARCHAR(10), " + 		// Author
				"Title VARCHAR(20), " +			// Title of post
				"PostContent TEXT, " +		 	// Post Content
				"IsFav INTEGER, " + 			// Is Favourite
				"ViewCount INTEGER" + 			// View Count
				")";
		
		return createPostsTable;
	}
	
	private String createCommentsTable(){
		
		String createCommentsTable = "CREATE TABLE Comments (" + 
				"CommentId INTEGER PRIMARY KEY, " +		// Comment Id
				"PostId INTEGER, " + 					// Post Id
				"Author VARCHAR(10), " + 				// Author
				"AuthorEmail VARCHAR(20), " + 			// Author Email
				"CommentDate VARCHAR(20), " + 			// Comment Date
				"CommentParent INTEGER, " + 			// Comment Parent
				"CommentsContent TEXT, " +		 		// Comments Content
				"SyncStatus VARCHAR(1)" +				// Sync Status
				")";
		
		return createCommentsTable;
	}
	
	void openDBForWriting(){
		data_base = appDB.getWritableDatabase();
	}
	
	synchronized void loadOptions(){
		
		if(!data_base.isOpen()){
			return;
		}
		
		// Load Parameters
		String name, value;
		Log.v(Application.TAG, "Loading application Options");
		
		Cursor cursor = data_base.query("Options", null, null, null, null, null, null);
		
		if(cursor.moveToFirst()){
			name = cursor.getString(cursor.getColumnIndex("ParamName"));
			value = cursor.getString(cursor.getColumnIndex("ParamValue"));
			app.Options.put(name, value);
		}
		
		while(cursor.moveToNext()){
			name = cursor.getString(cursor.getColumnIndex("ParamName"));
			value = cursor.getString(cursor.getColumnIndex("ParamValue"));
			app.Options.put(name, value);
		}
		
		cursor.close();
		
	}
	
	synchronized boolean executeQueries(List<String> queries){
		
		Iterator<String> iterator =  queries.listIterator();
		String query;
		
		try {
			data_base.beginTransaction();
			
			while(iterator.hasNext()){
				query = iterator.next();
				data_base.execSQL(query);			
			}
			
			data_base.setTransactionSuccessful();
			data_base.endTransaction();
			return true;
		}
		catch (Exception e){
			// Do nothing! -- Track the error causing query
			Log.e(Application.TAG, e.getMessage(), e);
			data_base.endTransaction();
			return false;
		}	
	}

	synchronized boolean postExists(int id) {
		
		// Check if post Exists or not !
		String selection = "Id='" + id + "'";
		Cursor cursor = data_base.query("Post", null, selection, null, null, null, null);
		if(cursor.moveToFirst()){
			cursor.close();
			return true;
		}
		else{
			cursor.close();
			return false;
		}
		
	}
	
	synchronized boolean commentExists(int id) {
		
		// Check if post Exists or not !
		String selection = "CommentId='" + id + "'";
		Cursor cursor = data_base.query("Comments", null, selection, null, null, null, null);
		if(cursor.moveToFirst()){
			cursor.close();
			return true;
		}
		else{
			cursor.close();
			return false;
		}
		
	}

	synchronized void loadFavouritePost() {
		// Load Favourite Posts
		String selection = "IsFav=1";
		Cursor postCursor;
		PostManager pm = PostManager.getInstance();
		Post post;
		
		// Clear List before adding.
		pm.clearDBPostList();

		postCursor = data_base.query("Post", null, selection, null, null, null, "ViewCount ASC, PubDate DESC");

		if(postCursor.moveToFirst()){
			
			do{
				
				post = preparePostObject(postCursor);
				pm.addPostToDBList(post);
				
			}while(postCursor.moveToNext());
			
			
		}
		
		postCursor.close();
		
	}

	synchronized void loadPost(String taxonomy, String name) {
		// Load Posts
		String selection = null;
		Cursor postCursor;
		PostManager pm = PostManager.getInstance();
		Post post;

		// Clear List before adding.
		pm.clearDBPostList();

		if(taxonomy != null && name != null){

			if(taxonomy.contentEquals("author")){
				selection = "Author='" + name + "'";
				postCursor = data_base.query("Post", null, selection, null, null, null, "ViewCount ASC, PubDate DESC");
			}
			else{
				selection = "Taxonomy='" + taxonomy + "' AND Name='" + name + "'";
				postCursor = data_base.query("PostTerm", null, selection, null, null, null, "ViewCount ASC, PubDate DESC");
			}

		}
		else{
			postCursor = data_base.query("Post", null, null, null, null, null, "ViewCount ASC, PubDate DESC");
		}

		if(postCursor.moveToFirst()){

			do{

				post = preparePostObject(postCursor);
				pm.addPostToDBList(post);

			}while(postCursor.moveToNext());


		}

		postCursor.close();

	}

	synchronized private Post preparePostObject(Cursor postCursor) {
		// Prepare Post Object
		Cursor postMetaCursor, postCommentCursor, postTermCursor;
		Post post = new Post();
		String selection;
		String taxonomy, name;
		PostComment comment;
		
		post.Id = postCursor.getInt(postCursor.getColumnIndex("Id"));
		post.author = postCursor.getString(postCursor.getColumnIndex("Author"));
		
		post.pubDate = new Date(postCursor.getLong(postCursor.getColumnIndex("PubDate")));		
		post.modDate = new Date(postCursor.getLong(postCursor.getColumnIndex("ModDate")));
		
		post.title = postCursor.getString(postCursor.getColumnIndex("Title"));
		post.content = postCursor.getString(postCursor.getColumnIndex("PostContent"));

		post.isFavourite = postCursor.getInt(postCursor.getColumnIndex("IsFav"));
		post.viewCount = postCursor.getInt(postCursor.getColumnIndex("ViewCount"));

		selection = "PostId='" + post.Id + "'";
		
		// Get Meta Data
		postMetaCursor = data_base.query("PostMeta", null, selection, null, null, null, null);
		if(postMetaCursor.moveToFirst()){
			post.metaData.put(postMetaCursor.getString(postMetaCursor.getColumnIndex("MetaKey")),
								postMetaCursor.getString(postMetaCursor.getColumnIndex("MetaValue")));
		}
		while(postMetaCursor.moveToNext()){
			post.metaData.put(postMetaCursor.getString(postMetaCursor.getColumnIndex("MetaKey")),
					postMetaCursor.getString(postMetaCursor.getColumnIndex("MetaValue")));
		}
		
		// Get Comments
		postCommentCursor = data_base.query("Comments", null, selection, null, null, null, "CommentId ASC");
		if(postCommentCursor.moveToFirst()){
			comment = new PostComment();
			comment.commentId = postCommentCursor.getInt(postCommentCursor.getColumnIndex("CommentId"));
			comment.postId = postCommentCursor.getInt(postCommentCursor.getColumnIndex("PostId"));
			comment.commentParent = postCommentCursor.getInt(postCommentCursor.getColumnIndex("CommentParent"));
			comment.author = postCommentCursor.getString(postCommentCursor.getColumnIndex("Author"));
			comment.authorEmail = postCommentCursor.getString(postCommentCursor.getColumnIndex("AuthorEmail"));
			comment.commentDate = new Date(postCommentCursor.getLong(postCommentCursor.getColumnIndex("CommentDate")));
			comment.content = postCommentCursor.getString(postCommentCursor.getColumnIndex("CommentsContent"));
			post.postComments.add(comment);
		}
		while(postCommentCursor.moveToNext()){
			comment = new PostComment();
			comment.commentId = postCommentCursor.getInt(postCommentCursor.getColumnIndex("CommentId"));
			comment.postId = postCommentCursor.getInt(postCommentCursor.getColumnIndex("PostId"));
			comment.commentParent = postCommentCursor.getInt(postCommentCursor.getColumnIndex("CommentParent"));
			comment.author = postCommentCursor.getString(postCommentCursor.getColumnIndex("Author"));
			comment.authorEmail = postCommentCursor.getString(postCommentCursor.getColumnIndex("AuthorEmail"));
			comment.commentDate = new Date(postCommentCursor.getLong(postCommentCursor.getColumnIndex("CommentDate")));
			comment.content = postCommentCursor.getString(postCommentCursor.getColumnIndex("CommentsContent"));
			post.postComments.add(comment);
		}
		
		// Get Terms
		postTermCursor = data_base.query("Terms", null, selection, null, null, null, null);
		if(postTermCursor.moveToFirst()){
			
			taxonomy = postTermCursor.getString(postTermCursor.getColumnIndex("Taxonomy"));
			name = postTermCursor.getString(postTermCursor.getColumnIndex("Name"));
			
			if(taxonomy.contentEquals("category")){
				post.categories.add(name);
			}
			else if(taxonomy.contentEquals("post_tag")){
				post.tags.add(name);
			}
			
		}
		while(postTermCursor.moveToNext()){
			
			taxonomy = postTermCursor.getString(postTermCursor.getColumnIndex("Taxonomy"));
			name = postTermCursor.getString(postTermCursor.getColumnIndex("Name"));
			
			if(taxonomy.contentEquals("category")){
				post.categories.add(name);
			}
			else if(taxonomy.contentEquals("post_tag")){
				post.tags.add(name);
			}
			
		}
		
		postMetaCursor.close();
		postCommentCursor.close();
		postTermCursor.close();
		
		return post;
		
	}

	synchronized void loadPostArtifacts(String postIds) {
		// Load Post Artifacts
		Cursor postCursor, commentCursor;
		String selection, query;
		PostArtifact artifact;
		PostManager pm = PostManager.getInstance();
		pm.dbPostArtifacts.clear();
		
		selection = "ID in (" + postIds + ")";
		postCursor = data_base.query("Post", null, selection, null, null, null, null);
		
		if(postCursor.moveToFirst()){
			
			artifact = new PostArtifact();
			artifact.postId = postCursor.getInt(postCursor.getColumnIndex("Id"));
			artifact.pubDate = new Date(postCursor.getLong(postCursor.getColumnIndex("PubDate")));		
			artifact.modDate = new Date(postCursor.getLong(postCursor.getColumnIndex("ModDate")));
			
			query = "SELECT max('CommentDate') FROM Comments WHERE PostId='" + artifact.postId + "'";
			commentCursor = data_base.rawQuery(query, null);
			if(commentCursor.moveToFirst()){
				artifact.commDate = new Date(commentCursor.getLong(0));
			}
			
			pm.dbPostArtifacts.put(artifact.postId, artifact);
			commentCursor.close();
			
		}
		while(postCursor.moveToNext()){
			
			artifact = new PostArtifact();
			artifact.postId = postCursor.getInt(postCursor.getColumnIndex("Id"));
			artifact.pubDate = new Date(postCursor.getLong(postCursor.getColumnIndex("PubDate")));		
			artifact.modDate = new Date(postCursor.getLong(postCursor.getColumnIndex("ModDate")));
			
			query = "SELECT max('CommentDate') FROM Comments WHERE PostId='" + artifact.postId + "'";
			commentCursor = data_base.rawQuery(query, null);
			if(commentCursor.moveToFirst()){
				artifact.commDate = new Date(commentCursor.getLong(0));
			}
			else{
				artifact.commDate = new Date(0);
			}
			
			pm.dbPostArtifacts.put(artifact.postId, artifact);
			commentCursor.close();
			
		}
		
		postCursor.close();
		
	}
	
	synchronized ArrayList<Integer> getSyncData(String syncType){
		
		ArrayList<Integer> syncList = new ArrayList<Integer>();
		String selection = "Type='" + syncType + "'";
		
		Cursor cursor = data_base.query("SyncStatus", null, selection, null, null, null, null);
		
		if(cursor.moveToFirst()){
			
			do{
				syncList.add(cursor.getInt(cursor.getColumnIndex("SyncId")));
			}while(cursor.moveToNext());
			
		}
		
		cursor.close();
		
		return syncList;
		
	}
	
	synchronized String getPostIds(){
		
		String postIds = "";
		String columns[] = {"Id"};
		
		Cursor cursor = data_base.query("Post", columns, null, null, null, null, null);
		
		if(cursor.moveToFirst()){
			do{
				
				postIds += String.valueOf(cursor.getInt(0)) + ",";
				
			}while(cursor.moveToNext());
		}
		
		postIds = postIds.substring(0, postIds.length() - 1);
		return postIds;
	}
	
	ArrayList<Integer> performSearch(String query){
		
		ArrayList<Integer> searchResult = new ArrayList<Integer>();
		
		// Search in Post Table.
		String selection = "PostIndex MATCH '" + query + "'";
		String columns[] = {"docid"};
		
		Cursor cursor = data_base.query("PostIndex", columns, selection, null, null, null, null);
		if(cursor.moveToFirst()){
			
			do{
				searchResult.add(cursor.getInt(0));
			} 
			while(cursor.moveToNext());
			
		}
		cursor.close();
		
		// Search in Comments Table
		selection = "CommentsIndex MATCH '" + query + "'";
		String columns1[] = {"PostId"};
		
		cursor = data_base.query("CommentsIndex", columns1, selection, null, null, null, null);
		if(cursor.moveToFirst()){
			
			do{
				searchResult.add(cursor.getInt(0));
			}
			while(cursor.moveToNext());
		}
		cursor.close();
		
		return searchResult;
	}

	List<String> getAllCategories() {
		
		List<String> catList = new ArrayList<String>();
		
		String sql = "SELECT DISTINCT Name FROM Terms WHERE Taxonomy = 'category'";
		
		Cursor cursor = data_base.rawQuery(sql, null);
		
		if(cursor.moveToFirst()){
			
			do{
				
				catList.add(cursor.getString(0));
				
			}while(cursor.moveToNext());
			
		}
		
		cursor.close();
		
		return catList;
		
	}

	List<String> getTagsInCategories(String category) {
		
		List<String> tagList = new ArrayList<String>();
		
		String sql = "SELECT DISTINCT a.Name FROM Terms as a INNER JOIN Terms as b"
				+ " on a.PostId = b.PostId WHERE b.Taxonomy = 'category' and b.Name = '"
				+ category + "' and a.Taxonomy = 'post_tag'";
		
		Cursor cursor = data_base.rawQuery(sql, null);
		
		if(cursor.moveToFirst()){
			
			do{
				
				tagList.add(cursor.getString(0));
				
			}while(cursor.moveToNext());
			
		}
		
		cursor.close();
		
		return tagList;
		
	}
	
}