/*
 * TODO put header
 */
package com.saic.quentin.carinfocollection.commands.engine;

import com.saic.quentin.carinfocollection.commands.ObdCommand;
import com.saic.quentin.carinfocollection.enums.AvailableCommandNames;

/**
 * TODO put description
 * 
 * Mass Air Flow
 */
public class MassAirFlowObdCommand extends ObdCommand {

	private float _maf = -1.0f;

	/**
	 * Default ctor.
	 */
	public MassAirFlowObdCommand() {
		super("01 10");
	}

	/**
	 * Copy ctor.
	 * 
	 * @param other
	 */
	public MassAirFlowObdCommand(MassAirFlowObdCommand other) {
		super(other);
	}

	/**
	 * 
	 */
	@Override
	public String getFormattedResult() {
		if (!"NODATA".equals(getResult())) {
			// ignore first two bytes [hh hh] of the response
			int a = buffer.get(2);
			int b = buffer.get(3);
			_maf = (a * 256 + b) / 100.0f;
		}

		return String.format("%.2f%s", _maf, "g/s");
	}

	/**
	 * @return MAF value for further calculus.
	 */
	public double getMAF() {
		return _maf;
	}

	@Override
	public String getName() {
		return AvailableCommandNames.MAF.getValue();
	}
}