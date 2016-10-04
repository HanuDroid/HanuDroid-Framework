package com.ayansh.CommandExecuter;
/**
 * @author Varun Verma
 *
 */
public abstract class Command {
	
	protected Invoker caller;	// Caller
	CommandExecuter commandExecuter;
	
	public static final Invoker DUMMY_CALLER = new Invoker(){
			@Override
			public void NotifyCommandExecuted(ResultObject result) {}
			@Override
			public void ProgressUpdate(ProgressInfo progressInfo) {}
		};
	
	public Command(Invoker caller){
		this.caller = caller;
	}
	
	protected abstract void execute(ResultObject result) throws Exception;
	
	public final ResultObject execute() {
		
		ResultObject result = new ResultObject();
		result.command = this;
		
		try {
			// Execute the command
			
			execute(result);
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
	
	public final String getCommandName(){
		return this.getClass().getName();
	}
	
	public final void publishProgress(ProgressInfo progress){
		if(commandExecuter != null){
			commandExecuter.PublishProgress(progress);
		}
	}
	
	public final void updateInvoker(Invoker newCaller){
		caller = newCaller;
	}
}