package com.jcity.roads;

import javax.vecmath.*;

public class Vertex extends Point3d {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4112293596596924014L;
	public int belongsTo = -1;

	public Vertex() {

	}

	public Vertex(Point3d p) {
		super(p);
	}

	public Vertex(Vertex v) {
		super(v);
		this.belongsTo = v.belongsTo;
	}
}
