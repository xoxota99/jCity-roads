package com.jcity.pipeline;

import com.jcity.roads.*;

@SuppressWarnings("serial")
public class RoadmapContext extends CityContext {

	private Roadmap roadmap;
	private RoadmapGenerator roadmapGenerator;

	public Roadmap getRoadmap() {
		return roadmap;
	}

	public void setRoadmap(Roadmap roadmap) {
		this.roadmap = roadmap;
	}

	public RoadmapGenerator getRoadmapGenerator() {
		return roadmapGenerator;
	}

	public void setRoadmapGenerator(RoadmapGenerator roadmapGenerator) {
		this.roadmapGenerator = roadmapGenerator;
	}

}
