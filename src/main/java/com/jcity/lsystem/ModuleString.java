package com.jcity.lsystem;

import java.util.ArrayList;

/**
 * List of {@link RoadModule}s being worked on. Stores a sequence of Modules and
 * methods working with them.
 * 
 * @author phil
 * 
 */
//TODO: Should refer to Module, not RoadModule.
public class ModuleString extends ArrayList<RoadModule> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3272157237831480115L;

	/**
	 * Copy Constructor
	 * 
	 * @param ms
	 */
	public ModuleString(ModuleString mString) {
		addAll(mString);
	}

	public void insertModule(int index, RoadModule m) {
		if (index > size()) {
			add(m);
		} else {
			add(index, m);
		}
	}

	public void insertModule(int index, ModuleString ms) {
		if (index > size()) {
			addAll(ms);
		} else {
			addAll(index, ms);
		}
	}

	/**
	 * Copy constructor that only takes a subsequence of the original string.
	 * 
	 * @param start
	 * @param end
	 * @param moduleString
	 */
	public ModuleString(int start, int end, ModuleString mString) {
		// swap if necessary.
		int s = start < end ? start : end, e = start < end ? end : start;
		// get an iterator and move it to starting index
		for (int i = s; i <= e; i++) {
			// add each wanted module to our module string
			add(mString.get(i));
		}
	}

	public ModuleString() {
		super();
	}

	/**
	 * Compare a module string with another one. Two module strings are
	 * considered equal if he have the same length and each module is equal to
	 * its corresponding partner in the other string.
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof ModuleString) {
			ModuleString ms = (ModuleString) o;
			if (ms.size() == size()) {
				int i = 0;
				for (RoadModule m : ms) {
					if (!m.equals(this.get(i++))) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	public String toString() {
		String retval = "";
		for (RoadModule m : this) {
			retval += m.toString();
		}
		return retval;
	}

	public void replace(int index, ModuleString successor) {
		remove(index);
		addAll(index, successor);
	}

	/**
	 * deep copy. All Modules are copied into new instances.
	 */
	public ModuleString clone() {
		ModuleString retval = new ModuleString();
		for (RoadModule m : this) {
			retval.add(new RoadModule(m));
		}
		return retval;
	}
}
