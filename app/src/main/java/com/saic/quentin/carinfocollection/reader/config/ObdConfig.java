
package com.saic.quentin.carinfocollection.reader.config;

import java.util.ArrayList;

import com.saic.quentin.carinfocollection.commands.ObdCommand;
import com.saic.quentin.carinfocollection.commands.SpeedObdCommand;
import com.saic.quentin.carinfocollection.commands.EngineRPMObdCommand;
import com.saic.quentin.carinfocollection.commands.protocol.ObdResetCommand;

public final class ObdConfig {

	public static ArrayList<ObdCommand> getCommands() {
		ArrayList<ObdCommand> cmds = new ArrayList<ObdCommand>();
		cmds.add(new ObdResetCommand());
		cmds.add(new EngineRPMObdCommand());
		cmds.add(new SpeedObdCommand());

		return cmds;
	}

}