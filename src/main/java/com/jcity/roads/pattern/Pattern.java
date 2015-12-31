package com.jcity.roads.pattern;

import com.jcity.lsystem.*;
import com.jcity.roads.*;

public interface Pattern {
	public void apply(Roadmap roadmap, int index, ModuleString pred, ModuleString succ);
}
