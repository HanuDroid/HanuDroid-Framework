package com.ayansh.CommandExecuter;

import android.os.AsyncTask;

/**
 * @author Varun Verma
 *
 */
public final class CommandExecuter extends AsyncTask<Command, ProgressInfo, ResultObject> {
		
	private Command command;
	
	@Override
	protected ResultObject doInBackground(Command... commands) {
		// Execute the commands here.
		command = commands[0];	// Assumption that only 1 command is passed.
		command.commandExecuter = this;
		
		ResultObject result = new ResultObject();
		result.command = command;
		
		try {
			// Execute the command
			
			// Check if task is cancelled.
			if(isCancelled()){
				result.setResultCode(ResultObject.ResultStatus.CANCELLED);
				return result;
			}
			
			command.execute(result);
			result.commandExecutionStatus = true;
			if(result.resultStatus == ResultObject.ResultStatus.UNDEFINED){
				result.resultStatus = ResultObject.ResultStatus.SUCCESS;
			}
			
		} catch (Exception e) {
			result.e = e;
			result.commandExecutionStatus = false;
			if(result.resultStatus == ResultObject.ResultStatus.UNDEFINED){
				result.resultStatus = ResultObject.ResultStatus.FAILED;
			}
		}
		
		return result;
	}
	
	protected void onPostExecute(ResultObject result){
		// Notify the caller that the command has been executed.
		// Pass the result Object.
		result.command.caller.NotifyCommandExecuted(result);
	}
	
	protected void onProgressUpdate(ProgressInfo... progress) {
		// Update progress Info.
		command.caller.ProgressUpdate(progress[0]);
	}
	
	protected void onCancelled(ResultObject result){
		result.command.caller.NotifyCommandExecuted(result);
	}
	
	void PublishProgress(ProgressInfo progress){
		this.publishProgress(progress);
	}

}