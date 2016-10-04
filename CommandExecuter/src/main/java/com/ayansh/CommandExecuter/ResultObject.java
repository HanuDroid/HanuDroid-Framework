package com.ayansh.CommandExecuter;

import android.os.Bundle;

/**
 * @author Varun Verma
 *
 */
public class ResultObject {
	
	public enum ResultStatus{
		SUCCESS, FAILED, CANCELLED, UNDEFINED;
	}
	
	protected Command command;
	protected ResultObject.ResultStatus resultStatus;
	private int resultCode;
	protected Exception e;
	boolean commandExecutionStatus;
	protected Bundle data;
	
	public ResultObject(){
		resultStatus = ResultStatus.UNDEFINED;		// Undefined.
		data = new Bundle();
	}
	
	public void setResultCode(ResultObject.ResultStatus resultStatus){
		this.resultStatus = resultStatus;
	}
	
	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}

	public ResultObject.ResultStatus getResultStatus(){
		return resultStatus;
	}
	
	public int getResultCode(){
		return resultCode;
	}
	
	public String getErrorMessage(){
		return e.getMessage();
	}
	
	public Exception getException(){
		return e;
	}
	
	public Command getCommand(){
		return command;
	}
	
	public boolean isCommandExecutionSuccess(){
		return commandExecutionStatus;
	}
	
	public Bundle getData(){
		return data;
	}
	
}