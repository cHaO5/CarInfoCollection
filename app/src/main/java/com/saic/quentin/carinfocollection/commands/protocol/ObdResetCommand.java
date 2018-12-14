/*
 * TODO put header 
 */
package com.saic.quentin.carinfocollection.commands.protocol;

import java.io.IOException;
import java.io.InputStream;

import com.saic.quentin.carinfocollection.commands.ObdCommand;

/**
 * This method will reset the OBD connection.
 */
public class ObdResetCommand extends ObdCommand {

	public ObdResetCommand() {
		super("AT Z");
	}

	/**
	 * @param other
	 */
	public ObdResetCommand(ObdResetCommand other) {
		super(other);
	}

	/**
	 * Reset command returns an empty string, so we must override the following
	 * two methods.
	 * @throws java.io.IOException
	 */
	@Override
	public void readResult(InputStream in) throws IOException {
		// do nothing
		return;
	}

	@Override
	public String getResult() {
		return "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 */
	@Override
	public String getFormattedResult() {
		return getResult();
	}

	@Override
	public String getName() {
		return "Reset OBD";
	}

}