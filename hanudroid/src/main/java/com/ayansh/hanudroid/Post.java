package com.ayansh.hanudroid;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import android.database.DatabaseUtils;
import android.os.Environment;
import android.text.Html;
import android.util.Log;

public class Post {
	
	int Id;
	String author, title, content;
	Date pubDate, modDate;
	HashMap<String,String> metaData;
	List<String> categories, tags;
	List<PostComment> postComments;
	
	// Constructor.	
	Post(){
		
		metaData = new HashMap<String,String>();
		categories = new ArrayList<String>();
		tags = new ArrayList<String>();
		postComments = new ArrayList<PostComment>();
		
	}

	@Override
	public String toString(){
		return title;
	}
	
	// Comparator
	static Comparator<Post> SortByPubDate = new Comparator<Post>(){
		@Override
		public int compare(Post lhs, Post rhs) {
			if(lhs.pubDate == null || rhs.pubDate == null){
				return 1;
			}
			else{
				return rhs.pubDate.compareTo(lhs.pubDate);
			}
		}
	};
	
	void saveToDB() throws Exception {
		//Save to DB.
		int start = 0, end = 0, s = 0, e = 0, index = 0, newStart = 0;
		int counter = 0;
		String subStr = "", replaceStr;
		String fileName;
		List<String> queries = new ArrayList<String>();
		
		do{
			
			start = content.indexOf("<a", newStart);
			end = content.indexOf("</a>", newStart);
			
			if(start > 0 && end > 0){
				subStr = content.substring(start, end);
			}
			
			if(start > 0 && end > 0 && subStr.contains("<img")){
				// We found something.
				replaceStr = subStr = content.substring(start, end);
				s = subStr.indexOf("src=\"") + 5;
				e = subStr.indexOf('"' , s);
				subStr = subStr.substring(s,e);	// This is Image URL.
				
				index = subStr.indexOf(".");
				fileName = subStr.substring(index, index + 4);
				fileName = String.valueOf(Id) + "-" + counter + fileName;
				
				// Download Image.
				downloadImage(subStr, fileName);
					
				// If download is success. Replace the File name
				String dir = Application.getApplicationInstance().DIR;				
				File root = Environment.getExternalStorageDirectory();
				File directory = new File(root, dir);
				String imgSrc = "<img class=\"alignnone\" src=\"file:" + directory + "/" + fileName + "\">";
				
				content = content.replace(replaceStr, imgSrc);				
				counter++;
				
			}
			
			newStart = end + 3;
			
		} while(start > 0 && end > 0);
		
		// Now save to DB.
		String query;
		
		if(ApplicationDB.getInstance().postExists(Id)){
			// We already have this -- Update DB.
			
			String c = DatabaseUtils.sqlEscapeString(content);
			String t = DatabaseUtils.sqlEscapeString(title);
			query = "UPDATE Post SET " +
					"Title=" + t + "," +
					"PubDate='" + pubDate.getTime() + "'," +
					"ModDate='" + modDate.getTime() + "'," +
					"PostContent=" + c + "" +
					"WHERE ID='" + Id + "'";
			
			queries.add(query);
			
			// Post Meta Data.
			query = "";
						
			for (Map.Entry<String, String> entry : metaData.entrySet()){
				
				query = "UPDATE PostMeta SET " +
						"MetaValue='" + entry.getValue() + "'" + 
						"WHERE PostId='" + Id + "' AND MetaKey='" + entry.getKey() + "'";
				
				queries.add(query);
				
			}
			
			// Post Comments.
			Iterator<PostComment> iterator = postComments.iterator();
			PostComment comment;
			
			while(iterator.hasNext()){
				comment = iterator.next();
				query = comment.UPSertQuery();
				if(query != null){
					queries.add(query);
				}
			}
			
			// Categories - Delete and add all.
			query = "DELETE FROM Terms WHERE PostId='" + Id + "'";
			queries.add(query);
			addQueriesForCategories(queries);
								
			// Save Tags
			addQueriesForTags(queries);
			
		}
		else{
			// New entry. Insert DB.
			// Post Query
			String c = DatabaseUtils.sqlEscapeString(content);
			String t = DatabaseUtils.sqlEscapeString(title);
			query = "INSERT INTO Post (Id, PubDate, ModDate, Author, Title, PostContent) VALUES (" +
					"'" + Id + "'," +
					"'" + pubDate.getTime() + "'," + 
					"'" + modDate.getTime() + "'," +
					"'" + author + "'," +
					"" + t + "," +
					"" + c + ")";
			
			queries.add(query);
			
			// Post Meta Data.
			query = "";
			
			for (Map.Entry<String, String> entry : metaData.entrySet()){
				
				query = "INSERT INTO PostMeta (PostId, MetaKey, MetaValue) VALUES (" +
						"'" + Id + "'," +
						"'" + entry.getKey() + "'," + 
						"'" + entry.getValue() + "')";
				
				queries.add(query);
				
			}
			
			// Post Comments.
			Iterator<PostComment> iterator = postComments.iterator();
			PostComment comment;
			
			while(iterator.hasNext()){
				
				comment = iterator.next();
				query = comment.insertQuery();
				queries.add(query);
				
			}
			
			// Categories
			query = "";
			addQueriesForCategories(queries);
			
			// Save Tags
			addQueriesForTags(queries);
			
		}
		
		boolean success = ApplicationDB.getInstance().executeQueries(queries);
		if(!success){
			throw new Exception("Error occured while saving post in DB");
		}
		
	}
	
	private void addQueriesForCategories(List<String> queries) {
		
		Iterator<String> cat = categories.listIterator();
		String query;
		while(cat.hasNext()){
			
			query = "INSERT INTO Terms (PostId, Taxonomy, Name) VALUES (" +
					"'" + Id + "'," +
					"'" + "category" + "'," + 
					"'" + cat.next() + "')";
			
			queries.add(query);
		}
		
	}
	
	private void addQueriesForTags(List<String> queries) {
		
		Iterator<String> tag = tags.listIterator();
		String query;
		while(tag.hasNext()){
			
			query = "INSERT INTO Terms (PostId, Taxonomy, Name) VALUES (" +
					"'" + Id + "'," +
					"'" + "post_tag" + "'," + 
					"'" + tag.next() + "')";
			
			queries.add(query);
		}
		
	}

	private void  downloadImage(String imageURL, String fileName) throws Exception{
		
		Log.v(Application.TAG, "Downloading Image for post...");
		String dir = Application.getApplicationInstance().DIR;
		
		// Create directory if it does not exists yet...
		File root = Environment.getExternalStorageDirectory();
		File directory = new File(root, dir);
		File image_file = new File(directory, fileName);
		
		if (directory.exists()){}
		else{
			directory.mkdirs();
		}
		
		URL url = new URL(imageURL);
			
		/* Open a connection to that URL. */
        URLConnection ucon = url.openConnection();
            
        //* Define InputStreams to read from the URLConnection.
		InputStream is = ucon.getInputStream();
		FileOutputStream fos = new FileOutputStream(image_file);

		int bytesRead = -1;
		byte[] buffer = new byte[4096];
		while ((bytesRead = is.read(buffer)) != -1) {
			fos.write(buffer, 0, bytesRead);
		}

	    fos.close();
		is.close();
	    Log.v(Application.TAG, "Image downloaded successfully...");
	}

	public String getContent(boolean stripHtmlTags){

		if(stripHtmlTags){
			return Html.fromHtml(content).toString();
		}else{
			return content;
		}
	}
	
	public int getId(){
		return Id;
	}
	
	public Date getPublishDate(){
		return pubDate;
	}
	
	public Date getModifiedDate(){
		return modDate;
	}
	
	public String getTitle(){
		return title;
	}
	
	public String getAuthor(){
		return author;
	}
	
	public List<PostComment> getComments(){
		return postComments;
	}
	
	public List<String> getCategories(){
		return categories;
	}
	
	public List<String> getTags(){
		return tags;
	}
	
	public HashMap<String,String> getMetaData(){
		return metaData;
	}
	
	public PostComment getComment(int commentId){
		
		ListIterator<PostComment> iterator = postComments.listIterator();
		PostComment comment;
		
		while(iterator.hasNext()){
			
			comment = iterator.next();
			if(comment.commentId == commentId){
				return comment;
			}
			
		}
		return null;
		
	}

	public String getHTMLCode() {
		// Create HTML Code.
		
		Application app = Application.getApplicationInstance();
		SimpleDateFormat df = new SimpleDateFormat();
		ListIterator<String> iterator;
		String name;

		String html = "<HTML>" +

				// HTML Head
				"<head>" +

				// Java Script
				"<script type=\"text/javascript\">function loadPosts(taxonomy,name){Main.loadPosts(taxonomy,name);}</script>" +

				// CSS
				"<style>" + 
				"h3 {color:" + app.titleColor + ";font-family:"	+ app.titleFont	+ ";}" +
				"#pub_date {color:" + app.pubDateColor + ";font-family:" + app.pubDateFont + ";font-size:14px;}" +
				"#content {color:"	+ app.contentColor + ";font-family:" + app.contentFont + "; font-size:18px;}" +
				".taxonomy {color:" + app.taxonomyColor + ";font-family:" + app.taxonomyFont + "; font-size:14px;}" +
				"#comments {color:" + app.contentColor + ";font-family:" + app.contentFont + "; font-size:16px;}" +
				"#ratings {color:black; font-family:verdana,geneva,sans-serif; font-size:14px;}" +
				"#footer {color:#0000ff; font-family:verdana,geneva,sans-serif; font-size:14px;}"+
				"</style>" +

				"</head>" +

				// HTML Body
				"<body>" +

				// Heading
				"<h3>" + title + "</h3>" +

				// Pub Date
				"<div id=\"pub_date\">" + df.format(pubDate) + "</div>" +
				"<hr />" +

				// Content
				"<div id=\"content\">" + getContent(false) + "</div>" +
				"<hr />" +
				
				// Author
				"<div class=\"taxonomy\">" +
				"by <a href=\"javascript:loadPosts('author','" + author + "')\">" + author + "</a>" +
				"</div>";

		// Categories
		iterator = categories.listIterator();
		if (categories.size() > 0) {

			html = html + "<br /><div class=\"taxonomy\">" + " in Category: ";

			while (iterator.hasNext()) {

				name = iterator.next();
				html = html + "<a href=\"javascript:loadPosts('category','" + name + "')\">" + name + "</a>, ";
			}

			html = html + "</div>";
		}

		// Tags
		iterator = tags.listIterator();
		if (tags.size() > 0) {

			html = html + "<br /><div class=\"taxonomy\">" + " tagged with: ";

			while (iterator.hasNext()) {
				name = iterator.next();
				html = html + "<a href=\"javascript:loadPosts('post_tag','" + name + "')\">" + name + "</a>, ";
			}

			html = html + "</div>";
		}

		// Ratings
		if (metaData.size() > 0
				&& !metaData.get("ratings_users").contentEquals("0")) {
			// We have some ratings !
			html = html + "<div id=\"ratings\">" + "<br>Rating: "
					+ String.format("%.2g%n", Float.valueOf(metaData.get("ratings_average")))
					+ " / 5 (by " + metaData.get("ratings_users") + " users)";

			html = html + "</div>";
		}

		// Comments.
		if (postComments.size() > 0) {
			// we have comments
			html = html + "<div id=\"comments\">" + "Comments:<br>";

			Iterator<PostComment> commentIterator = postComments.listIterator();
			PostComment comment, parentComment;

			while (commentIterator.hasNext()) {

				comment = commentIterator.next();

				html = html + "<p>" + comment.getAuthor() + " on " + df.format(comment.getCommentDate());

				if (comment.getCommentParent() != 0) {
					// We have a parent also !
					parentComment = getComment(comment.getCommentParent());
					html = html + " in reply to " + parentComment.getAuthor()
							+ " on " + df.format(parentComment.getCommentDate());
				}

				html = html + ":" + comment.getContent() + "</p>";
			}

		}

		// Footer
		html = html + "<br /><hr />" + "<div id=\"footer\">" 
				+ "Powered by <a href=\"http://hanu-droid.varunverma.org\">Hanu-Droid framework</a>"
				+ "</div>" +

				"</body>" +
				"</html>";

		return html;
	}

	public void addRating(float rating) {
		// Add Rating
		int usersRated;
		float ratingScore, ratingAvg;
		String query;
		ArrayList<String> queries = new ArrayList<String>();
		
		try{
			usersRated = Integer.valueOf(metaData.get("ratings_users"));
			ratingScore = Float.valueOf(metaData.get("ratings_score"));
			ratingAvg = Float.valueOf(metaData.get("ratings_average"));
		}catch (Exception e){
			usersRated = 0;
			ratingScore = ratingAvg = 0;
		}
		
		String oldRating = metaData.get("my_rating");
		
		if(oldRating == null || oldRating.contentEquals("")){
			// I have not rated it before ! So Insert.
			usersRated++;	// Add by 1.
			ratingScore += rating;
			ratingAvg = ratingScore / usersRated;
			
			// Add My rating
			query = "INSERT INTO PostMeta (PostId,MetaKey,MetaValue) VALUES ('" + Id + "'," + "'my_rating'," + 
					"'" + rating + "')";
			queries.add(query);
		}
		else{
			// Updating the rating.
			ratingScore = ratingScore - Float.valueOf(oldRating) + rating;
			ratingAvg = ratingScore / usersRated;
			
			//Update My Rated
			query = "UPDATE PostMeta SET MetaValue='" + rating + "' WHERE PostId='" + Id + "' AND MetaKey='my_rating'";
			queries.add(query);
		}
		
		
		if(metaData.get("ratings_users") == null){
			// Globally, I am rating for first time !
			//Update Users Rated
			query = "INSERT INTO PostMeta (PostId,MetaKey,MetaValue) VALUES ('" + Id + "'," + "'ratings_users'," + 
					"'" + usersRated + "')";
			queries.add(query);
			
			//Update Rating Score
			query = "INSERT INTO PostMeta (PostId,MetaKey,MetaValue) VALUES ('" + Id + "'," + "'ratings_score'," + 
					"'" + ratingScore + "')";
			queries.add(query);
			
			//Update Average Rating
			query = "INSERT INTO PostMeta (PostId,MetaKey,MetaValue) VALUES ('" + Id + "'," + "'ratings_average'," + 
					"'" + ratingAvg + "')";
			queries.add(query);
		}
		else{
			//Update Rating Score
			query = "UPDATE PostMeta SET MetaValue='" + ratingScore + "' WHERE PostId='" + Id + "' AND MetaKey='ratings_score'";
			queries.add(query);
			
			//Update Average Rating
			query = "UPDATE PostMeta SET MetaValue='" + ratingAvg + "' WHERE PostId='" + Id + "' AND MetaKey='ratings_average'";
			queries.add(query);
		}

		// Add Data for Sync
		query = "INSERT INTO SyncStatus (Type,SyncId) VALUES ('PostRating','" + Id + "')";
		queries.add(query);
			 
		ApplicationDB.getInstance().executeQueries(queries);
		
	}

	public float getMyRating() {
		// Get My Rating
		String rating = metaData.get("my_rating");
		if(rating == null || rating.contentEquals("")){
			rating = "0";
		}
		
		return Float.valueOf(rating);
	}
	
}