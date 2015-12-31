package com.jcity.lsystem.modifier;

import com.jcity.lsystem.*;
import com.jcity.pipeline.*;
import com.jcity.roads.*;

public abstract class AbstractModifier implements Modifier {

	protected CityContext ctx;
	protected Roadmap roadmap;

	public AbstractModifier(CityContext ctx, Roadmap roadmap) {
		this.ctx = ctx;
		this.roadmap = roadmap;
	}

	@Override
	public abstract void modify(int index, ModuleString pred, ModuleString succ);

}
