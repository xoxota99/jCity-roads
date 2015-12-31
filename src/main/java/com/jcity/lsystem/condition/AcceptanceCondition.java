package com.jcity.lsystem.condition;

import com.jcity.lsystem.*;

public class AcceptanceCondition extends Condition{

	/**
	 * L-System condition. Checks if a road/street segment module was
	 * accepted.
	 */
	@Override
	public boolean evaluate(int index, ModuleString mString) {
		boolean b = mString.get(index).status == ModuleStatus.ACCEPT;
		// logger.debug("Is module " + index + " accepted? " + b);
		return b;
	}
}
