/*
 * TODO put header
 */
package com.saic.quentin.carinfocollection.commands.engine;

import com.saic.quentin.carinfocollection.commands.ObdCommand;
import com.saic.quentin.carinfocollection.commands.PercentageObdCommand;
import com.saic.quentin.carinfocollection.enums.AvailableCommandNames;

/**
 * Calculated Engine Load value.
 */
public class EngineLoadObdCommand extends PercentageObdCommand {

	/**
	 * @param command
	 */
	public EngineLoadObdCommand() {
		super("01 04");
	}

	/**
	 * @param other
	 */
	public EngineLoadObdCommand(ObdCommand other) {
		super(other);
	}

	/* (non-Javadoc)
	 * @see eu.lighthouselabs.MyCommand.commands.ObdCommand#getName()
	 */
	@Override
	public String getName() {
		return AvailableCommandNames.ENGINE_LOAD.getValue();
	}

}