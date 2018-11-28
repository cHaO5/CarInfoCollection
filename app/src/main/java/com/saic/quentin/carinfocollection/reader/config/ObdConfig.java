/*
 * TODO put header
 */
package com.saic.quentin.carinfocollection.reader.config;

import java.util.ArrayList;

import com.saic.quentin.carinfocollection.commands.ObdCommand;
import com.saic.quentin.carinfocollection.commands.SpeedObdCommand;
import com.saic.quentin.carinfocollection.commands.control.CommandEquivRatioObdCommand;
import com.saic.quentin.carinfocollection.commands.control.DtcNumberObdCommand;
import com.saic.quentin.carinfocollection.commands.control.TimingAdvanceObdCommand;
import com.saic.quentin.carinfocollection.commands.control.TroubleCodesObdCommand;
import com.saic.quentin.carinfocollection.commands.engine.EngineLoadObdCommand;
import com.saic.quentin.carinfocollection.commands.engine.EngineRPMObdCommand;
import com.saic.quentin.carinfocollection.commands.engine.EngineRuntimeObdCommand;
import com.saic.quentin.carinfocollection.commands.engine.MassAirFlowObdCommand;
import com.saic.quentin.carinfocollection.commands.engine.ThrottlePositionObdCommand;
//import com.saic.quentin.carinfocollection.commands.fuel.FindFuelTypeObdCommand;
//import com.saic.quentin.carinfocollection.commands.fuel.FuelLevelObdCommand;
//import com.saic.quentin.carinfocollection.commands.fuel.FuelTrimObdCommand;
//import com.saic.quentin.carinfocollection.commands.pressure.BarometricPressureObdCommand;
//import com.saic.quentin.carinfocollection.commands.pressure.FuelPressureObdCommand;
//import com.saic.quentin.carinfocollection.commands.pressure.IntakeManifoldPressureObdCommand;
import com.saic.quentin.carinfocollection.commands.protocol.ObdResetCommand;
//import com.saic.quentin.carinfocollection.commands.temperature.AirIntakeTemperatureObdCommand;
//import com.saic.quentin.carinfocollection.commands.temperature.AmbientAirTemperatureObdCommand;
//import com.saic.quentin.carinfocollection.commands.temperature.EngineCoolantTemperatureObdCommand;
//import com.saic.quentin.carinfocollection.enums.FuelTrim;

/**
 * TODO put description
 */
public final class ObdConfig {

	public static ArrayList<ObdCommand> getCommands() {
		ArrayList<ObdCommand> cmds = new ArrayList<ObdCommand>();
		// Protocol
		cmds.add(new ObdResetCommand());

		// Control
//		cmds.add(new CommandEquivRatioObdCommand());
//		cmds.add(new DtcNumberObdCommand());
//		cmds.add(new TimingAdvanceObdCommand());
//		cmds.add(new TroubleCodesObdCommand(0));

		// Engine
//		cmds.add(new EngineLoadObdCommand());
		cmds.add(new EngineRPMObdCommand());
//		cmds.add(new EngineRuntimeObdCommand());
//		cmds.add(new MassAirFlowObdCommand());

		// Fuel
		// cmds.add(new AverageFuelEconomyObdCommand());
		// cmds.add(new FuelEconomyObdCommand());
		// cmds.add(new FuelEconomyMAPObdCommand());
		// cmds.add(new FuelEconomyCommandedMAPObdCommand());
//		cmds.add(new FindFuelTypeObdCommand());
//		cmds.add(new FuelLevelObdCommand());
//		cmds.add(new FuelTrimObdCommand(FuelTrim.LONG_TERM_BANK_1));
//		cmds.add(new FuelTrimObdCommand(FuelTrim.LONG_TERM_BANK_2));
//		cmds.add(new FuelTrimObdCommand(FuelTrim.SHORT_TERM_BANK_1));
//		cmds.add(new FuelTrimObdCommand(FuelTrim.SHORT_TERM_BANK_2));
//
//		// Pressure
//		cmds.add(new BarometricPressureObdCommand());
//		cmds.add(new FuelPressureObdCommand());
//		cmds.add(new IntakeManifoldPressureObdCommand());
//
//		// Temperature
//		cmds.add(new AirIntakeTemperatureObdCommand());
//		cmds.add(new AmbientAirTemperatureObdCommand());
//		cmds.add(new EngineCoolantTemperatureObdCommand());

		// Misc
		cmds.add(new SpeedObdCommand());
//		cmds.add(new ThrottlePositionObdCommand());

		return cmds;
	}

}