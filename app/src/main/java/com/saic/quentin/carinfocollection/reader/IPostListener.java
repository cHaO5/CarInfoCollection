/*
 * TODO put header 
 */
package com.saic.quentin.carinfocollection.reader;

import com.saic.quentin.carinfocollection.reader.io.ObdCommandJob;

/**
 * TODO put description
 */
public interface IPostListener {

	void stateUpdate(ObdCommandJob job);
	
}