package com.ayansh.hanudroid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import android.util.Log;

public class PostManager {

	private Application app;
	private static PostManager pm;
	
	List<PostArtifact> postArtifacts;				// Post artifacts from Internet
	List<Post> downloadPostList;					// List downloaded from Internet 
	
	HashMap<Integer, PostArtifact> dbPostArtifacts;	// Current DB Post Artifacts
	private List<Post> postList;					// List selected from DB.
	private HashMap<Integer,Post> postMap;			// Map of Post ID <-> Post
	
	static PostManager getInstance(){
		
		if(pm == null){
			pm = new PostManager();
		}
		
		return pm;
	}
	
	private PostManager(){
		
		postArtifacts = new ArrayList<PostArtifact>();
		downloadPostList = new ArrayList<Post>();
		postList = new ArrayList<Post>();
		dbPostArtifacts = new HashMap<Integer, PostArtifact>();
		app = Application.getApplicationInstance();
		postMap = new HashMap<Integer,Post>();
		
	}
	
	void clearDBPostList(){
		postList.clear();
		postMap.clear();
	}
	
	void addPostToDBList(Post post){
		postList.add(post);
		postMap.put(post.Id, post);
	}
	
	List<Post> getDBPostList(){	
		return postList;
	}
	
	List<Post> filterPostList(String taxonomy, String name){
		
		postList.clear();
		
		if(taxonomy == null || name == null){
			postList.addAll(postMap.values());
		}
		else{
			
			Iterator<Post> i = postMap.values().iterator();
			while(i.hasNext()){
				
				Post post = i.next();
				
				if(taxonomy.contentEquals("author")){
					
					if(post.author.contentEquals(name)){
						postList.add(post);
					}
					
				}
				
				else if(taxonomy.contentEquals("category")){
					
					if(post.categories.contains(name)){
						postList.add(post);
					}
					
				}
				
				else if(taxonomy.contentEquals("post_tag")){
					
					if(post.tags.contains(name)){
						postList.add(post);
					}
					
				}
				
			}
		}
		
		Collections.sort(postList, Post.SortByPubDate);
		
		return postList;
	}
	
	Post getPostById(int postId){
		return postMap.get(postId);
	}
	
	boolean savePostsToDB() {
		// Save Posts to DB
		ListIterator<Post> iterator = downloadPostList.listIterator();
		Post post;
		boolean allSuccess = true;
		Log.v(Application.TAG, "Saving posts data to DB.");
		
		while(iterator.hasNext()){
			
			post = iterator.next();
			try {
				post.saveToDB();
			} catch (Exception e) {
				allSuccess = false;
				Log.w(Application.TAG, "Following error occured while saving post data to DB:");
				Log.e(Application.TAG, e.getMessage(), e);
			}
			
		}
		
		downloadPostList.clear();
		return allSuccess;
		
	}

	void filterArtifactsForDownload() {
		/*
		 * Filter Artifacts for download.
		 * 
		 * 1. The ones that are up to date need not be downloaded. 
		 */
		
		//
		Log.v(Application.TAG, "Filter of artifacts started...");
		String postIds = "";
		PostArtifact artifact, dbArtifact;
		ListIterator<PostArtifact> iterator = postArtifacts.listIterator();
		
		while(iterator.hasNext()){
			
			artifact = iterator.next();
			
			if(iterator.nextIndex() == 1){
				// This is first Index
				postIds = String.valueOf(artifact.postId);
			}
			else{
				postIds = postIds + "," + artifact.postId;
			}	
			
		}
		
		// Get Post Artifacts from DB.
		app.appDB.loadPostArtifacts(postIds);
		
		// Loop and filter.
		iterator = postArtifacts.listIterator(0);
		while(iterator.hasNext()){
			
			artifact = iterator.next();
			
			if(dbPostArtifacts.containsKey(artifact.postId)){
				
				dbArtifact = dbPostArtifacts.get(artifact.postId);
				
				if(artifact.modDate.compareTo(dbArtifact.modDate) <= 0 &&
					artifact.commDate.compareTo(dbArtifact.commDate) <= 0){
					// Remove. We have the latest.
					iterator.remove();
				}				
			}
		}
		
		Log.v(Application.TAG, "Artifacts filtered.");
	}

	void performSearch(String query) {
		// Perform Search
		ArrayList<Integer> postIds = ApplicationDB.getInstance().performSearch(query);
		
		postList.clear();
		
		Iterator<Integer> i = postIds.listIterator();
		while(i.hasNext()){
			
			int postId = i.next();
			Post post = postMap.get(postId);
			
			if(!postList.contains(post)){
				postList.add(post);
			}
			
		}
		
		Collections.sort(postList, Post.SortByPubDate);
		
	}

	void deletePost(int postId) {
		// Delete Post
		ArrayList<String> queries = new ArrayList<String>();
		
		String query = "DELETE FROM Post WHERE Id = " + postId;
		queries.add(query);
		
		query = "DELETE FROM PostMeta WHERE PostId = " + postId;
		queries.add(query);
		
		query = "DELETE FROM Comments WHERE PostId = " + postId;
		queries.add(query);
		
		query = "DELETE FROM Terms WHERE PostId = " + postId;
		queries.add(query);
		
		ApplicationDB.getInstance().executeQueries(queries);
		
		postList.remove(postMap.get(postId));
		postMap.remove(postId);
		dbPostArtifacts.remove(postId);
		
	}

	public Collection<String> getTitlesOfNewPosts() {
		
		ArrayList<String> list = new ArrayList<String>();
		
		ListIterator<Post> iterator = downloadPostList.listIterator();
		Post post;
		
		while(iterator.hasNext()){
			
			post = iterator.next();
			list.add(post.getTitle());
			
		}
		
		return list;
	}
	
}