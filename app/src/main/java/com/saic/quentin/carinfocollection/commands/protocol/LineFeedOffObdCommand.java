/*
 * TODO put header
 */
package com.saic.quentin.carinfocollection.commands.protocol;

import com.saic.quentin.carinfocollection.commands.ObdCommand;

/**
 * Turns off line-feed.
 */
public class LineFeedOffObdCommand extends ObdCommand {

	/**
	 * @param command
	 */
	public LineFeedOffObdCommand() {
		super("AT L0");
	}

	/**
	 * @param other
	 */
	public LineFeedOffObdCommand(ObdCommand other) {
		super(other);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.lighthouselabs.MyCommand.commands.ObdCommand#getFormattedResult()
	 */
	@Override
	public String getFormattedResult() {
		return getResult();
	}

	@Override
	public String getName() {
		return "Line Feed Off";
	}

}