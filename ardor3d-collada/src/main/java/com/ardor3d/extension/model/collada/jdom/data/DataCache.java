package com.ardor3d.extension.model.collada.jdom.data;

import com.ardor3d.image.Texture;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.jdom.Element;
import org.jdom.xpath.XPath;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class DataCache {
	private final Map<String, Element> boundMaterials;
	private final Map<String, Texture> textures;
	private final Map<String, Element> idCache;
	private final Map<String, Element> sidCache;
	private final Map<String, XPath> xPathExpressions;
	private final Pattern pattern;
	private final List<String> transformTypes;

	private final Map<Element, float[]> floatArrays;
	private final Map<Element, double[]> doubleArrays;
	private final Map<Element, boolean[]> booleanArrays;
	private final Map<Element, int[]> intArrays;
	private final Map<Element, String[]> stringArrays;

	private final Multimap<Element, MeshVertPairs> vertMappings;

	public DataCache() {
		boundMaterials = Maps.newHashMap();
		textures = Maps.newHashMap();
		idCache = Maps.newHashMap();
		sidCache = Maps.newHashMap();
		xPathExpressions = Maps.newHashMap();
		pattern = Pattern.compile("\\s");

		transformTypes = Collections.unmodifiableList(Lists.newArrayList("lookat", "matrix", "rotate", "scale", "scew", "translate"));

		floatArrays = Maps.newHashMap();
		doubleArrays = Maps.newHashMap();
		booleanArrays = Maps.newHashMap();
		intArrays = Maps.newHashMap();
		stringArrays = Maps.newHashMap();
		vertMappings = ArrayListMultimap.create();

	}

	public void bindMaterial(final String ref, final Element material) {
		if (!boundMaterials.containsKey(ref)) {
			boundMaterials.put(ref, material);
		}
	}


	public void unbindMaterial(final String ref) {
		boundMaterials.remove(ref);
	}

	public Element getBoundMaterial(final String ref) {
		return boundMaterials.get(ref);
	}

	public boolean containsTexture(final String path) {
		return textures.containsKey(path);
	}

	public void addTexture(final String path, final Texture texture) {
		textures.put(path, texture);
	}

	public Texture getTexture(final String path) {
		return textures.get(path);
	}

	public Map<String, Element> getIdCache() {
		return idCache;
	}

	public Map<String, Element> getSidCache() {
		return sidCache;
	}

	public Map<String, XPath> getxPathExpressions() {
		return xPathExpressions;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public List<String> getTransformTypes() {
		return transformTypes;
	}

	public Map<Element, float[]> getFloatArrays() {
		return floatArrays;
	}

	public Map<Element, double[]> getDoubleArrays() {
		return doubleArrays;
	}

	public Map<Element, boolean[]> getBooleanArrays() {
		return booleanArrays;
	}

	public Map<Element, int[]> getIntArrays() {
		return intArrays;
	}

	public Map<Element, String[]> getStringArrays() {
		return stringArrays;
	}

	public Multimap<Element, MeshVertPairs> getVertMappings() {
		return vertMappings;
	}


}
