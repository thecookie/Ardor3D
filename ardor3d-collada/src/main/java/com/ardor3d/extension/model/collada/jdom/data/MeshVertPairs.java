package com.ardor3d.extension.model.collada.jdom.data;

import com.ardor3d.scenegraph.Mesh;

public class MeshVertPairs {
	private final Mesh mesh;
	private final int[] indices;

	public MeshVertPairs(final Mesh mesh, final int[] indices) {
		this.mesh = mesh;
		this.indices = indices;
	}

	public Mesh getMesh() {
		return mesh;
	}

	public int[] getIndices() {
		return indices;
	}
}
