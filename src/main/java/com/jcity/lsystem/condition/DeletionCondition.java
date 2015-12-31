package com.jcity.lsystem.condition;

import com.jcity.lsystem.*;


/**
 * L-System condition. Checks if a road/street segment module should be deleted.
 */
public class DeletionCondition extends Condition {
	/**
	 * Should the RoadModule at index <code>index</code> be deleted?
	 * 
	 * @param index
	 *            the index of the {@link RoadModule} to be checked.
	 * @param mString
	 *            the {@link ModuleString} containing the RoadModule
	 * @return true if the RoadModule should be removed, otherwise false.
	 */
	@Override
	public boolean evaluate(int index, ModuleString mString) {
		boolean b = mString.get(index).status == ModuleStatus.DELETE;
		// logger.debug("Should module " + index + " be deleted? " + b);
		return b;
	}
}
