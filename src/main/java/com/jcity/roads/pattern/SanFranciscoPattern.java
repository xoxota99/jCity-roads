package com.jcity.roads.pattern;

import javax.vecmath.*;

import org.apache.log4j.*;

import com.jcity.lsystem.*;
import com.jcity.pipeline.*;
import com.jcity.roads.*;
import com.jcity.util.*;

public class SanFranciscoPattern extends AbstractPattern {
	public SanFranciscoPattern(CityContext ctx) {
		super(ctx);
	}

	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Follows vertical slope. (tries to go around mountains)
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
		if (pModule.vertexId == pModule.originVertexId) {
			throw new IllegalStateException("vertexIndex == originIndex");
		}
		Vector2d basevec = new Vector2d(v1.x - v2.x, v1.y - v2.y);

		basevec.normalize();
		logger.debug("Basevec (" + v2.x + "," + v2.y + ")-(" + v1.x + "," + v1.y + ")");
		// set direction vectors to values.
		sModule1.direction = new Vector2d(basevec.y, -basevec.x);
		sModule2.direction = new Vector2d(basevec.x, basevec.y);
		sModule3.direction = new Vector2d(-basevec.y, basevec.x);

		// adjust to follow gradient
		Point3d base = new Point3d(v1.x, v1.y, 0);
		// Streets are always sensitive. Roads *might* be, depending on config.
		boolean isSensitive = ctx.isRoadPatternSensitive();
		if ((sModule1.roadType2 != RoadType.ROAD) || isSensitive) {
			sModule1.direction = gradientSampler(base, sModule1.direction);
		}

		if ((sModule2.roadType2 != RoadType.ROAD) || isSensitive) {
			sModule2.direction = gradientSampler(base, sModule2.direction);
		}

		if ((sModule3.roadType2 != RoadType.ROAD) || isSensitive) {
			sModule3.direction = gradientSampler(base, sModule3.direction);
		}

	}

	/**
	 * calculate direction with smallest gradient. from a given point.
	 * 
	 * @param base
	 *            origin
	 * @param dir
	 *            direction
	 **/
	private Vector2d gradientSampler(Point3d base, Vector2d dir) {
		// some variables
		Vector2d v = new Vector2d(dir);
		double baseAng = MathUtil.angle(v);
		double minGradient = 999999.0;
		double tempGradient;
		Point3d e = new Point3d();
		Vector2d t = new Vector2d();
		// z1 is z-value at base
		ctx.getHeightMap().getValue(base);

		double terrainMaxHeight = ctx.getTerrainMaxHeight();
		double terrainMinHeight = ctx.getTerrainMinHeight();
		long sRate = ctx.getRoadSamplingRate();
		double tAngle = ctx.getRoadMaxTurningAngle();
		double segLength = ctx.getRoadSegmentLength();

		base.z = (float) (base.z * (terrainMaxHeight - terrainMinHeight));
		// search for direction with minimal gradient
		for (long i = -sRate; i < sRate; i++) {
			// calculate new direction
			MathUtil.rotateNormalizedVector(baseAng + i * (tAngle / (double) sRate), v);
			// calculate end point and new density sum
			e = MathUtil.calculatePoint(segLength, base, t);
			// ez is z-value at calculated point
			ctx.getHeightMap().getValue(e);
			e.z = (float) (e.z * (terrainMaxHeight - terrainMinHeight));
			// calculate gradient
			tempGradient = MathUtil.gradient(base, e);
			// scale sum
			if (i == 0)
				tempGradient /= 1.2;
			// if calculated gradient is smaller than minGradient we've found a
			// new direction
			if (tempGradient < minGradient) {
				v.x = t.x;
				v.y = t.y;
				minGradient = tempGradient;
			}
		}
		return v;
	}
}
