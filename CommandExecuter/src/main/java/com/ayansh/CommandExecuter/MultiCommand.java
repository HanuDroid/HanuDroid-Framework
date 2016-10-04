package com.ayansh.CommandExecuter;
/**
 * @author Varun Verma
 *
 */
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MultiCommand extends Command {

	List<Command> commands;
	
	public MultiCommand(Invoker caller) {

		// Call Super class constructor
		super(caller);
		
		// Initialize an empty list of commands.
		commands = new ArrayList<Command>();
		
	}

	@Override
	protected void execute(ResultObject result) throws Exception {
		// Execute all Commands.
		ProgressInfo progress;
		int percentage, count = 0, size;
		
		Iterator<Command> iterator = commands.listIterator();
		Command command;
		size = commands.size();
		
		while(iterator.hasNext()){
			
			command = iterator.next();
			
			// Check if task is cancelled.
			if(commandExecuter != null){
				if(commandExecuter.isCancelled()){
					result.setResultCode(ResultObject.ResultStatus.CANCELLED);
					return;
				}
				command.commandExecuter = this.commandExecuter;
			}
						
			command.execute(result);
			
			// Publish Progress.
			count++;
			percentage = (int) ((count / (float) size) * 100);
			progress = new ProgressInfo(percentage);
			publishProgress(progress);
			
		}

	}
	
	public void addCommand(Command command){
		// Add command to list of commands.
		commands.add(command);
	}
	
	public void addCommand(MultiCommand command){
		// Add all commands of the multi command to this command.
		commands.addAll(command.commands);
	}
	
	public void clearCommands(){
		commands.clear();
	}

}