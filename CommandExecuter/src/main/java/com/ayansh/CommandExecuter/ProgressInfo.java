package com.ayansh.CommandExecuter;
/**
 * @author Varun Verma
 *
 */
public class ProgressInfo {

	protected int progressPercentage;
	protected String progressMessage;
	
	public ProgressInfo(){
		
		progressPercentage = 0;
		progressMessage = "";
	}
	
	public ProgressInfo(int percentage){
		progressPercentage = percentage;
		progressMessage = "";
	}
	
	public ProgressInfo(String message){
		progressMessage = message;
		progressPercentage = 0;
	}
	
	public ProgressInfo(int percentage, String message){
		progressPercentage = percentage;
		progressMessage = message;
	}
	
	public int getProgressPercentage(){
		return progressPercentage;
	}

	public String getProgressMessage(){
		return progressMessage;
	}
	
}