package com.ardor3d.extension.model.collada.jdom.data;

import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.scenegraph.Node;
import com.google.common.collect.Lists;

import java.util.List;

public class SkinData {

	private SkeletonPose pose;
	private Node skinBaseNode;
	private final List<SkinnedMesh> skins = Lists.newArrayList();
	private final String name;

	public SkinData(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public SkeletonPose getPose() {
		return pose;
	}

	public void setPose(SkeletonPose pose) {
		this.pose = pose;
	}

	public Node getSkinBaseNode() {
		return skinBaseNode;
	}

	public void setSkinBaseNode(Node skinBaseNode) {
		this.skinBaseNode = skinBaseNode;
	}

	public List<SkinnedMesh> getSkins() {
		return skins;
	}
}
