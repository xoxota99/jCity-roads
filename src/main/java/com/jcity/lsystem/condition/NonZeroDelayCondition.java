package com.jcity.lsystem.condition;

import com.jcity.lsystem.*;
import com.jcity.roads.*;

/**
 * L-System condition. Checks if a branch module delay is greater than zero.
 */
public class NonZeroDelayCondition extends Condition {

	/**
	 * @return true if the {@link RoadModule}'s delay is greater than zero,
	 *         otherwise false.
	 */
	@Override
	public boolean evaluate(int index, ModuleString mString) {

		// This is the only place where roadScope (lookOnlyAtRoads) is used.
		RoadModule m = mString.get(index);
		// logger.debug("Looking at " + m.roadType2 + ", and our scope is " +
		// scope + " so " + (scope.getValue() <= m.roadType2.getValue() ?
		// "CONSIDER ("+scope.getValue() + "<=" + m.roadType2.getValue()+")" :
		// "IGNORE ("+scope.getValue() + ">" + m.roadType2.getValue()+")"));
		boolean b = ((m.delay > 0) && ((!lookOnlyAtRoads) || m.roadType2 == RoadType.ROAD));
		// only look at ROADS if the scope is ROADS.

		try {
//			logger.debug("delay: " + m.delay + ", inScope:" + ((!lookOnlyAtRoads) || m.roadType2 == RoadType.ROAD) + ", roadType2:" + (m.roadType2==null ? "null": m.roadType2.toString()) + ", result: " + b);
		} catch (Exception x) {
//			logger.error(x);
		}
		return b;
	}
}
