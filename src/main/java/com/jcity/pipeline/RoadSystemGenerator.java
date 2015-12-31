package com.jcity.pipeline;

import org.apache.commons.chain.Context;

import com.jcity.roads.Roadmap;
import com.jcity.roads.RoadmapGenerator;

/**
 * Command that generates the system of roads (and curbs).
 * 
 * @author philippd
 * 
 */
public class RoadSystemGenerator extends LogEnabledCommand {

	@Override
	public boolean execute(Context ctx) throws Exception {
		RoadmapContext sctx = (RoadmapContext) ctx;
		RoadmapGenerator rmg = new RoadmapGenerator(sctx);
		Roadmap rm = rmg.execute();
		sctx.setRoadmap(rm);
		sctx.setRoadmapGenerator(rmg);
		return false;
	}

}
