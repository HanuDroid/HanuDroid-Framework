package com.ayansh.hanudroid;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PostXMLParser extends DefaultHandler {

	private Post post;
	private PostComment comment;
	private boolean reading;
	private String content, taxonomy;
	private PostManager pm;
	private SimpleDateFormat df;
	
	
	public PostXMLParser(){
		
		pm = PostManager.getInstance();
		reading = false;
		content = "";
		df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes){
		
		if(localName.contentEquals("PostsInfoRow")){
			// A new post.
			post = new Post();
		}
		
		if(localName.contentEquals("PostData")){
			
			post.Id = Integer.valueOf(attributes.getValue("Id"));
			
			try {
				post.pubDate = df.parse(attributes.getValue("PublishDate"));
				post.modDate = df.parse(attributes.getValue("ModifiedDate"));
			} catch (ParseException e) {
				post.pubDate = new Date();
				post.modDate = new Date();
			}
			
			post.author = attributes.getValue("Author");
			
		}
		
		if(localName.contentEquals("PostTitle")){
			reading = true;
			content = "";
		}
		
		if(localName.contentEquals("PostContent")){
			reading = true;
			content = "";
		}
		
		if(localName.contentEquals("PostMetaDataRow")){
			post.metaData.put(attributes.getValue("MetaKey"), attributes.getValue("MetaValue"));
		}
		
		if(localName.contentEquals("CommentsDataRow")){
			
			comment = new PostComment();
			comment.commentId = Integer.valueOf(attributes.getValue("CommentId"));
			comment.postId = Integer.valueOf(attributes.getValue("PostId"));
			comment.author = attributes.getValue("Author");
			comment.authorEmail = attributes.getValue("AuthorEmail");
			
			try {
				comment.commentDate = df.parse(attributes.getValue("CommentDate"));
			} catch (ParseException e) {
				comment.commentDate = new Date();
			}
			
			comment.commentParent = Integer.valueOf(attributes.getValue("CommentParent"));
			
			post.postComments.add(comment);
			
		}
		
		if(localName.contentEquals("CommentContent")){
			reading = true;
			content = "";
		}
		
		if(localName.contentEquals("TermName")){
			reading = true;
			content = "";
		}
		
		if(localName.contentEquals("TermsDataRow")){
			taxonomy = attributes.getValue("Taxonomy");
		}
		
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException{
		
		if(localName.contentEquals("PostContent")){
			reading = false;
			post.content = content;
		}
		
		if(localName.contentEquals("PostTitle")){
			reading = false;
			post.title = content;
		}
		
		if(localName.contentEquals("CommentContent")){
			reading = false;
			comment.content = content;
		}
		
		if(localName.contentEquals("TermName")){
			reading = false;
			if(taxonomy.contentEquals("category")){
				post.categories.add(content);
			}
			else if (taxonomy.contentEquals("post_tag")){
				post.tags.add(content);
			}
		}
		
		if(localName.contentEquals("PostsInfoRow")){
			pm.downloadPostList.add(post);
		}
		
	}
	
	@Override
	public void characters(char[] ch, int start, int length){
		if (reading){
			
			int i = 0;
			int index = start;
			do{
				content = content + ch[index];
				i = i+1;
				index = index + 1;
			}while(i<length);
		}
	}
	
}