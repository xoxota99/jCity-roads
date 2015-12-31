package com.jcity.lsystem.condition;

import org.apache.log4j.*;

import com.jcity.lsystem.*;

/**
 * Evaluate a Condition to determine if it's true.
 * 
 * @author philippd
 * 
 * @param <T>
 */
public abstract class Condition {
	protected Logger logger = Logger.getLogger(this.getClass());
	protected boolean lookOnlyAtRoads = false;

	public boolean getRoadFlag() {
		return lookOnlyAtRoads;
	}

	public void setRoadFlag(boolean lookOnlyAtRoads) {
		this.lookOnlyAtRoads = lookOnlyAtRoads;
	}

	/**
	 * 
	 * @param index
	 * @param mString
	 * @return
	 */
	public abstract boolean evaluate(int index, ModuleString mString);
}
