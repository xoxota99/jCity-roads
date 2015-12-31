package com.jcity.roads;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;

import org.apache.log4j.Logger;

import com.jcity.model.Layer;

/**
 * Requires major refactoring. Ported from C with minimal OOD.
 * 
 * @author philippd
 * 
 */
public class Roadmap implements Serializable {

	public List<List<Integer>> attachedEdges = new ArrayList<>(); // index
																	// into
																	// the
																	// Edges
																	// collection.
	public List<Edge> edges = new ArrayList<>();
	public List<Vertex> vertices = new ArrayList<>();
	private Logger logger = Logger.getLogger(this.getClass());

	/**
	 * 
	 */
	private static final long serialVersionUID = 2999243658638814417L;

	/**
	 * Add a vertex to the vertex list.
	 * 
	 * @param vertex
	 * @return index of the added vertex in the vertex list.
	 */
	public int addVertex(Vertex vertex) {
		List<Integer> vec = new ArrayList<>(); // empty IntVector
		vertices.add(vertex); // add vertex to list
		attachedEdges.add(vec); // add vec to list; vertex hasn't any
								// attached edges yet
		return (vertices.size() - 1); // return as index (size - 1)
	}

	/**
	 * Add an edge to the edge list.
	 * 
	 * @param edge
	 * @return index of the added edge in the edge list.
	 */
	public int addEdge(Edge edge) {
		// throw an exception if first specified vertex doesn't exist
		if ((edge.vertices.getFirst() < 0) || (edge.vertices.getFirst() >= vertices.size()))
			throw new IndexOutOfBoundsException("Vertex doesn't exists!");
		// throw an exception if second specified vertex doesn't exist
		if ((edge.vertices.getSecond() < 0) || (edge.vertices.getSecond() >= vertices.size()))
			throw new IndexOutOfBoundsException("Vertex doesn't exists!");
		// throw an exception if first vertex == second vertex
		if (edge.vertices.getFirst() == edge.vertices.getSecond())
			throw new IllegalArgumentException("First vertex equals second vertex!");
		// check if edge is already in list, if it is throw an exception
		if (isConnected(edge.vertices.getFirst(), edge.vertices.getSecond()))
			// throw new
			// IllegalArgumentException("Tried to add already existing edge!");
			return findEdgeBetween(edge.vertices.getFirst(), edge.vertices.getSecond());
		// add edge to edge list
		edges.add(edge);
		// edge index, which is (size - 1)
		int index = edges.size() - 1;
		// add edge to attached edge list
		attachedEdges.get(edge.vertices.getFirst()).add(index);
		attachedEdges.get(edge.vertices.getSecond()).add(index);
		// return edge index, which is (size - 1)
		return index;

	}

	/**
	 * Determine if two vertices are connected by an edge.
	 * 
	 * @param first
	 *            - index of the first vertex.
	 * @param second
	 *            - index of the second vertex.
	 * @return true if first vertex is connected to second vertex, otherwise
	 *         false.
	 */
	public boolean isConnected(Integer first, Integer second) {
		List<Integer> attEdge = attachedEdges.get(first); // list of all edges
															// attached to
															// vertex first.
		for (Integer c : attEdge) {
			if (c >= edges.size()) {
				logger.error("in isConnected(), tried to access edges that don't exist (ID=" + c + ")");
				// TODO: We should remove this edge from the attachedEdges when
				// this
				// happens. Better yet, this should never happen.
				return false;
			}
			if ((edges.get(c).vertices.getFirst() == second) || (edges.get(c).vertices.getSecond() == second))
				return true;
		}
		return false;
	}

	/**
	 * Remove a vertex
	 * 
	 * @param i
	 */
	public void deleteVertex(int i) {
		// check index and throw an exception if it's out of bounds
		if ((i < 0) || (i >= vertices.size()))
			throw new IndexOutOfBoundsException("Tried to access non-existing vertex!");
		// get attached edges
		List<Integer> attEdges = attachedEdges.get(i);
		// delete vertex
		vertices.remove(i);
		// delete attached edges vector
		attachedEdges.remove(i);
		// sort attached edges
		for (int m = (int) attEdges.size() - 1; m > 0; m--) {
			for (int n = 0; n < m; n++) {
				if (attEdges.get(n) > attEdges.get(n + 1)) {
					int temp;
					temp = attEdges.get(n);
					attEdges.set(n, attEdges.get(n + 1));
					attEdges.set(n + 1, temp);
				}
			}
		}
		// delete them
		for (int m = (int) attEdges.size() - 1; m >= 0; m--) {
			edges.remove(attEdges.get(m));
		}

		// clear attached edges vectors
		for (List<Integer> li : attachedEdges) {
			li.clear();
		}

		// update edges
		for (int m = 0; m < edges.size(); m++) {
			Edge edge = edges.get(m);
			if (edge.vertices.getFirst() >= i)
				edge.vertices.setFirst(edge.vertices.getFirst() - 1);
			if (edge.vertices.getSecond() >= i)
				edge.vertices.setSecond(edge.vertices.getSecond() - 1);
			attachedEdges.get(edge.vertices.getFirst()).add(m);
			attachedEdges.get(edge.vertices.getSecond()).add(m);
		}

	}

	/**
	 * Delete an edge.
	 * 
	 * @param i
	 */
	public void deleteEdge(int i) {
		// check index and throw an exception if it's out of bounds
		if ((i < 0) || (i >= edges.size()))
			throw new IndexOutOfBoundsException("Tried to access non-existant edge! (index=" + i + ")");
		// delete edge
		edges.remove(i);

		// We've removed the edge. Now remove any reference to it in the index
		// mapping of attachedEdges. For any entries that refer to indexes
		// greater than i, decrement those by one.
		for (List<Integer> temp : attachedEdges) {
			List<Integer> t2 = new ArrayList<>();
			for (Integer j : temp) {
				if (j != i) {
					if (j > i) {
						t2.add(j - 1); // decrement by one.
					} else {
						t2.add(j); // retain as-is
					}
				}
			}
			temp.clear();
			temp.addAll(t2);
		}
	}

	/**
	 * Search for nearby vertices. Search will omit Z coordinate, so it is
	 * basically 2D in the XY-Plane.
	 * 
	 * @param loc
	 *            location we want to search nearby vertices for
	 * @param radius
	 *            radius around vertex we want to search
	 * @param result
	 *            vector for storing results
	 **/
	public List<Integer> searchNearbyVertices(Point3d loc, double radius) {
		// clear result vector
		List<Integer> result = new ArrayList<>();
		// calculate radius * radius so we can omit sqrt
		double radius2 = radius * radius;
		// for each vertex do...
		for (int i = 0; i < (int) vertices.size(); i++) {
			// calculate distance * distance
			double tx = loc.x - vertices.get(i).x;
			double ty = loc.y - vertices.get(i).y;
			double dist2 = tx * tx + ty * ty;
			// if distance * distance <= radius * radius add index to result
			// vector
			if (dist2 <= radius2)
				result.add(i);
		}
		return result;
	}

	/**
	 * Search for intersecting edges to a given one. Search will omit Z
	 * coordinate, so it is basically 2D in the XY-Plane.
	 * 
	 * @param v1
	 *            end vertex of edge
	 * @param v2
	 *            end vertex of edge
	 * @param result
	 *            vector for storing results
	 * @return
	 **/
	public List<Intersection> searchIntersectingEdges(Vertex v1, Vertex v2) {
		List<Intersection> result = new ArrayList<>();

		// get vertices positions
		double a[] = { v1.x, v1.y };
		double b[] = { v2.x, v2.y };
		// calculate bounding box for a & b
		double boundAB[] = new double[4];
		if (a[0] <= b[0]) {
			boundAB[0] = a[0];
			boundAB[2] = b[0];
		} else {
			boundAB[2] = a[0];
			boundAB[0] = b[0];
		}
		if (a[1] <= b[1]) {
			boundAB[1] = a[1];
			boundAB[3] = b[1];
		} else {
			boundAB[3] = a[1];
			boundAB[1] = b[1];
		}
		// calculate direction vector
		double u[] = { b[0] - a[0], b[1] - a[1] };
		// for each edge do...
		for (int count = 0; count < (int) edges.size(); count++) {
			// get vertices positions
			int v1index = edges.get(count).vertices.getFirst();
			int v2index = edges.get(count).vertices.getSecond();
			double c[] = { vertices.get(v1index).x, vertices.get(v1index).y };
			double d[] = { vertices.get(v2index).x, vertices.get(v2index).y };
			// calculate bounding box for c & d
			double boundCD[] = new double[4];
			if (c[0] <= d[0]) {
				boundCD[0] = c[0];
				boundCD[2] = d[0];
			} else {
				boundCD[2] = c[0];
				boundCD[0] = d[0];
			}
			if (c[1] <= d[1]) {
				boundCD[1] = c[1];
				boundCD[3] = d[1];
			} else {
				boundCD[3] = c[1];
				boundCD[1] = d[1];
			}
			// if boxes don't overlap, intersection of lines isn't possible
			double eps = 0.1;
			if (boundCD[0] > boundAB[2] + eps)
				continue;
			if (boundCD[2] + eps < boundAB[0])
				continue;
			if (boundCD[1] > boundAB[3] + eps)
				continue;
			if (boundCD[3] + eps < boundAB[1])
				continue;
			// calculate direction vector
			double v[] = { d[0] - c[0], d[1] - c[1] };
			// calculate common divisor
			double div = u[0] * v[1] - u[1] * v[0];
			// if divisor is near zero the two lines are parallel
			if ((div > 0.0000001) || (div < -0.0000001)) {
				// calculate intersection
				double j = -(-u[0] * a[1] + u[0] * c[1] + u[1] * a[0] - u[1] * c[0]) / div;
				double i = -(a[0] * v[1] - c[0] * v[1] - v[0] * a[1] + v[0] * c[1]) / div;
				// if intersection occures in the part of the line which we are
				// intrested in...
				if ((i >= 0.0) && (i <= 1.0) && (j >= 0.0) && (j <= 1.0)) {
					// calculate intersection record and add it to result
					Intersection t = new Intersection();
					t.edge = count;
					t.x = a[0] + i * u[0];
					t.y = a[1] + i * u[1];
					result.add(t);
				}
			}
		}
		return result;
	}

	/**
	 * Sort attached edges.
	 */
	public void sortAttachedEdges() {
		// for each vertex...
		for (int i = 0; i < (int) vertices.size(); i++) {
			// some variables
			List<Integer> attEdges = attachedEdges.get(i);
			double angle[] = new double[10];
			// for each attached edge calculate its angle...
			for (int j = 0; j < attEdges.size(); j++) {
				// some variables
				Vertex v1, v2;
				Edge edge = edges.get(attEdges.get(j));
				// get edge end vertices, v1 is vertex for which we are
				// currently sorting
				if (edge.vertices.getFirst() == i) {
					v1 = vertices.get(edge.vertices.getFirst());
					v2 = vertices.get(edge.vertices.getSecond());
				} else {
					v2 = vertices.get(edge.vertices.getFirst());
					v1 = vertices.get(edge.vertices.getSecond());
				}
				// calculate normalized direction vector
				double x = v2.x - v1.x;
				double y = v2.y - v1.y;
				double s = 1.0 / Math.sqrt(x * x + y * y);
				x *= s;
				y *= s;
				// calculate angle and write it in angle array
				if ((x >= 0.0) && (y >= 0.0))
					angle[j] = Math.asin(y);
				if ((x < 0.0) && (y >= 0.0))
					angle[j] = Math.PI - Math.asin(y);
				if ((x < 0.0) && (y < 0.0))
					angle[j] = Math.PI + Math.asin(-y);
				if ((x >= 0.0) && (y < 0.0))
					angle[j] = 2 * Math.PI - Math.asin(-y);
			}
			// do a bubble sort to sort attached edges
			for (int a = 0; a < (int) attEdges.size() - 1; a++) {
				for (int b = 0; b < (int) attEdges.size() - a - 1; b++) {
					if (angle[b] > angle[b + 1]) {
						// swap angle array elements
						double t1;
						t1 = angle[b];
						angle[b] = angle[b + 1];
						angle[b + 1] = t1;
						// swap attached edges indices
						int t2;
						t2 = attEdges.get(b);
						attEdges.set(b, attEdges.get(b + 1));
						attEdges.set(b + 1, t2);
					}
				}
			}
		}
	}

	/**
	 * Search for edge in attached edges of a vertex.
	 * 
	 * @param vIndex
	 *            vertex index
	 * @param eIndex
	 *            edge index
	 * @return -1 if edge is not attached to vertex, else the index in the
	 *         attached edges
	 */
	public int findEdge(int vIndex, int eIndex) {
		List<Integer> attEdges = attachedEdges.get(vIndex);
		for (Integer i : attEdges) {
			if (i == eIndex)
				return i;
		}
		return -1; // not found.
	}

	/**
	 * Find the edge between the given two vertices
	 * 
	 * @param v1
	 *            index of the first vertex.
	 * @param v2
	 *            index of the second vertex
	 * @return index of the edge between the two given vertices, if one exists,
	 *         otherwise -1.
	 */
	public int findEdgeBetween(int v1, int v2) {
		List<Integer> attEdges = attachedEdges.get(v1);
		for (Integer i : attEdges) {
			if (edges.get(i).vertices.getSecond() == v2 || edges.get(i).vertices.getFirst() == v2) {
				return i;
			}
		}
		return -1; // not found.
	}

	/**
	 * Export the roadmap in JSON format.
	 */
	public String toJSON() {
		StringBuffer sb = new StringBuffer();
		int vSize = vertices.size();
		int eSize = edges.size();
		sb.append("{");
		sb.append("vertexCount:" + vSize + ",");
		sb.append("edgeCount:" + eSize + ",");
		sb.append("vertices: [");
		for (int i = 0; i < vSize; i++) {
			Vertex v = vertices.get(i);
			sb.append("{");
			sb.append("i:" + i + ",");
			sb.append("x:" + v.x + ",");
			sb.append("y:" + v.y + ",");
			sb.append("z:" + v.z + ",");
			if (v.belongsTo >= 0) {
				sb.append("belongsTo:" + v.belongsTo);
			}
			List<Integer> p = attachedEdges.get(i);
			if (p.size() > 0) {
				sb.append(",edges:[");
				for (Integer i2 : p) {
					sb.append(i2).append(",");
				}
				sb.append("]");
			}

			sb.append("}"); // vertex
			if (i < vSize - 1) {
				sb.append(",");
			}
		}
		sb.append("],"); // vertices

		sb.append("edges: [");
		for (int i = 0; i < eSize; i++) {
			Edge e = edges.get(i);
			sb.append("{");
			sb.append("i:" + i + ",");
			sb.append("from:" + e.vertices.getFirst() + ",");
			sb.append("to:" + e.vertices.getSecond() + ",");
			sb.append("}"); // edge
			if (i < eSize - 1) {
				sb.append(",");
			}
		}
		sb.append("],"); // edges

		sb.append("}"); // top
		return sb.toString();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("\nRoadmap object dump =====================\n");
		int sizev = vertices.size();
		int sizee = edges.size();
		sb.append(sizev).append(" vertices, ").append(sizee).append(" edges\n");
		sb.append("-----------------------------------------\n");
		for (int i = 0; i < sizev; i++) {
			sb.append(i).append(": ");
			sb.append("(").append(vertices.get(i).x).append(" | ");
			sb.append(vertices.get(i).y).append(" | ");
			sb.append(vertices.get(i).z).append(")");
			sb.append(" -> ");
			List<Integer> p = attachedEdges.get(i);
			for (Integer i2 : p) {
				sb.append(i2).append(" ");
			}
			sb.append("\n");
		}
		sb.append("-----------------------------------------\n");
		for (int i = 0; i < sizee; ++i) {
			sb.append(i).append(": ");
			sb.append("(").append(edges.get(i).vertices.getFirst()).append(" <-> ")
					.append(edges.get(i).vertices.getSecond()).append(")\n");
		}
		sb.append("=========================================\n\n");

		return sb.toString();
	}

	public int getEdgeCount() {
		return edges.size();
	}

	public void saveToFile(String fName) throws IOException {
		PrintWriter out = new PrintWriter(fName);
		out.write(this.toJSON());
		out.close();
	}

	
	public int getVertexCount() {
		return vertices.size();
	}

	public Vertex getVertex(int i) {
		if (i < 0 || i > vertices.size() - 1)
			return null;
		return vertices.get(i);
	}

	public Edge getEdge(int i) {
		if (i < 0 || i > edges.size() - 1)
			return null;
		return edges.get(i);
	}

	/**
	 * 
	 * @param i
	 *            index of the Vertex
	 * @return the list of edges attached to the Vertex at index i, or an empty
	 *         list if there are no edges attached to the Vertex, or if the
	 *         index is out of bounds.
	 */
	public List<Integer> getAttachedEdges(int i) {
		// commented out temporarily, to cause traceable errors.
		// if (i < 0 || i > attachedEdges.size() - 1)
		// return new ArrayList<Integer>();
		return attachedEdges.get(i);
	}

	/**
	 * @param i
	 *            index of the Vertex
	 * @return the number of edges attached to the Vertex at index i.
	 */

	public int getAttachedEdgeCount(int i) {
		if (i < 0 || i > attachedEdges.size() - 1)
			return 0;
		return attachedEdges.get(i).size();
	}

	public BufferedImage toImage(int width, int height, Layer backgroundLayer) {
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = img.createGraphics();
		
		BasicStroke roadStroke = new BasicStroke(3f);
		BasicStroke streetStroke = new BasicStroke(1f);
		double scaleX = (double) width / backgroundLayer.getResolutionX();
		double scaleY = (double) height / backgroundLayer.getResolutionY();

		if (backgroundLayer != null) {
			g2.drawImage(backgroundLayer.getImage(), 0, 0, width, height, null);
		}
		for (Edge e : edges) {
			Vertex v1 = vertices.get(e.vertices.getFirst());
			Vertex v2 = vertices.get(e.vertices.getSecond());

			g2.setColor(e.roadType == RoadType.ROAD ? Color.red : Color.blue);

			int x1, x2, y1, y2;
			x1 = (int) (v1.x * scaleX);
			y1 = (int) (height - scaleY * v1.y);
			x2 = (int) (v2.x * scaleX);
			y2 = (int) (height - scaleY * v2.y);

			// logger.debug("drawing " + e.roadType + ": (" + x1 + ", " + y1 +
			// ")-(" + x2 + "," + y2 + ") @scale (" + scaleX + ", " + scaleY +
			// ")");
			g2.setStroke(e.roadType == RoadType.ROAD ? roadStroke : streetStroke);

			g2.drawLine(x1, y1, x2, y2);
			
			g2.setColor(Color.MAGENTA);
			 g2.drawRect((int) x1-1, (int) y1-1, 1,1);
			 g2.drawRect((int) x2-1, (int) y2-1, 1,1);
			// g2.setColor(Color.red);
			// g2.drawString("" +
			// getAttachedEdges(e.vertices.getSecond()).size(),
			// (int) (scaleX * (v2.x - 2)), (int) (scaleY * (rY - v2.y - 2)));
		}
		return img;
	}
}
