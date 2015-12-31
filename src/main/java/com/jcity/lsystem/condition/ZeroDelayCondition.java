package com.jcity.lsystem.condition;

import com.jcity.lsystem.*;

public class ZeroDelayCondition extends Condition {
	/**
	 * L-System condition. Checks if a branch module delay is zero.
	 */
	@Override
	public boolean evaluate(int index, ModuleString mString) {
		boolean b = mString.get(index).delay == 0;
		// logger.debug("delay[" + index + "] = " +
		// mString.get(index).delay);
		return b;
	}

}
