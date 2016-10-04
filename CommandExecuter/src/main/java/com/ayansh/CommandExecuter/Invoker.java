package com.ayansh.CommandExecuter;
/**
 * @author Varun Verma
 *
 */
public interface Invoker {
	
	public void NotifyCommandExecuted(ResultObject result);
	public void ProgressUpdate(ProgressInfo progressInfo);

}