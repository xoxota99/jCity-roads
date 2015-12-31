package com.jcity.roads.pattern;

import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;

import org.apache.log4j.Logger;

import com.jcity.lsystem.ModuleString;
import com.jcity.lsystem.RoadModule;
import com.jcity.pipeline.CityContext;
import com.jcity.roads.Roadmap;
import com.jcity.roads.Vertex;
import com.jcity.util.MathUtil;

public class NaturalPattern extends AbstractPattern {

	private Logger logger = Logger.getLogger(this.getClass());

	public NaturalPattern(CityContext ctx) {
		super(ctx);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Follows population.
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
		Vertex v1 = roadmap.getVertex((int) pModule.vertexId);
		Vertex v2 = roadmap.getVertex((int) pModule.originVertexId);
		Vector2d basevec = new Vector2d(v1.x - v2.x, v1.y - v2.y);
		if (pModule.vertexId == pModule.originVertexId) {
			logger.error("vertexIndex == originIndex");
		}
		basevec.normalize();
		// set direction vectors to values.
		sModule1.direction = new Vector2d(basevec.y, -basevec.x);
		sModule2.direction = new Vector2d(basevec.x, basevec.y);
		sModule3.direction = new Vector2d(-basevec.y, basevec.x);

		// adjust to follow population density
		Point3d base = new Point3d(v1.x, v1.y, 0);
		sModule1.direction = sampleDensity(base, sModule1.direction);
		sModule2.direction = sampleDensity(base, sModule2.direction);
		sModule3.direction = sampleDensity(base, sModule3.direction);
	}

	/**
	 * calculate direction with largest accumulated population density.
	 * Essentially: We will cast roadSampleRate*2 rays in a "fan" formation,
	 * starting at 0-roadMaxTurningAngle, ending at roadMaxTurningAngle. Each ray is roadSamplingLength units long. Then, along each ray, we sample the population density in even distances, gathering roadSampleRate samples, and summing them.
	 * 
	 * The ray with the highest total population density will "win", and the road will continue in that direction.
	 * 
	 * The road will bias in favor of moving in the current direction (straight ahead) by a factor of 1.2.
	 * 
	 * 
	 * @param base
	 *            origin - the starting point of the evaluation.
	 * @param dir
	 *            direction - the current direction ("center" of the "fan").
	 **/
	private Vector2d sampleDensity(Point3d base, Vector2d dir) {
		// some variables
		Vector2d retval = new Vector2d(dir);
		double baseAng = MathUtil.angle(retval);
		double densitySum = 0.0;
		double density, tempDensitySum;
		Point3d e = new Point3d();
		Vector2d t = new Vector2d();
		// search for direction with largest accumulated density (angle)
		long sRate = ctx.getRoadSamplingRate();
		double tAngle = ctx.getRoadMaxTurningAngle();
		double sLength = ctx.getRoadSamplingLength();
		double popDense = ctx.getDefaultPopulationDensity();
		for (long i = -sRate; i < sRate; i++) {
			tempDensitySum = 0.0;
			// calculate new direction
			MathUtil.rotateNormalizedVector(baseAng + i * (tAngle / (double) sRate), t);
			// search for direction with largest accumulated density (length)
			for (int j = 0; j < sRate; j++) {
				// calculate end point and new density sum
				e = MathUtil.calculatePoint((long) (j * sLength), base, t);
				density = ctx.getDensityMap().getValue(e);
				if (density > 0D)
					tempDensitySum += density;
				else
					tempDensitySum += popDense;
			}
			// scale sum
			if (i == 0)
				tempDensitySum *= 1.2; // represents a tendency to move forward
										// (in a straight line), regardless of
										// population density.

			// if calculated density sum is larger than previous one we have
			// found a new direction
			if (tempDensitySum > densitySum) {
				retval.x = t.x;
				retval.y = t.y;
				densitySum = tempDensitySum;
			}
		}
		return retval;
	}

}
