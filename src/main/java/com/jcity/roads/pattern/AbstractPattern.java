package com.jcity.roads.pattern;

import com.jcity.lsystem.*;
import com.jcity.pipeline.*;
import com.jcity.roads.*;

public class AbstractPattern implements Pattern {
	protected CityContext ctx;

	public AbstractPattern(CityContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public void apply(Roadmap roadmap, int index, ModuleString pred, ModuleString succ) {
		// TODO Auto-generated method stub

	}

}
