package com.ayansh.hanudroid;

import java.util.Date;

import android.database.DatabaseUtils;

public class PostComment {

	int commentId, commentParent, postId;
	String author, authorEmail, content;
	Date commentDate;

	PostComment(){
		commentParent = 0;
	}

	String insertQuery() {
		// Create Query to Save to DB.
		String query = "";
		String c = DatabaseUtils.sqlEscapeString(content);
		query = "INSERT INTO Comments (CommentId, PostId, Author, AuthorEmail, CommentDate, " +
				"CommentParent, CommentsContent) VALUES (" +
				"'" + commentId + "'," +
				"'" + postId + "'," +
				"'" + author + "'," + 
				"'" + authorEmail + "'," +
				"'" + commentDate.getTime() + "'," +
				"'" + commentParent + "'," +
				"" + c + ")";
		
		return query;
	}

	String UPSertQuery() {
		// Find if comment exits or not...
		if(!Application.getApplicationInstance().appDB.commentExists(commentId)){
			return insertQuery();
		}
		return null;
	}

	public String getAuthor() {
		return author;
	}
	
	public Date getCommentDate(){
		return commentDate;
	}
	
	public String getContent(){
		return content;
	}
	
	public int getCommentParent(){
		return commentParent;
	}
	
}