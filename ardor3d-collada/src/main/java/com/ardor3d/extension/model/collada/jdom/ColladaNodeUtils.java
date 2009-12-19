package com.ardor3d.extension.model.collada.jdom;

import com.ardor3d.extension.model.collada.jdom.data.AssetData;
import com.ardor3d.extension.model.collada.jdom.data.DataCache;
import com.ardor3d.extension.model.collada.jdom.data.NodeType;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.Vector4;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ColladaNodeUtils {
	private static final Logger logger = Logger.getLogger(ColladaNodeUtils.class.getName());

	private DataCache dataCache;
	private ColladaDOMUtil colladaDOMUtil;
	private ColladaMaterialUtils colladaMaterialUtils;
	private ColladaMeshUtils colladaMeshUtils;
	private ColladaAnimUtils colladaAnimUtils;

	public ColladaNodeUtils(DataCache dataCache, ColladaDOMUtil colladaDOMUtil, ColladaMaterialUtils colladaMaterialUtils,
									ColladaMeshUtils colladaMeshUtils, ColladaAnimUtils colladaAnimUtils) {
		this.dataCache = dataCache;
		this.colladaDOMUtil = colladaDOMUtil;
		this.colladaMaterialUtils = colladaMaterialUtils;
		this.colladaMeshUtils = colladaMeshUtils;
		this.colladaAnimUtils = colladaAnimUtils;
	}

	/**
	 * Retrieves the scene and returns it as an Ardor3D Node.
	 *
	 * @param colladaRoot The collada root element
	 * @return Scene as an Node or null if not found
	 */
	@SuppressWarnings("unchecked")
	public Node getVisualScene(final Element colladaRoot) {
		if (colladaRoot.getChild("scene") == null) {
			logger.warning("No scene found in collada file!");
			return null;
		}

		final Element instance_visual_scene = colladaRoot.getChild("scene").getChild("instance_visual_scene");
		if (instance_visual_scene == null) {
			logger.warning("No instance_visual_scene found in collada file!");
			return null;
		}

		final Element visualScene = colladaDOMUtil.findTargetWithId(instance_visual_scene.getAttributeValue("url"));

		if (visualScene != null) {
			final Node sceneRoot = new Node(visualScene.getAttributeValue("name") != null ? visualScene.getAttributeValue("name") : "Collada Root");

			// Load each sub node and attach
			for (final Element n : (List<Element>) visualScene.getChildren("node")) {
				NodeType nodeType = NodeType.NODE;
				if (n.getAttribute("type") != null) {
					nodeType = Enum.valueOf(NodeType.class, n.getAttributeValue("type"));
				}
				if (nodeType == NodeType.NODE) {
					final Node subNode = buildNode(n);
					if (subNode != null) {
						sceneRoot.attachChild(subNode);
					}
				}
			}

			return sceneRoot;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public AssetData parseAsset(final Element asset) {
		final AssetData assetData = new AssetData();

		for (final Element child : (List<Element>) asset.getChildren()) {
			if ("contributor".equals(child.getName())) {
				parseContributor(assetData, child);
			} else if ("created".equals(child.getName())) {
				assetData.setCreated(child.getText());
			} else if ("kewords".equals(child.getName())) {
				assetData.setKeywords(child.getText());
			} else if ("modified".equals(child.getName())) {
				assetData.setModified(child.getText());
			} else if ("revision".equals(child.getName())) {
				assetData.setRevision(child.getText());
			} else if ("subject".equals(child.getName())) {
				assetData.setSubject(child.getText());
			} else if ("title".equals(child.getName())) {
				assetData.setTitle(child.getText());
			} else if ("unit".equals(child.getName())) {
				assetData.setUnitName(child.getAttributeValue("name"));
				assetData.setUnitMeter(Float.parseFloat(child.getAttribute("meter").getValue().replace(",", ".")));
			} else if ("up_axis".equals(child.getName())) {
				final String axis = child.getText();
				if ("X_UP".equals(axis)) {
					assetData.setUpAxis(new Vector3());
				} else if ("Y_UP".equals(axis)) {
					assetData.setUpAxis(Vector3.UNIT_Y);
				} else if ("Z_UP".equals(axis)) {
					assetData.setUpAxis(Vector3.UNIT_Z);
				}
			}
		}

		return assetData;
	}

	@SuppressWarnings("unchecked")
	private void parseContributor(final AssetData assetData, final Element contributor) {
		for (final Element child : (List<Element>) contributor.getChildren()) {
			if ("author".equals(child.getName())) {
				assetData.setAuthor(child.getText());
			} else if ("authoringTool".equals(child.getName())) {
				assetData.setCreated(child.getText());
			} else if ("comments".equals(child.getName())) {
				assetData.setComments(child.getText());
			} else if ("copyright".equals(child.getName())) {
				assetData.setCopyright(child.getText());
			} else if ("source_data".equals(child.getName())) {
				assetData.setSourceData(child.getText());
			}
		}
	}

	/**
	 * @param instanceNode
	 * @return a new Ardor3D node, created from the <node> pointed to by the given <instance_node> element
	 */
	public Node getNode(final Element instanceNode) {
		final Element node = colladaDOMUtil.findTargetWithId(instanceNode.getAttributeValue("url"));

		if (node == null) {
			throw new ColladaException("No node with id: " + instanceNode.getAttributeValue("url") + " found",
					instanceNode);
		}

		return buildNode(node);
	}

	/**
	 * @param dNode
	 * @return a new Ardor3D node, created from the given <node> element
	 */
	@SuppressWarnings("unchecked")
	public Node buildNode(final Element dNode) {
		final Node node = new Node(dNode.getAttributeValue("name", dNode.getName()));

		final List<Element> transforms = new ArrayList<Element>();
		for (final Element child : (List<Element>) dNode.getChildren()) {
			if (dataCache.getTransformTypes().contains(child.getName())) {
				transforms.add(child);
			}
		}

		// process any transform information.
		if (!transforms.isEmpty()) {
			final Transform localTransform = getNodeTransforms(transforms);

			node.setTransform(localTransform);
		}

		// process any instance geometries
		for (final Element instance_geometry : (List<Element>) dNode.getChildren("instance_geometry")) {
			colladaMaterialUtils.bindMaterials(instance_geometry.getChild("bind_material"));

			final Spatial mesh = colladaMeshUtils.getGeometryMesh(instance_geometry);
			if (mesh != null) {
				node.attachChild(mesh);
			}

			colladaMaterialUtils.unbindMaterials(instance_geometry.getChild("bind_material"));
		}

		// process any instance controllers
		for (final Element ic : (List<Element>) dNode.getChildren("instance_controller")) {
			colladaMaterialUtils.bindMaterials(ic.getChild("bind_material"));

			final Element controller = colladaDOMUtil.findTargetWithId(ic.getAttributeValue("url"));

			if (controller == null) {
				throw new ColladaException("Unable to find controller with id: " + ic.getAttributeValue("url")
						+ ", referenced from node " + dNode, dNode);
			}

			final Element skin = controller.getChild("skin");
			if (skin != null) {
				colladaAnimUtils.buildSkinMeshes(node, ic, controller, skin);
			} else {
				// look for morph... can only be one or the other according to Collada
				final Element morph = controller.getChild("morph");
				if (morph != null) {
					colladaAnimUtils.buildMorphMeshes(node, controller, morph);
				}
			}

			colladaMaterialUtils.unbindMaterials(ic.getChild("bind_material"));
		}

		// process any instance nodes
		for (final Element in : (List<Element>) dNode.getChildren("instance_node")) {
			final Node subNode = getNode(in);
			if (subNode != null) {
				node.attachChild(subNode);
			}
		}

		// process any concrete child nodes.
		for (final Element n : (List<Element>) dNode.getChildren("node")) {
			final Node subNode = buildNode(n);
			if (subNode != null) {
				node.attachChild(subNode);
			}
		}

		return node;
	}

	/**
	 * Combines a list of transform elements into an Ardor3D Transform object.
	 *
	 * @param transforms List of transform elements
	 * @return an Ardor3D Transform object
	 */
	private Transform getNodeTransforms(final List<Element> transforms) {
		final Matrix4 workingMat = Matrix4.fetchTempInstance();
		final Matrix4 finalMat = Matrix4.fetchTempInstance();
		finalMat.setIdentity();
		for (final Element transform : transforms) {
			final double[] array = colladaDOMUtil.parseDoubleArray(transform);
			if ("translate".equals(transform.getName())) {
				workingMat.setIdentity();
				workingMat.setColumn(3, new double[]{array[0], array[1], array[2], 1});
				finalMat.multiplyLocal(workingMat);
			} else if ("rotate".equals(transform.getName())) {
				if (array[3] != 0) {
					workingMat.setIdentity();
					final Matrix3 rotate = new Matrix3().fromAngleAxis(array[3] * MathUtils.DEG_TO_RAD, new Vector3(
							array[0], array[1], array[2]));
					workingMat.set(rotate);
					finalMat.multiplyLocal(workingMat);
				}
			} else if ("scale".equals(transform.getName())) {
				workingMat.setIdentity();
				workingMat.scale(new Vector4(array[0], array[1], array[2], 1), workingMat);
				finalMat.multiplyLocal(workingMat);
			} else if ("matrix".equals(transform.getName())) {
				workingMat.fromArray(array);
				finalMat.multiplyLocal(workingMat);
			} else if ("lookat".equals(transform.getName())) {
				final Vector3 pos = new Vector3(array[0], array[1], array[2]);
				final Vector3 target = new Vector3(array[3], array[4], array[5]);
				final Vector3 up = new Vector3(array[6], array[7], array[8]);
				final Matrix3 rot = new Matrix3();
				rot.lookAt(target.subtractLocal(pos), up);
				workingMat.set(rot);
				workingMat.setColumn(3, new double[]{array[0], array[1], array[2], 1});
				finalMat.multiplyLocal(workingMat);
			} else {
				ColladaNodeUtils.logger.warning("transform not currently supported: "
						+ transform.getClass().getCanonicalName());
			}
		}
		return new Transform().fromHomogeneousMatrix(finalMat);
	}
}
