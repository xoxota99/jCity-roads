package com.jcity.roads;

import com.jcity.util.*;

/**
 * An undirected edge in a graph. Stores a pair of integers, which are used to lookup vertices in a global vertex table.
 * 
 * @author philippd
 *
 */
public class Edge {

	public Pair<Integer,Integer> vertices = new Pair<>();
	public Pair<Integer,Integer> curbs = new Pair<>();
	public RoadType roadType = RoadType.ROAD;

	public boolean equals(Edge e) {
		return (((vertices.getFirst() == e.vertices.getFirst()) && (vertices.getSecond() == e.vertices.getSecond()))
		|| ((vertices.getFirst() == e.vertices.getSecond()) && (vertices.getSecond() == e.vertices.getFirst())));
	}
}
