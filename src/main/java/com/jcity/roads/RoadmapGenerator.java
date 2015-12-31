package com.jcity.roads;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.vecmath.*;

import org.apache.log4j.*;

import com.jcity.lsystem.*;
import com.jcity.lsystem.condition.*;
import com.jcity.lsystem.modifier.*;
import com.jcity.pipeline.*;
import com.jcity.util.*;

public class RoadmapGenerator {
	private ProductionManager roadSys;
	// < production manager of L-System */
	private ModuleString mString;
	// < L-System module string */
	private Roadmap roadmap;
	// < road map */
	// private boolean lookOnlyAtRoads;
	// < flag */
	private int gstep;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private Graphics2D g;
	private CityContext ctx;

	public RoadmapGenerator(CityContext ctx) {
		this(ctx, null);
	}

	/**
	 * 
	 * @param g
	 *            (optional) Graphics object, where the generator will render
	 *            each step. If not null, the generator will pause between each
	 *            step, to provide an animation, so this slows down the process.
	 */
	public RoadmapGenerator(CityContext ctx, Graphics2D g) {
		this.ctx = ctx;
		this.g = g;
		this.roadSys = new ProductionManager();
		this.mString = new ModuleString();
		this.roadmap = new Roadmap();

		gstep = 0;

		// L-System setup
		logger.info("L-System setup... ");
		// create modules
		RoadModule R = new RoadModule('R'); // road.
		RoadModule B = new RoadModule('B'); // branch. With an extra roadType,
											// (to track
		// the child road type)
		// create successors (right sides of productions)
		// for production p1
		ModuleString p1succ = new ModuleString();

		// for production p2
		ModuleString p2succ = new ModuleString();
		// WTF: adding the same module 3 times? (not copies. the same actual
		// reference).
		p2succ.add(new RoadModule(B));
		p2succ.add(new RoadModule(B));
		p2succ.add(new RoadModule(B));

		// for production p3
		ModuleString p3succ = new ModuleString();
		p3succ.add(new RoadModule(B));

		// for production p4
		ModuleString p4succ = new ModuleString();
		p4succ.add(new RoadModule(R));

		// create productions
		// production p1
		// If the module is marked for deletion, do nothing.
		roadSys.add(new Production(R, p1succ, new DeletionCondition(), null));
		// production p2
		// If the module is marked as accepted, initialize Branch modules.
		roadSys.add(new Production(R, p2succ, new AcceptanceCondition(), new RoadSeedModifier(ctx, roadmap)));
		// production p3
		// If the branch's delay is greater than zero, decrement it.
		roadSys.add(new Production(B, p3succ, new NonZeroDelayCondition(), new DelayModifier()));
		// production p4
		// If the branch's delay equals zero, convert branches to actual roads
		// in the roadmap.
		roadSys.add(new Production(B, p4succ, new ZeroDelayCondition(), new RoadCreatorModifier(ctx, roadmap, g)));
		// add productions to manager

		logger.info("L-System setup done\n");
		// create initial road map
		Vertex v1 = new Vertex(), v2 = new Vertex();
		Edge edge = new Edge();
		double z1, z2;

		v1.x = ctx.getInitialRoadSegmentStartX();
		v1.y = ctx.getInitialRoadSegmentStartY();
		v2.x = ctx.getInitialRoadSegmentEndX();
		v2.y = ctx.getInitialRoadSegmentEndY();

		try {
			z1 = ctx.getHeightMap().getInterpolatedValue(v1.x, v1.y);
			z2 = ctx.getHeightMap().getInterpolatedValue(v2.x, v2.y);
		} catch (Exception e) {
			throw new IllegalArgumentException("Error Setting initial road segment!", e);
		}
		v1.z = (float) (z1 * (ctx.getTerrainMaxHeight() - ctx.getTerrainMinHeight()));
		v2.z = (float) (z2 * (ctx.getTerrainMaxHeight() - ctx.getTerrainMinHeight()));

		edge.vertices.setFirst(roadmap.addVertex(v1));
		edge.vertices.setSecond(roadmap.addVertex(v2));
		edge.roadType = RoadType.ROAD;
		roadmap.addEdge(edge);

		logger.debug(String.format("Created first road segment, (" + v1.x + "," + v1.y + "," + v1.z + ")-(" + v2.x + ","
				+ v2.y + "," + v2.z + ")"));

		// create mString
		RoadModule Rt = new RoadModule(R);
		Rt.originVertexId = edge.vertices.getFirst();
		Rt.delay = Rt.originVertexId;
		Rt.vertexId = edge.vertices.getSecond();
		Rt.roadType = RoadType.ROAD;
		Rt.status = ModuleStatus.ACCEPT;
		Rt.direction = new Vector2d(v2.x - v1.x, v2.y - v1.y);
		// Rt.direction.normalize();

		mString.add(Rt);

	}

	/**
	 * Generate base road map by applying the L-System until no more changes
	 * occur.
	 **/
	private void generateBaseRoadmap() throws IOException {
		boolean changed = true;
		int step = 0;
		// repeat until roadmap doesn't change anymore

		while (changed) {
			// get some variables
			int numVertices = roadmap.getVertexCount();
			int numEdges = roadmap.getEdgeCount();
			// print some status information
			// logger.debug("step " + step + ":\t\tVertices: " + numVertices +
			// "\t\tEdges: " + numEdges);
			// apply rules branchDelay + 2 times

			// Don't branch until ROAD_BRANCH_DELAY iterations have passed?
			long delay = ctx.getRoadBranchDelay();
			for (int i = 0; i <= delay + 1; i++) {
				roadSys.apply(mString);
				step++;
				// save roadmap for debugging if wanted
				if (ctx.isDebug()) {
					String fname = ctx.getOutputDir() + ctx.getProjectName() + "." + (gstep++) + ".rmap";
					roadmap.saveToFile(fname);
				}
			}
			// look if changes have occurred
			changed = (numVertices != roadmap.getVertexCount()) || (numEdges != roadmap.getEdgeCount());
		}
	}

	/**
	 * Filter road map. Remove dead ends and intersecting edges.
	 **/
	private void filter() {
		// remove multi edges
		logger.debug("Removing edges:");
		int count = 0;
		for (int i = roadmap.getEdgeCount() - 1; i >= 0; i--) {
			Edge edge1 = roadmap.edges.get(i);
			for (int j = i - 1; j >= 0; j--) {
				Edge edge2 = roadmap.getEdge(j);
				if (edge1.equals(edge2)) {
					logger.debug("Edge " + i + " is the same as Edge " + j + ". Deleting " + j);
					roadmap.deleteEdge(j);
					i--;
					count++;
				}
			}
		}
		// remove all edges which still have intersections
		List<Integer> badEdges = new ArrayList<>();
		for (int i = 0; i < roadmap.getEdgeCount(); i++) {
			Vertex v1 = roadmap.getVertex(roadmap.getEdge(i).vertices.getFirst());
			Vertex v2 = roadmap.getVertex(roadmap.getEdge(i).vertices.getSecond());
			// List<Integer> attEdges1 =
			// roadmap.getAttachedEdges(roadmap.getEdge(i).vertices.getFirst());
			// List<Integer> attEdges2 =
			// roadmap.getAttachedEdges(roadmap.getEdge(i).vertices.getSecond());
			List<Intersection> intersections = roadmap.searchIntersectingEdges(v1, v2);
			for (int j = 0; j < (int) intersections.size(); j++) {
				if ((roadmap.getEdge(intersections.get(j).edge).roadType == RoadType.STREET)
						&& (roadmap.findEdge(roadmap.getEdge(i).vertices.getFirst(), intersections.get(j).edge) == -1)
						&& (roadmap.findEdge(roadmap.getEdge(i).vertices.getSecond(),
								intersections.get(j).edge) == -1)) {
					badEdges.add(intersections.get(j).edge);
					logger.debug("edge " + intersections.get(j).edge + " intersects edge " + i + ". Deleting "
							+ intersections.get(j).edge);
				}
			}
		}
		logger.debug("sorting " + badEdges.size() + " indices.");
		// sort these edges from big indices to small ones
		for (int i = (int) badEdges.size() - 1; i > 0; i--) {
			for (int j = 0; j < i; j++) {
				if (badEdges.get(j) > badEdges.get(j + 1)) {
					int temp;
					// swap
					temp = badEdges.get(j);
					badEdges.set(j, badEdges.get(j + 1));
					badEdges.set(j + 1, temp);
				}
			}
		}

		// remove duplicates
		for (int i = badEdges.size() - 1; i > 0; i--) {
			logger.debug("comparing badedge " + i + " with badEdge " + (i - 1) + ": " + badEdges.get(i) + " -> "
					+ badEdges.get(i - 1));
			if (badEdges.get(i).equals(badEdges.get(i - 1))) {
				logger.debug("removing duplicate badEdge: " + badEdges.get(i));
				badEdges.remove(i);
			}
		}

		// remove these edges
		for (int i = badEdges.size() - 1; i >= 0; i--, count++) {
			logger.debug("* Removing edge " + badEdges.get(i));
			roadmap.deleteEdge(badEdges.get(i));
		}
		// remove all vertices which only have a degree of 1 or 0, except vertex
		// 0
		logger.debug("Removing vertices with degree smaller than 2:");
		count = 0;
		boolean changed = true;
		while (changed) {
			changed = false;
			for (int i = roadmap.getVertexCount() - 1; i > 0; i--) {
				if (roadmap.getAttachedEdgeCount(i) < 2) {
					logger.debug(i);
					roadmap.deleteVertex(i);
					changed = true;
					count++;
				}
			}
		}
	}

	/**
	 * Generate curbs. Not currently working.
	 * 
	 */
	private void generateCurbs() {
		// sort attached edges
		roadmap.sortAttachedEdges();
		// generate curb vertices and info telling which vertex belongs to which
		// edges etc.
		List<CurbVertexInfo> cvInfo = generateCurbVertices();
		logger.debug("CurbVertex count: " + cvInfo.size());
		// generate curb edges
		generateCurbEdges(cvInfo);
	}

	/**
	 * Generate Curb Edges
	 * 
	 * @param cvInfo
	 */
	private void generateCurbEdges(List<CurbVertexInfo> cvInfo) {
		int numEdges = roadmap.getEdgeCount();
		// for each edge...
		for (int i = 0; i < numEdges; i++) {
			logger.debug("Adding curbs for edge #" + i);
			Edge edge = roadmap.getEdge(i);
			Pair<Integer, Integer> ev1, ev2;
			// search indices of curb vertices belonging to this road segment
			// TODO: Broken.
			ev1 = searchIndices(cvInfo, edge.vertices.getFirst(), i);
			ev2 = searchIndices(cvInfo, edge.vertices.getSecond(), i);
			// make dead ends if necessary
			if (roadmap.getAttachedEdgeCount(edge.vertices.getFirst()) == 1) {
				Edge edge1 = new Edge(), edge2 = new Edge();
				edge1.vertices.setFirst(ev1.getFirst());
				edge1.vertices.setSecond(edge.vertices.getFirst());
				edge1.roadType = RoadType.CURB;
				edge1.curbs.setFirst(-1);
				edge1.curbs.setSecond(-1);

				edge2.vertices.setFirst(ev1.getSecond());
				edge2.vertices.setSecond(edge.vertices.getFirst());
				edge2.roadType = RoadType.CURB;
				edge2.curbs.setFirst(-1);
				edge2.curbs.setSecond(-1);
				roadmap.addEdge(edge1);
				roadmap.addEdge(edge2);
			}
			if (roadmap.getAttachedEdgeCount(edge.vertices.getSecond()) == 1) {
				Edge edge1 = new Edge(), edge2 = new Edge();
				edge1.vertices.setFirst(ev2.getFirst());
				edge1.vertices.setSecond(edge.vertices.getFirst());
				edge1.roadType = RoadType.CURB;
				edge1.curbs.setFirst(-1);
				edge1.curbs.setSecond(-1);

				edge2.vertices.setFirst(ev2.getSecond());
				edge2.vertices.setSecond(edge.vertices.getFirst());
				edge2.roadType = RoadType.CURB;
				edge2.curbs.setFirst(-1);
				edge2.curbs.setSecond(-1);
				roadmap.addEdge(edge1);
				roadmap.addEdge(edge2);
			}
			// add two curbs on each side of the edge
			Edge edge1 = new Edge(), edge2 = new Edge();
			edge1.vertices.setFirst(ev1.getFirst());
			edge1.vertices.setSecond(ev2.getSecond());
			edge1.roadType = RoadType.CURB;
			edge1.curbs.setFirst(-1);
			edge1.curbs.setSecond(-1);

			edge2.vertices.setFirst(ev1.getSecond());
			edge2.vertices.setSecond(ev2.getFirst());
			edge2.roadType = RoadType.CURB;
			edge2.curbs.setFirst(-1);
			edge2.curbs.setSecond(-1);

			int index1 = roadmap.addEdge(edge1);
			int index2 = roadmap.addEdge(edge2);
			roadmap.getEdge(i).curbs.setFirst(index1);
			roadmap.getEdge(i).curbs.setSecond(index2);
		}
	}

	/**
	 * Generate Curb Vertices.
	 * 
	 * @param cvInfo
	 */
	private List<CurbVertexInfo> generateCurbVertices() {
		List<CurbVertexInfo> retval = new ArrayList<>();
		int numVertices = roadmap.getVertexCount();
		for (int i = 0; i < numVertices; i++) {
			Vertex v = roadmap.getVertex(i);
			if (roadmap.getAttachedEdgeCount(i) == 1) {
				Vector2d eDir = calculateEdgeDirection(i, roadmap.getAttachedEdges(i).get(0));
				// get two direction vectors
				Vector2d eDir1 = new Vector2d(eDir.y, -eDir.x);
				Vector2d eDir2 = new Vector2d(-eDir.y, eDir.x);
				// add 2 vertices
				Vertex v1 = new Vertex(), v2 = new Vertex();
				v1.x = v.x + eDir1.x * ctx.getRoadLaneWidth();
				v1.y = v.y + eDir1.y * ctx.getRoadLaneWidth();
				v1.z = v.z;
				v1.belongsTo = i;

				v2.x = v.x + eDir2.x * ctx.getRoadLaneWidth();
				v2.y = v.y + eDir2.y * ctx.getRoadLaneWidth();
				v2.z = v.z;
				v2.belongsTo = i;

				int index1 = roadmap.addVertex(v1);
				int index2 = roadmap.addVertex(v2);
				// add 2 curb vertex infos
				CurbVertexInfo cv1 = new CurbVertexInfo(), cv2 = new CurbVertexInfo();

				if (MathUtil.angle(eDir1) < 180.0) {
					cv1.edges.setFirst(roadmap.getAttachedEdges(i).get(0));
					cv1.edges.setSecond(-1);
					cv1.vertices.setFirst(i);
					cv1.vertices.setSecond(index1);
					cv2.edges.setFirst(-1);
					cv2.edges.setSecond(roadmap.getAttachedEdges(i).get(0));
					cv2.vertices.setFirst(i);
					cv2.vertices.setSecond(index2);
				} else {
					cv1.edges.setFirst(-1);
					cv1.edges.setSecond(roadmap.getAttachedEdges(i).get(0));
					cv1.vertices.setFirst(i);
					cv1.vertices.setSecond(index1);
					cv2.edges.setFirst(roadmap.getAttachedEdges(i).get(0));
					cv2.edges.setSecond(-1);
					cv2.vertices.setFirst(i);
					cv2.vertices.setSecond(index2);
				}
				retval.add(cv1);
				retval.add(cv2);
			} else {
				List<Integer> attEdges = roadmap.getAttachedEdges(i);
				for (int j = 0; j < (int) attEdges.size(); j++) {
					Vector2d e1Dir = calculateEdgeDirection(i, attEdges.get(j));
					Vector2d e2Dir = calculateEdgeDirection(i, attEdges.get((j + 1) % attEdges.size()));

					double alpha1 = MathUtil.angle(e1Dir);
					double alpha2 = MathUtil.angle(e2Dir);
					double alpha, beta;
					if (alpha2 >= alpha1) {
						beta = (alpha2 - alpha1) / 2.0;
						alpha = alpha1 + beta;
					} else {
						beta = (360.0 - alpha2 + alpha1) / 2.0;
						alpha = alpha2 + beta;
					}
					Vector2d dir = new Vector2d();
					MathUtil.rotateNormalizedVector(alpha, dir);
					// calculate scale factor
					double scale = 1.0;
					if (beta == 90.0) {
						scale = ctx.getRoadLaneWidth();
					} else {
						scale = Math.abs(ctx.getRoadLaneWidth() / Math.sin(beta * MathUtil.PI180));
					}
					// add a vertex
					Vertex vertex = new Vertex();
					vertex.x = roadmap.getVertex(i).x + dir.x * scale;
					vertex.y = roadmap.getVertex(i).y + dir.y * scale;
					vertex.z = roadmap.getVertex(i).z;
					vertex.belongsTo = i;
					int index = roadmap.addVertex(vertex);
					// add curb vertex info
					CurbVertexInfo cv = new CurbVertexInfo();
					cv.edges.setFirst(attEdges.get(j));
					cv.edges.setSecond(attEdges.get((j + 1) % (int) attEdges.size()));
					cv.vertices.setFirst(i);
					cv.vertices.setSecond(index);
					retval.add(cv);
				}
			}
		}
		return retval;
	}

	/**
	 * Calculate direction of an edge from given vertex.
	 * 
	 * @param from
	 *            index of the origin Vertex.
	 * @param to
	 *            index of the Edge to calculate.
	 * @return direction of the edge, as a normalized vector.
	 */
	private Vector2d calculateEdgeDirection(int vIndex, int eIndex) {
		Vector2d dir = new Vector2d();

		Edge edge = roadmap.getEdge(eIndex);
		if (edge.vertices.getFirst() == vIndex) {
			Vertex v1 = roadmap.getVertex(vIndex);
			Vertex v2 = roadmap.getVertex(edge.vertices.getSecond());
			dir.x = v2.x - v1.x;
			dir.y = v2.y - v1.y;
		}
		if (edge.vertices.getSecond() == vIndex) {
			Vertex v1 = roadmap.getVertex(vIndex);
			Vertex v2 = roadmap.getVertex(edge.vertices.getFirst());
			dir.x = v2.x - v1.x;
			dir.y = v2.y - v1.y;
		}
		dir.normalize();
		return dir;
	}

	/**
	 * Main program of class. First roads are generated, later streets are added
	 * to complete the road map. Bad edges are removed by filtering. As a last
	 * step curbs are generated.
	 * 
	 * @throws IOException
	 **/
	public Roadmap execute() throws IOException {
		// apply L-System, generate road map, first only for roads
		logger.info("\nPhase 1: Creating initial road map ----------------------------------------\n");
		setRoadFlag(true);
		generateBaseRoadmap();
		logger.info("\nGenerated road map has " + roadmap.getVertexCount() + " vertices and " + roadmap.getEdgeCount()
				+ " edges.\n");

		// apply L-System, complete road map with streets
		logger.info("\nPhase 2: Completing road map ----------------------------------------------\n");
		setRoadFlag(false);
		generateBaseRoadmap();

		// print some status information
		logger.info("\nGenerated road map has " + roadmap.getVertexCount() + " vertices and " + roadmap.getEdgeCount()
				+ " edges.\n");
		// do some filtering (not currently working).
		// logger.info("\nFiltering
		// -----------------------------------------------------------------\n");
		// filter();
		// logger.info("\nFiltered road map has " + roadmap.getVertexCount() +
		// " vertices and " + roadmap.getEdgeCount() + " edges.\n");
		// // generate curbmap
		// logger.info("\nGenerating curbs
		// ----------------------------------------------------------\n");
		// // generateCurbs(); //not working properly.
		// logger.info("\nRoad map with curbs has " + roadmap.getNumOfVertices()
		// + " vertices and "
		// + roadmap.getNumOfEdges() + " edges.\n");
		//
		// logger.info("segments discarded due to too many edges: " +
		// discardedDueToMaxEdges);
		// logger.info("segments discarded due to no possible legal adjustment:
		// "
		// + discardedDueToNoAdjustment);
		// logger.info("segments discarded due to sector already accupied: " +
		// discardedDueToSectorOccupied);

		return roadmap;
	}

	private void setRoadFlag(boolean lookOnlyAtRoads) {
		for (Production p : roadSys) {
			p.setRoadFlag(lookOnlyAtRoads);
		}
	}

	/**
	 * Find two vertices in curb vertex information belonging to the given
	 * vertex with given attached edge (road/street).
	 * 
	 * @param vIndex
	 *            vertex index
	 * @param eIndex
	 *            edge index
	 * @param cvInfo
	 *            curb vertex information
	 * 
	 **/
	// TODO: Broken.
	private Pair<Integer, Integer> searchIndices(List<CurbVertexInfo> cvInfo, int vIndex, int eIndex) {
		Pair<Integer, Integer> retval = new Pair<>();

		int edgeIndex1 = -1;
		int edgeIndex2 = -1;
		if (roadmap.getAttachedEdgeCount(vIndex) > 1) {
			int index = roadmap.findEdge(vIndex, eIndex);
			List<Integer> attEdges = roadmap.getAttachedEdges(vIndex);
			edgeIndex1 = attEdges.get((index + 1) % (int) attEdges.size());
			edgeIndex2 = attEdges.get((index - 1 + (int) attEdges.size()) % (int) attEdges.size());
		}
		System.out.println("Searching " + cvInfo.size() + " CurbVertices for vertex belonging to roadVertex " + vIndex
				+ ", with curbEdge with index " + eIndex);
		// Assumes every CurbVertex has two edges.
		for (CurbVertexInfo cv : cvInfo) {
			if (cv.vertices.getFirst() == vIndex) {
				if ((cv.edges.getFirst() == eIndex) && (cv.edges.getSecond() == edgeIndex1)) {
					retval.setSecond(cv.vertices.getSecond());
				}
			}
		}
		for (CurbVertexInfo cv : cvInfo) {
			if (cv.vertices.getFirst() == vIndex) {
				if ((cv.edges.getSecond() == eIndex) && (cv.edges.getFirst() == edgeIndex2)) {
					retval.setFirst(cv.vertices.getSecond());
				}
			}
		}
		return retval;
	}

}
