package com.ardor3d.extension.model.collada.jdom;

import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.geom.BufferUtils;
import org.jdom.Element;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ColladaInputPipe {
	private static final Logger logger = Logger.getLogger(ColladaInputPipe.class.getName());

	private final int offset;
	private final int set;
	private final Element source;
	private int paramCount;
	private SourceData sourceData = null;
	private Type type;
	private FloatBuffer buffer;
	private int texCoord = 0;

	public enum Type {
		VERTEX, NORMAL, TEXCOORD, COLOR, JOINT, WEIGHT, INV_BIND_MATRIX, UNKNOWN
	}

	class SourceData {
		int count;
		int stride;
		int offset;

		ParamType paramType;
		float[] floatArray;
		boolean[] boolArray;
		int[] intArray;
		String[] stringArray;
	}

	public enum ParamType {
		float_param, bool_param, int_param, name_param, idref_param
	}

	@SuppressWarnings("unchecked")
	public ColladaInputPipe(ColladaDOMUtil colladaDOMUtil, final Element input) {
		// Setup our type
		try {
			type = Type.valueOf(input.getAttributeValue("semantic"));
		} catch (final Exception ex) {
			ColladaInputPipe.logger.warning("Unknown input type: " + input.getAttributeValue("semantic"));
			type = Type.UNKNOWN;
		}

		// Locate our source
		final Element n = colladaDOMUtil.findTargetWithId(input.getAttributeValue("source"));
		if (n == null) {
			throw new ColladaException("Input source not found: " + input.getAttributeValue("source"), input);
		}

		if ("source".equals(n.getName())) {
			source = n;
		} else if ("vertices".equals(n.getName())) {
			source = colladaDOMUtil.getPositionSource(n);
		} else {
			throw new ColladaException("Input source not found: " + input.getAttributeValue("source"), input);
		}

		// TODO: Need to go through the params and see if they have a name set, and skip values if not when
		// parsing the array?

		sourceData = new SourceData();
		if (source.getChild("float_array") != null) {
			sourceData.floatArray = colladaDOMUtil.parseFloatArray(source.getChild("float_array"));
			sourceData.paramType = ParamType.float_param;
		} else if (source.getChild("bool_array") != null) {
			sourceData.boolArray = colladaDOMUtil.parseBooleanArray(source.getChild("bool_array"));
			sourceData.paramType = ParamType.bool_param;
		} else if (source.getChild("int_array") != null) {
			sourceData.intArray = colladaDOMUtil.parseIntArray(source.getChild("int_array"));
			sourceData.paramType = ParamType.int_param;
		} else if (source.getChild("Name_array") != null) {
			sourceData.stringArray = colladaDOMUtil.parseStringArray(source.getChild("Name_array"));
			sourceData.paramType = ParamType.name_param;
		} else if (source.getChild("IDREF_array") != null) {
			sourceData.stringArray = colladaDOMUtil.parseStringArray(source.getChild("IDREF_array"));
			sourceData.paramType = ParamType.idref_param;
		}

		// add a hook to our params from the technique_common
		final Element accessor = getCommonAccessor(source);
		if (accessor != null) {
			if (ColladaInputPipe.logger.isLoggable(Level.FINE)) {
				ColladaInputPipe.logger.fine("Creating buffers for: " + source.getAttributeValue("id"));
			}

			final List<Element> params = accessor.getChildren("param");
			paramCount = params.size();

			// Might use this info for real later, but use for testing for unsupported param skipping.
			boolean skippedParam = false;
			for (final Element param : params) {
				final String paramName = param.getAttributeValue("name");
				if (paramName == null) {
					skippedParam = true;
					break;
				}
				// String paramType = param.getAttributeValue("type");
			}
			if (paramCount > 1 && skippedParam) {
				ColladaInputPipe.logger.warning("Parameter skipping not yet supported when parsing sources. "
						+ source.getAttributeValue("id"));
			}

			sourceData.count = colladaDOMUtil.getAttributeIntValue(accessor, "count", 0);
			sourceData.stride = colladaDOMUtil.getAttributeIntValue(accessor, "stride", 1);
			sourceData.offset = colladaDOMUtil.getAttributeIntValue(accessor, "offset", 0);
		}

		// save our offset
		offset = colladaDOMUtil.getAttributeIntValue(input, "offset", 0);
		set = colladaDOMUtil.getAttributeIntValue(input, "set", 0);

		texCoord = 0;
	}

	public int getOffset() {
		return offset;
	}

	public int getSet() {
		return set;
	}

	public Type getType() {
		return type;
	}

	public SourceData getSourceData() {
		return sourceData;
	}

	public void setupBuffer(final int numEntries, final MeshData meshData) {
		// use our source and the number of params to determine our buffer length
		// we'll use the params from the common technique accessor:
		final int size = paramCount * numEntries;
		switch (type) {
			case VERTEX:
				buffer = BufferUtils.createFloatBuffer(size);
				meshData.setVertexBuffer(buffer);
				break;
			case NORMAL:
				buffer = BufferUtils.createFloatBuffer(size);
				meshData.setNormalBuffer(buffer);
				break;
			case TEXCOORD:
				buffer = BufferUtils.createFloatBuffer(size);
				// TODO: set is not right?
				meshData.setTextureCoords(new FloatBufferData(buffer, paramCount), texCoord++);
				break;
			case COLOR:
				buffer = BufferUtils.createFloatBuffer(size);
				meshData.setColorBuffer(buffer);
				break;
			default:
		}
	}

	void pushValues(final int memberIndex) {
		if (buffer == null) {
			return;
		}

		if (sourceData == null) {
			throw new ColladaException("No source data found in pipe!", source);
		}

		if (memberIndex >= sourceData.count) {
			ColladaInputPipe.logger.warning("Accessed invalid count " + memberIndex + " on source " + source + ".");
			return;
		}

		int index = memberIndex * sourceData.stride + sourceData.offset;
		final ParamType paramType = sourceData.paramType;
		for (int i = 0; i < paramCount; i++) {
			if (ParamType.float_param == paramType) {
				buffer.put(sourceData.floatArray[index]);
			} else if (ParamType.int_param == paramType) {
				buffer.put(sourceData.intArray[index]);
			}
			index++;
		}
	}


	private Element getCommonAccessor(final Element source) {
		final Element techniqueCommon = source.getChild("technique_common");
		if (techniqueCommon != null) {
			return techniqueCommon.getChild("accessor");
		}
		return null;
	}

}
