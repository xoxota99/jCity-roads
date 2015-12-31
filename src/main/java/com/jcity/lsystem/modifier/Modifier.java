package com.jcity.lsystem.modifier;

import com.jcity.lsystem.*;

/**
 * Modify Parameters of a module.
 * 
 * @author philippd
 * 
 * @param <T>
 */
public interface Modifier {
	public void modify(int index, ModuleString pred, ModuleString succ);
}
