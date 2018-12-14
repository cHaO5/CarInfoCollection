/*
 * TODO put header 
 */
package com.saic.quentin.carinfocollection.reader;

import com.saic.quentin.carinfocollection.reader.IPostListener;
import com.saic.quentin.carinfocollection.reader.io.ObdCommandJob;


public interface IPostMonitor {
	void setListener(IPostListener callback);

	boolean isRunning();

	void executeQueue();
	
	void addJobToQueue(ObdCommandJob job);
}