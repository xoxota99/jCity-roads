package com.jcity.lsystem.modifier;

import java.awt.*;
import java.awt.image.*;
import java.util.List;

import javax.vecmath.*;

import org.apache.log4j.*;

import com.jcity.lsystem.*;
import com.jcity.pipeline.*;
import com.jcity.roads.*;
import com.jcity.util.*;

public class RoadCreatorModifier extends AbstractModifier {

	private Logger logger = Logger.getLogger(this.getClass());
	private int discardedDueToMaxEdges;
	private int discardedDueToSectorOccupied;
	private int discardedDueToNoAdjustment;
	private Graphics2D g;

	public RoadCreatorModifier(CityContext ctx, Roadmap roadmap, Graphics2D g) {
		super(ctx, roadmap);
		this.g = g;
	}

	/**
	 * L-System parameter modifier. Converts branch module to a road/street and
	 * adds a segment to the road map. Several things have to be done and
	 * checked here:
	 * <ul>
	 * <li>Parameter adaption</li>
	 * <li>Intersection with other edges</li>
	 * <li>Merge with nearby vertices</li>
	 * </ul>
	 */
	@Override
	public void modify(int index, ModuleString pred, ModuleString succ) {
		// get predecessor module
		RoadModule pModule = pred.get(index);
		// get successor modules
		RoadModule sModule = succ.get(index);
		if (sModule == pModule) {
			throw new IllegalStateException("successor == predecessor! WTF?");
		}
		// some variables
		int originIndex = pModule.vertexId;
		Vector2d dir;
		if (pModule.direction == null) {
			// throw new
			// IllegalArgumentException("pModule direction is null, WTF?");
			Vertex from = roadmap.getVertex(pModule.originVertexId);
			Vertex to = roadmap.getVertex(pModule.vertexId);
			pModule.direction = new Vector2d(to.x - from.x, to.y - from.y);
			// pModule.direction.normalize();
			logger.warn("FIXED pModule direction. " + pModule.direction);
		}
		dir = new Vector2d(pModule.direction);
		RoadType segType = pModule.roadType2;
		// set default value
		sModule.originVertexId = originIndex;
		sModule.delay = originIndex; // WTF: Setting delay to the
										// originIndex?
		sModule.roadType = segType; // Just use the same road type. Never
									// change.
		sModule.status = ModuleStatus.DELETE;
		// some variables
		int vertexIndex = -1;
		int edgeIndex = -1;
		// discard segment if origin vertex has already maximum number of
		// edges attached

		if (roadmap.getAttachedEdgeCount(originIndex) >= ctx.getRoadMaxBranches()) {
			discardedDueToMaxEdges++;
			return;
		}

		// discard segment if sector is already occupied
		if (sectorOccupied(originIndex, dir)) {
			discardedDueToSectorOccupied++;
			return;
		}

		// get position of origin vertex
		Point3d startPos = new Point3d(roadmap.getVertex(originIndex));
		// calculate position of end vertex
		Point3d endPos = new Point3d();
		
		// generates a road segment of length [(ROAD_SEGMENT_LENGTH*0.8)..ROAD_SEGMENT_LENGTH]
		
		double segLength = (ctx.getRoadSegmentLength() * 0.8)
				+ (ctx.getRandom().nextDouble() * ctx.getRoadSegmentLength() * 0.2);

//		double segLength=ctx.getRoadSegmentLength();
		
		endPos = MathUtil.calculatePoint(segLength, startPos, dir);
		endPos.z = ctx.getHeightMap().getInterpolatedValue(endPos.x, endPos.y);
		endPos.z = (float) (endPos.z * (ctx.getTerrainMaxHeight() - ctx.getTerrainMinHeight()));
		// adjust parameters until segment is in legal area
		// if unsuccessful exit
		boolean lookAtDensity = ((segType == RoadType.STREET) || ctx.isRoadDensitySensitive());
		if (!adjustSegment(startPos, dir, endPos, lookAtDensity)) {
			discardedDueToNoAdjustment++;
			return;
		}
		// look if segment can be attached to a nearby vertex
		Point3d endPosV = new Point3d(endPos);
		vertexIndex = attachToNearbyVertex(originIndex, endPosV);

		if (vertexIndex == originIndex) {
			throw new IllegalStateException("vertexIndex == originIndex");
		}
		// look if segment intersects with existing edges
		Point3d endPosE = new Point3d(endPos);
		if (vertexIndex != -1) {
			endPosE.x = endPosV.x;
			endPosE.y = endPosV.y;
			endPosE.z = endPosV.z;
		}
		edgeIndex = intersectWithOtherEdges(originIndex, dir, endPosE);
		// adjust roadmap
		if (vertexIndex == -1) {
			if (edgeIndex == -1) {
				// no nearby vertex or intersection
				// simply add a new vertex and an edge
				addVertexAndEdge(originIndex, endPos, segType, sModule);
			} else {
				// no nearby vertex, but an intersection with another edge.
				// add an intersection
				addIntersection(originIndex, edgeIndex, endPosE, segType, sModule);
			}
		} else {
			if (vertexIndex == originIndex) {
				throw new IllegalStateException(
						"vertexIndex == originIndex! (" + vertexIndex + " == " + originIndex + ")");
			}

			if (edgeIndex == -1) {
				// nearby vertex, but no intersection
				// add edge to nearby vertex
				addEdge(originIndex, vertexIndex, segType, sModule);
			} else {
				// nearby vertex and an intersection
				double distanceV = MathUtil.distanceXY(startPos, endPosV);
				double distanceE = MathUtil.distanceXY(startPos, endPosE);
				if (distanceE <= distanceV) {
					// add edge to nearby vertex
					addEdge(originIndex, vertexIndex, segType, sModule);
				} else {
					// add an intersection
					addIntersection(originIndex, edgeIndex, endPosE, segType, sModule);
				}
			}
		}
		if (g != null) {
			BufferedImage buff = roadmap.toImage((int) ctx.getResolutionX(), (int) ctx.getResolutionY(),
					ctx.getHeightMap());
			g.drawImage(buff, 0, 0, (int) (ctx.getResolutionX() * 0.5), (int) (ctx.getResolutionY() * 0.5), 0, 0,
					(int) ctx.getResolutionX(), (int) ctx.getResolutionY(), null);
		}
	}

	/**
	 * Adapts road/street segment's direction and end point to fulfill all
	 * parameters. That is:
	 * <ul>
	 * <li>End point is in bounds of generated city.</li>
	 * <li>End point is in populated area with some exceptions.</li>
	 * <li>End point is on land.
	 * <li>Road/street segment's gradient is not to steep.</li>
	 * <li>End point is not in a blocked area.</li>
	 * </ul>
	 * 
	 * @param start
	 *            source vertex
	 * @param dir
	 *            segment direction
	 * @param end
	 *            end point
	 * @param lookAtDensity
	 *            consider population density
	 * @return true if segment was adapted, else false
	 **/
	private boolean adjustSegment(Point3d start, Vector2d dir, Point3d end, boolean lookAtDensity) {
		// some variables
		double baseAng = MathUtil.angle(dir);
		double curSegShorten = 1.0;
		double curSegAng = baseAng;
		int shStep = 0;
		int anStep = 0;
		double deltaShorten = (1.0 - ctx.getRoadMaxShortening()) / ctx.getRoadSamplingRate();
		double deltaAngle = ctx.getRoadSamplingRate() / (double) ctx.getRoadSamplingRate();
		// conditions if segment is legal
		// end point is in bounds?
		boolean inBounds = (end.x >= 0.0) && (end.x <= ctx.getResolutionX()) && (end.y >= 0.0)
				&& (end.y <= ctx.getResolutionY());
		// in a populated area?
		boolean populated = ctx.getDensityMap().getInterpolatedValue(end) > 0.0;
		// on land?
		boolean onLand = ctx.getWaterMap().getValue(end) != 255;
		// segment not too steep?
		boolean notSteep = (MathUtil.gradient(start, end) <= ctx.getRoadMaxGradient());
		// Not blocked?
		boolean notBlocked = ctx.getBlockedMap().getValue(end) != 255;

		boolean isLegal = inBounds && ((!lookAtDensity) || (populated)) && onLand && notSteep && notBlocked;

		// while segment is not legal
		while (!isLegal) {
			// adjust sh(orten)Step and an(gle)Step
			if (anStep > ctx.getRoadSamplingRate())
				return false;
			if (shStep < ctx.getRoadSamplingRate())
				shStep++;
			else {
				shStep = 0;
				if (anStep == 0)
					anStep = 1;
				else {
					if (anStep > 0) {
						anStep = -anStep;
					} else {
						anStep = -anStep + 1;
					}
				}
			}

			// calculate shortening and angle
			curSegShorten = 1.0 - shStep * deltaShorten;
			curSegAng = baseAng + anStep * deltaAngle;

			MathUtil.rotateNormalizedVector(curSegAng, dir);

			// recalculate end point
			// logger.debug("Finding end based on segLength=" +
			// (ctx.getRoadSegmentLength() * curSegShorten));
			end = MathUtil.calculatePoint(ctx.getRoadSegmentLength() * curSegShorten, start, dir);
			end.z = ctx.getHeightMap().getValue(end);
			end.z = (float) (end.z * (ctx.getTerrainMaxHeight() - ctx.getTerrainMinHeight()));

			// recalculate condition s
			// end point is in bounds?
			inBounds = (end.x >= 0.0) && (end.x <= ctx.getResolutionX()) && (end.y >= 0.0)
					&& (end.y <= ctx.getResolutionY());
			// in a populated area?
			populated = ctx.getDensityMap().getValue(end) > 0.0;
			// on land?
			onLand = ctx.getWaterMap().getValue(end) != 255;
			// segment not too steep?
			notSteep = (MathUtil.gradient(start, end) <= ctx.getRoadMaxGradient());
			// Not bocked?
			notBlocked = ctx.getBlockedMap().getValue(end) != 255;

			isLegal = inBounds && ((!lookAtDensity) || (populated)) && onLand && notSteep && notBlocked;
		}
		return true;
	}

	/**
	 * Looks if a road/street segment is attached to a vertex in the given
	 * direction
	 * 
	 * @param originIndex
	 *            origin vertex
	 * @param dirX
	 *            direction vector x component
	 * @param dirY
	 *            direction vector y component
	 * @return true if a segment is attached in the given direction, else false
	 **/
	private boolean sectorOccupied(int originIndex, Vector2d dir) {
		List<Integer> attachedEdges = roadmap.attachedEdges.get(originIndex);
		for (int i = 0; i < (int) attachedEdges.size(); i++) {
			Edge edge = roadmap.edges.get(attachedEdges.get(i));
			Point3d start = roadmap.getVertex(originIndex);
			Point3d end;
			if (originIndex == edge.vertices.getFirst()) {
				end = roadmap.getVertex(edge.vertices.getSecond());
			} else {
				end = roadmap.getVertex(edge.vertices.getFirst());
			}
			Vector2d edgeDir = new Vector2d(end.x - start.x, end.y - start.y);
			edgeDir.normalize();

			double dotProduct = edgeDir.dot(dir);
			if (dotProduct > 0.7)
				return true;
		}
		return false;
	}

	/**
	 * Looks if a new segment's end point is near enough to a vertex so that
	 * both can be merged.
	 * 
	 * @param origin
	 *            origin vertex index
	 * @param end
	 *            end point
	 * @return -1 if end point is not near another vertex, else the index of the
	 *         nearby vertex
	 **/
	private int attachToNearbyVertex(int originIndex, Point3d end) {
		// temporary vertex
		int ivertex = -1;

		double curdist = ctx.getRoadSearchRadius();
		// search for nearby vertices
		List<Integer> nearby = roadmap.searchNearbyVertices(end, ctx.getRoadSearchRadius());
		// search for nearest vertex in the set
		for (int i = 0; i < (int) nearby.size(); i++) {
			Vertex near = roadmap.getVertex(nearby.get(i));
			double dist = MathUtil.distanceXY(end, near);
			// check if vertex is "good"
			boolean branchesC = (roadmap.attachedEdges.get(nearby.get(i)).size() < ctx.getRoadMaxBranches());
			boolean distanceC = (curdist > dist);
			if (branchesC && distanceC && !near.equals(end) && nearby.get(i) != originIndex) {
				ivertex = nearby.get(i);
				curdist = dist;
				end.x = near.x;
				end.y = near.y;
				end.z = near.z;
			}
		}
		// return index of vertex if found
		return ivertex;
	}

	/**
	 * Looks if a new segment intersects an existing segment.
	 * 
	 * @param origin
	 *            origin vertex index
	 * @param dir
	 *            segment's direction
	 * @param end
	 *            end point position
	 * @return -1 if no intersection exists, else intersecting edge's index
	 **/
	private int intersectWithOtherEdges(int origin, Vector2d dir, Point3d end) {
		// temporary vertex
		Vertex vertex = new Vertex();
		vertex.x = end.x + dir.x * ctx.getRoadSearchRadius();
		vertex.y = end.y + dir.y * ctx.getRoadSearchRadius();
		vertex.z = 0.0;
		// some variables
		Point3d start = roadmap.getVertex(origin);
		int iedge = -1;
		List<Intersection> intersections;
		double curdist = MathUtil.distanceXY(start, vertex);
		// search intersections
		intersections = roadmap.searchIntersectingEdges(vertex, roadmap.getVertex(origin));
		// search for nearest intersection
		for (Intersection is : intersections) {
			double dist = MathUtil.distanceXY(start, is);
			if ((dist <= curdist) && (roadmap.findEdge(origin, is.edge) == -1)) {
				iedge = is.edge;
				end.x = is.x;
				end.y = is.y;
				end.z = ctx.getHeightMap().getValue(end);
				end.z = end.z * (ctx.getTerrainMaxHeight() - ctx.getTerrainMinHeight());
			}
		}
		return iedge;

	}

	/**
	 * Adds a vertex and an edge to the road map. Used if a new road segment is
	 * not affected by existing ones.
	 * 
	 * @param originIndex
	 *            origin vertex
	 * @param x
	 *            x coordinate
	 * @param y
	 *            y coordinate
	 * @param z
	 *            z coordinate
	 * @param segType
	 *            segment type
	 * @param sModule
	 *            current road/street segment module
	 **/
	private void addVertexAndEdge(int originIndex, Point3d position, RoadType segType, RoadModule sModule) {
		// simply add a new vertex and a edge
		Vertex vertex = new Vertex(position);

		int vertexIndex = roadmap.addVertex(vertex);
		Edge edge = new Edge();
		edge.vertices.setFirst(originIndex);
		edge.vertices.setSecond(vertexIndex);
		edge.roadType = segType;
		roadmap.addEdge(edge);
		// set missing parameters of successor module
		sModule.vertexId = vertexIndex;
		sModule.status = ModuleStatus.ACCEPT;
		if (vertexIndex == originIndex) {
			throw new IllegalStateException("vertexIndex == originIndex");
		}
	}

	/**
	 * Adds an intersection to the road map. Used if a new road segment
	 * intersects an existing edge.
	 * 
	 * @param originIndex
	 *            origin vertex
	 * @param edgeIndex
	 *            intersecting edge
	 * @param position
	 *            coordinates
	 * @param segType
	 *            segment type
	 * @param sModule
	 *            current road/street segment module
	 **/
	private void addIntersection(int originIndex, int edgeIndex, Point3d position, RoadType segType,
			RoadModule sModule) {
		int vertexIndex1 = roadmap.edges.get(edgeIndex).vertices.getFirst();
		int vertexIndex2 = roadmap.edges.get(edgeIndex).vertices.getSecond();
		double distance1 = MathUtil.distanceXY(position, roadmap.getVertex(vertexIndex1));
		double distance2 = MathUtil.distanceXY(position, roadmap.getVertex(vertexIndex2));
		if ((distance1 > ctx.getRoadSearchRadius()) && (distance2 > ctx.getRoadSearchRadius())) {
			Vertex vertex = new Vertex(position);

			int vertexIndex = roadmap.addVertex(vertex);
			// create edge 1
			Edge edge1 = new Edge();
			edge1.vertices.setFirst(vertexIndex1);
			edge1.vertices.setSecond(vertexIndex);
			edge1.roadType = roadmap.edges.get(edgeIndex).roadType;
			if (vertexIndex1 == vertexIndex) {
				throw new IllegalStateException("vertexIndex == originIndex");
			}
			// create edge 2
			Edge edge2 = new Edge();
			edge2.vertices.setFirst(vertexIndex);
			edge2.vertices.setSecond(vertexIndex2);
			edge2.roadType = roadmap.edges.get(edgeIndex).roadType;
			if (vertexIndex == vertexIndex2) {
				throw new IllegalStateException("vertexIndex == originIndex");
			}
			// create edge 3
			Edge edge3 = new Edge();
			edge3.vertices.setFirst(originIndex);
			edge3.vertices.setSecond(vertexIndex);
			if (vertexIndex == originIndex) {
				throw new IllegalStateException("vertexIndex == originIndex");
			}
			edge3.roadType = segType;
			// delete old edge and add new ones
			roadmap.deleteEdge(edgeIndex);
			roadmap.addEdge(edge1);
			roadmap.addEdge(edge2);
			roadmap.addEdge(edge3);

			// set missing parameters of successor module
			sModule.vertexId = vertexIndex;
			sModule.status = ModuleStatus.ACCEPT;
		} else {
			if (distance1 <= ctx.getRoadSearchRadius()) {
				if (vertexIndex1 == originIndex) {
					logger.error("vertexIndex == originIndex! (" + vertexIndex1 + " == " + originIndex + ")");
				}
				addEdge(originIndex, vertexIndex1, segType, sModule);
			} else {
				if (vertexIndex2 == originIndex) {
					logger.error("vertexIndex == originIndex! (" + vertexIndex2 + " == " + originIndex + ")");
				}
				addEdge(originIndex, vertexIndex2, segType, sModule);
			}
		}
	}

	/**
	 * Adds an edge to the road map. Used if a new road segment ends near an
	 * existing vertex.
	 * 
	 * @param originIndex
	 *            origin vertex
	 * @param vertexIndex
	 *            end vertex
	 * @param segType
	 *            segment type
	 * @param sModule
	 *            current road/street segment module
	 **/
	private void addEdge(int originIndex, int vertexIndex, RoadType segType, RoadModule sModule) {
		Edge edge = new Edge();
		edge.vertices.setFirst(originIndex);
		edge.vertices.setSecond(vertexIndex);
		edge.roadType = segType;
		// add edge if not already in road map
		if ((roadmap.attachedEdges.get(vertexIndex).size() < ctx.getRoadMaxBranches())
				&& (!roadmap.isConnected(originIndex, vertexIndex)) && originIndex != vertexIndex) {
			// add edge
			roadmap.addEdge(edge);
			// set missing parameters of successor module
			sModule.vertexId = vertexIndex;
			if (vertexIndex == originIndex) {
				logger.error("vertexIndex == originIndex! (" + vertexIndex + " == " + originIndex + ")");
			}
			sModule.status = ModuleStatus.ACCEPT;
		}
	}

}
