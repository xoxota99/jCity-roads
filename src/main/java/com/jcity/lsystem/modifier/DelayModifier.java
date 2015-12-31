package com.jcity.lsystem.modifier;

import com.jcity.lsystem.*;

public class DelayModifier implements Modifier {

	/**
	 * L-System parameter modifier. Reduces delay of a branch module by one.
	 */
	@Override
	public void modify(int index, ModuleString pred, ModuleString succ) {
		// get predecessor module
		RoadModule pModule = pred.get(index);
		// get successor modules
		RoadModule sModule = succ.get(index);

		// ensure successor is a perfect copy of predecessor.
		sModule.set(pModule);
		// decrement delay.
		sModule.delay = pModule.delay - 1;
	}
}
