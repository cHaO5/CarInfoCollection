/*
 * TODO put header
 */
package com.saic.quentin.carinfocollection.commands.control;

import com.saic.quentin.carinfocollection.commands.PercentageObdCommand;
import com.saic.quentin.carinfocollection.enums.AvailableCommandNames;

/**
 * TODO put description
 * 
 * Timing Advance
 */
public class TimingAdvanceObdCommand extends PercentageObdCommand {

	public TimingAdvanceObdCommand() {
		super("01 0E");
	}

	public TimingAdvanceObdCommand(TimingAdvanceObdCommand other) {
		super(other);
	}

	@Override
	public String getName() {
		return AvailableCommandNames.TIMING_ADVANCE.getValue();
	}
}