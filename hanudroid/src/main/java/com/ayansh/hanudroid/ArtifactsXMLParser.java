package com.ayansh.hanudroid;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class ArtifactsXMLParser extends DefaultHandler {

	private PostManager pm;
	private PostArtifact artifact;
	SimpleDateFormat df;
	
	public ArtifactsXMLParser(){
		
		pm = PostManager.getInstance();
		pm.postArtifacts.clear();
		df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes){
		
		if(localName.contentEquals("PostArtifcatData")){
			
			artifact = new PostArtifact();
			
			artifact.postId = Integer.valueOf(attributes.getValue("Id"));
			
			try {
				
				artifact.pubDate = df.parse(attributes.getValue("PublishDate"));
				artifact.modDate = df.parse(attributes.getValue("ModifiedDate"));
				
			} catch (ParseException e) {
				// Error !
				Log.e(Application.TAG, e.getMessage(), e);
			}
			
			try {
				artifact.commDate = df.parse(attributes.getValue("CommentDate"));
			} catch (ParseException e) {
				artifact.commDate = new Date(0);
			}
			
			pm.postArtifacts.add(artifact);
			
		}
		
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException{
		
	}
	
	@Override
	public void characters(char[] ch, int start, int length){
		// Nothing to do :)
	}
	
}