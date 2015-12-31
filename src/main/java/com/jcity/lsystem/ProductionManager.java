package com.jcity.lsystem;

import java.util.*;

import org.apache.log4j.*;

/**
 * Represents a list of Productions to apply.
 */
public class ProductionManager extends ArrayList<Production> {

	/**
	 * @changed
	 */
	private static final long serialVersionUID = -7651918061388923184L;

	private Logger logger = Logger.getLogger(this.getClass());

	/**
	 * Apply this production to the given ModuleString.
	 * 
	 * @param mString
	 *            the ModuleString to apply the production to.
	 * @return true if the ModuleString was modified, otherwise false.
	 */
	public boolean apply(ModuleString mString) {
		// set result to false
		boolean altered = false;
		// make a copy of the work string
		// OR should we use mString.clone()?
		ModuleString mPredString = new ModuleString(mString);//mString.clone();
		
		int initSize = mPredString.size();
		for (int index = mString.size() - 1; index >= 0; index--) {	//index is initialized to current mString size. mString can change.
			for (Production prod : this) {
				// WTF: altered can flip true/false. Which means mString can be modified, and this function may still return false if the last Module in mString was not altered.
				altered = prod.applyAtIndex(index, mPredString, mString);
				if (altered)
					break;
			}
		}
		if (mPredString.size() != initSize) {
			throw new IllegalStateException("WTF, mPredString.size() has changed! was " + initSize + ", now it's " + mPredString.size());
		}

		return altered;

	}
}
