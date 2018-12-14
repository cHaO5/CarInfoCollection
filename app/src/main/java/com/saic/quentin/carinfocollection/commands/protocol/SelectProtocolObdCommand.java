
package com.saic.quentin.carinfocollection.commands.protocol;

import com.saic.quentin.carinfocollection.commands.ObdCommand;
import com.saic.quentin.carinfocollection.enums.ObdProtocols;

/**
 * Select the protocol to use.
 */
public class SelectProtocolObdCommand extends ObdCommand {
	
	private final ObdProtocols _protocol;

	/**
	 * @param command
	 */
	public SelectProtocolObdCommand(ObdProtocols protocol) {
		super("AT SP " + protocol.getValue());
		_protocol = protocol;
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
		return "Select Protocol " + _protocol.name();
	}

}