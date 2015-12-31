package com.jcity.lsystem;

/**
 * Represents an L-System parameterized Module, with a unique ID.
 * 
 * @author philippd
 * 
 */

public class Module {
	private double[] params;
	private char id;

	public Module(char id, int paramCount) {
		this.id = id;
		this.params = new double[paramCount];
	}

	public Module(Module m) {
		this.id=m.id;
		params = new double[m.params.length];
		System.arraycopy(m.params, 0, params, 0, m.params.length);
	}

	public double getParam(int idx) {
		return params[idx];
	}
}
