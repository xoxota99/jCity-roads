package com.jcity.roads;

public enum RoadType {
	STREET(1), ROAD(2), CURB(0);
	private int value;

	private RoadType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
};
