package com.jcity.lsystem.modifier;

import java.util.*;

import org.apache.log4j.*;

import com.jcity.lsystem.*;
import com.jcity.pipeline.*;
import com.jcity.roads.*;
import com.jcity.roads.pattern.*;

public class RoadSeedModifier extends AbstractModifier {

	private Logger logger = Logger.getLogger(this.getClass());

	private static Map<Class, Pattern> patterns = new HashMap<>();

	public RoadSeedModifier(CityContext ctx, Roadmap roadmap) {
		super(ctx, roadmap);

		patterns.put(ManhattenPattern.class, new ManhattenPattern(ctx));
		patterns.put(SanFranciscoPattern.class, new SanFranciscoPattern(ctx));
		patterns.put(NaturalPattern.class, new NaturalPattern(ctx));
	}

	/**
	 * L-System parameter modifier. Sets initial parameters of branch modules.
	 * That is mainly the direction in which the later to be added road/street
	 * segments should point. Here the current street pattern is used.
	 */
	@Override
	public void modify(int index, ModuleString pred, ModuleString succ) {
		// get predecessor module
		RoadModule pModule = pred.get(index);
		// get successor modules
		RoadModule sModule1 = succ.get(index);
		RoadModule sModule2 = succ.get(index + 1);
		RoadModule sModule3 = succ.get(index + 2);
		// set delay values
		sModule1.delay = ctx.getRoadBranchDelay() + 1;
		sModule2.delay = 0L;
		sModule3.delay = ctx.getRoadBranchDelay();
		// set current vertex id as origin id for successors
		sModule1.vertexId = pModule.vertexId;
		sModule2.vertexId = pModule.vertexId;
		sModule3.vertexId = pModule.vertexId;
		// set type
		if (pModule.roadType == RoadType.ROAD) {
			// If the predecessor RoadModule is a ROAD, see if we need to
			// branch into a street.
			double rval1 = ctx.getRandom().nextDouble();
			double rval2 = ctx.getRandom().nextDouble();

			if (rval1 > ctx.getRoadBranchProbability())
				sModule1.roadType2 = RoadType.STREET;
			else
				sModule1.roadType2 = RoadType.ROAD;

			sModule2.roadType2 = RoadType.ROAD;

			if (rval2 > ctx.getRoadBranchProbability())
				sModule3.roadType2 = RoadType.STREET;
			else
				sModule3.roadType2 = RoadType.ROAD;

		} else { // RoadModule is a STREET. All children must be streets.
			sModule1.roadType = RoadType.STREET;
			sModule2.roadType = RoadType.STREET;
			sModule3.roadType = RoadType.STREET;
		}
		// depending on the road pattern at the current vertex modify
		// direction vectors
		int patternMapVal = ctx.getPatternMap().getValue(roadmap.getVertex(pModule.vertexId).x,
				roadmap.getVertex(pModule.vertexId).y);
		// pre init
		Pattern p = patterns.get(NaturalPattern.class); // Avoid creating /
														// tearing down an
														// instance for
														// every module.

		// Streets are always sensitive. If we want roads to be sensitive as
		// well, we set that in config.
		boolean sensitive = ctx.isRoadPatternSensitive();
		if (!sensitive) {
			// If we're not sensitive, then none of these patterns will
			// work. So run Natural pattern once. Natural pattern follows
			// population, and ignores sensitivity.
			p.apply(roadmap, index, pred, succ);
		}

		// TODO: Allow interpolation between multiple patterns, by patternMapVal
		if (patternMapVal == ctx.getManhattenPatternColorKey()) {
			p = patterns.get(ManhattenPattern.class);
			logger.debug("manhattan ("+patternMapVal+")");
		} else if (patternMapVal == ctx.getSanFranciscoPatternColorKey()) {
			p = patterns.get(SanFranciscoPattern.class);
			logger.debug("sanfrancisco ("+patternMapVal+")");
		} else {
			p = patterns.get(NaturalPattern.class); // WTF: natural pattern
													// again?
			logger.debug("natural ("+patternMapVal+")");
		}

		// Second pass. update with local pattern.
		p.apply(roadmap, index, pred, succ);
	}
}
