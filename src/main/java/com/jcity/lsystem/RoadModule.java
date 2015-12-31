package com.jcity.lsystem;

import javax.vecmath.Vector2d;

import com.jcity.roads.RoadType;

/**
 * Represents an L-System parameterized RoadModule, with a unique ID.
 * 
 * @author philippd
 * 
 */
// TODO: To be a *true* L-System Module, this should be much more generic,
// emulating an array, rather than specific properties, like vertexId or
// roadType. Subclasses can overlay the indexed values with property names.

public class RoadModule {
	// 0 - tDelay, or sometimes originVertexId
	// 1 - VertexID
	// 2 - Type (ROAD, STREET or CURB), or sometimes directionX
	// 3 - Status (ACCEPT or DELETE), or sometimes directionY
	// 4 - roadType? (ROAD, STREET or CURB) - for Branch modules, determines the
	// type of child road.

	public char id;

	public int vertexId;
	public int originVertexId = 0;
	// public Vertex vertex;
	public RoadType roadType = RoadType.STREET;
	public long delay = 0L; // When delay gets down to zero, the RoadModule will
							// be converted to a Road, and deleted from the
							// String.
	public ModuleStatus status = ModuleStatus.DELETE;
	public RoadType roadType2 = RoadType.STREET;
	// public double directionX=0D;
	// public double directionY=0D;
	public Vector2d direction = new Vector2d(0, 0);

	// public Vertex originVertex;

	public RoadModule(RoadModule m) {
		this.id = m.id;
		set(m);
	}

	public RoadModule(char id) {
		this.id = id;
	}

	/**
	 * Compare two modules. Modules are equivalent if they have the same ID (B
	 * or R)
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof RoadModule) {
			RoadModule m = (RoadModule) o;
			return m.id == this.id;
		}
		return false;
	}

	public String toString() {
		return "" + this.id + " (" + this.delay + this.vertexId + this.roadType.toString() + this.status.toString() + this.roadType2.toString() + ")\n";
	}

	public void set(RoadModule m) {
		// this.id = m.id;
		this.vertexId = m.vertexId;
		// this.vertex = m.vertex;
		this.roadType = m.roadType;
		this.delay = m.delay;
		this.roadType2 = m.roadType2;
		this.status = m.status;
		this.direction = new Vector2d(m.direction);
		this.originVertexId = m.originVertexId;
	}
}
