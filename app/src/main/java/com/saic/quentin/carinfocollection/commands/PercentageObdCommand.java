
package com.saic.quentin.carinfocollection.commands;

/**
 * Abstract class for percentage commands.
 */
public abstract class PercentageObdCommand extends ObdCommand {

	/**
	 * @param command
	 */
	public PercentageObdCommand(String command) {
		super(command);
	}

	/**
	 * @param other
	 */
	public PercentageObdCommand(ObdCommand other) {
		super(other);
	}

	/**
	 * 
	 */
	@Override
	public String getFormattedResult() {
		String res = getResult();

		if (!"NODATA".equals(res)) {
			// ignore first two bytes [hh hh] of the response
			float tempValue = (buffer.get(2) * 100.0f) / 255.0f;
			res = String.format("%.1f%s", tempValue, "%");
		}

		return res;
	}

}