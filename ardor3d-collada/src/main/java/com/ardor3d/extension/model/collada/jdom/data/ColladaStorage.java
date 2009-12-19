package com.ardor3d.extension.model.collada.jdom.data;

import com.ardor3d.scenegraph.Node;
import com.google.common.collect.Lists;

import java.util.List;

public class ColladaStorage {
	private Node scene;
	private AssetData assetData;
	private final List<SkinData> skins = Lists.newArrayList();

	public Node getScene() {
		return scene;
	}

	public void setScene(Node scene) {
		this.scene = scene;
	}

	public AssetData getAssetData() {
		return assetData;
	}

	public void setAssetData(AssetData assetData) {
		this.assetData = assetData;
	}

	public List<SkinData> getSkins() {
		return skins;
	}
}
