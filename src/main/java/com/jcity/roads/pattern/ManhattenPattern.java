package com.jcity.roads.pattern;

import javax.vecmath.*;

import org.apache.log4j.*;

import com.jcity.lsystem.*;
import com.jcity.pipeline.*;
import com.jcity.roads.*;

public class ManhattenPattern extends AbstractPattern {

	public ManhattenPattern(CityContext ctx) {
		super(ctx);
	}

	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Follows "grid" pattern. This results in very "square" blocks (as opposed to rectangles, as with the real Manhattan).
	 */
	@Override
	public void apply(Roadmap roadmap, int index, ModuleString pred, ModuleString succ) {
		// get predecessor module
		RoadModule pModule = pred.get(index);
		// get successor modules
		RoadModule sModule1 = succ.get(index);
		RoadModule sModule2 = succ.get(index + 1);
		RoadModule sModule3 = succ.get(index + 2);
		// calculate normalized base vector, that is the vector from the
		// origin vertex of the predecessor to the current vertex
		Vertex v1 = roadmap.getVertex(pModule.vertexId);
		Vertex v2 = roadmap.getVertex(pModule.originVertexId);
		if(pModule.vertexId == pModule.originVertexId){
			logger.error("vertexIndex == originIndex");
		}
		// logger.debug("vId=" +
		// pModule.vertexId+", ovId="+pModule.originVertexId);
		// logger.debug("Basevec (" + v2.x + "," + v2.y + ")-(" + v1.x + "," +
		// v1.y + ")");
		double x=v1.x - v2.x, y=v1.y - v2.y;
		Vector2d basevec = new Vector2d(x, y);
		basevec.normalize();

		// set direction vectors to values
		// Only apply to streets, or apply to roads if we are sensitive to
		// patterns.
		boolean sensitive = ctx.isRoadPatternSensitive();
		// default. Keep going straight.
		sModule1.direction = new Vector2d(basevec.x, basevec.y);
		sModule2.direction = new Vector2d(basevec.x, basevec.y);
		sModule3.direction = new Vector2d(basevec.x, basevec.y);

		// branching at 90-degree angles.
		if ((sModule1.roadType != RoadType.ROAD) || sensitive) {
//			logger.debug("basevec = (" + basevec.x + "," + basevec.y + "). branching LEFT to " + sModule1.roadType2 + "(" + basevec.y + "," + (-basevec.x) + ")");
			logger.debug("Branch left/right!");
			sModule1.direction = new Vector2d(basevec.y, -basevec.x);
		} else {
			throw new IllegalStateException("Holy Shit! sModule1 has no direction!");
		}
		if ((sModule2.roadType != RoadType.ROAD) || sensitive) {
//			logger.debug("basevec = (" + basevec.x + "," + basevec.y + "). branching STRAIGHT to " + sModule2.roadType2 + "(" + basevec.x + "," + basevec.y + ")");
			logger.debug("Go straight!");
			sModule2.direction = new Vector2d(basevec.x, basevec.y);
		} else {
			throw new IllegalStateException("Holy Shit! sModule2 has no direction!");
		}
		if ((sModule3.roadType != RoadType.ROAD) || sensitive) {
//			logger.debug("basevec = (" + basevec.x + "," + basevec.y + "). branching RIGHT to " + sModule3.roadType2 + "(" + (-basevec.y) + "," + basevec.x + ")");
			logger.debug("Branch right/left!");
			sModule3.direction = new Vector2d(-basevec.y, basevec.x);
		} else {
			throw new IllegalStateException("Holy Shit! sModule3 has no direction!");
		}
	}

}
