package com.ardor3d.extension.model.collada.jdom;

import com.ardor3d.extension.model.collada.jdom.data.AssetData;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.DataCache;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.resource.RelativeResourceLocator;
import com.ardor3d.util.resource.ResourceLocator;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;
import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.DefaultJDOMFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.input.SAXHandler;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class ColladaImporter {
	private boolean loadTextures = true;
	private ResourceLocator textureLocator;
	private ResourceLocator modelLocator;

	public ColladaImporter loadTextures(boolean loadTextures) {
		this.loadTextures = loadTextures;
		return this;
	}

	public ColladaImporter textureLocator(ResourceLocator textureLocator) {
		this.textureLocator = textureLocator;
		return this;
	}

	public ColladaImporter modelLocator(ResourceLocator modelLocator) {
		this.modelLocator = modelLocator;
		return this;
	}

	public ColladaStorage load(String resource) {
		final ResourceSource source;
		if (modelLocator == null) {
			source = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL, resource);
		} else {
			source = modelLocator.locateResource(resource);
		}

		if (source == null) {
			throw new Error("Unable to locate '" + resource + "'");
		}

		return load(source);

	}

	private ColladaStorage load(ResourceSource resource) {

		ColladaStorage colladaStorage = new ColladaStorage();
		DataCache dataCache = new DataCache();
		ColladaDOMUtil colladaDOMUtil = new ColladaDOMUtil(dataCache);
		ColladaMaterialUtils colladaMaterialUtils = new ColladaMaterialUtils(loadTextures, dataCache, colladaDOMUtil, textureLocator);
		ColladaMeshUtils colladaMeshUtils = new ColladaMeshUtils(dataCache, colladaDOMUtil, colladaMaterialUtils);
		ColladaAnimUtils colladaAnumUtils = new ColladaAnimUtils(colladaStorage, dataCache, colladaDOMUtil, colladaMeshUtils);
		ColladaNodeUtils colladaNodeUtils = new ColladaNodeUtils(dataCache, colladaDOMUtil, colladaMaterialUtils, colladaMeshUtils, colladaAnumUtils);

		try {

			// Pull in the DOM tree of the Collada resource.
			final Element collada = readCollada(resource, dataCache);

			// if we don't specify a texture locator, add a temporary texture locator at the location of this model
			// resource..
			final boolean addLocator = textureLocator != null;

			final RelativeResourceLocator loc;
			if (addLocator) {
				loc = new RelativeResourceLocator(resource);
				ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, loc);
			} else {
				loc = null;
			}

			final AssetData assetData = colladaNodeUtils.parseAsset(collada.getChild("asset"));

			// Collada may or may not have a scene, so this can return null.
			final Node scene = colladaNodeUtils.getVisualScene(collada);

			// Pull out our storage

			// set our scene into storage
			colladaStorage.setScene(scene);

			// set our asset data into storage
			colladaStorage.setAssetData(assetData);


			// drop our added locator if needed.
			if (addLocator) {
				ResourceLocatorTool.removeResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, loc);
			}

			// return storage
			return colladaStorage;
		} catch (final Exception e) {
			throw new RuntimeException("Unable to load collada resource from URL: " + resource, e);
		}
	}

	private Element readCollada(final ResourceSource resource, final DataCache dataCache) {
		try {
			final SAXBuilder builder = new SAXBuilder() {
				@Override
				protected SAXHandler createContentHandler() {
					return new SAXHandler(new ArdorFactory(dataCache)) {
						@Override
						public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
							// Just kill what's usually done here...
						}

					};
				}
			};
			//
			// final SAXBuilder builder = new SAXBuilder();
			// builder.setFactory(new ArdorSimpleFactory());

			final Document doc = builder.build(resource.openStream());
			final Element collada = doc.getRootElement();

			// ColladaDOMUtil.stripNamespace(collada);

			return collada;
		} catch (final Exception e) {
			throw new RuntimeException("Unable to load collada resource from source: " + resource, e);
		}
	}

	final class ArdorSimpleFactory extends DefaultJDOMFactory {
		private DataCache dataCache;

		ArdorSimpleFactory(DataCache dataCache) {
			this.dataCache = dataCache;
		}

		@Override
		public Text text(final String text) {
			return new Text(Text.normalizeString(text));
		}

		@Override
		public void setAttribute(final Element parent, final Attribute a) {
			if ("id".equals(a.getName())) {
				dataCache.getIdCache().put(a.getValue(), parent);
			} else if ("sid".equals(a.getName())) {
				dataCache.getSidCache().put(a.getValue(), parent);
			}

			super.setAttribute(parent, a);
		}
	}

	private enum BufferType {
		None, Float, Double, Int, String, P
	}


	/**
	 * A JDOMFactory that normalizes all text (strips extra whitespace etc)
	 */
	final class ArdorFactory extends DefaultJDOMFactory {
		private DataCache dataCache;
		private Element currentElement;
		private BufferType bufferType = BufferType.None;
		private int count = 0;
		private final List<String> list = new ArrayList<String>();

		ArdorFactory(DataCache dataCache) {
			this.dataCache = dataCache;
		}

		@Override
		public Text text(final String text) {
			switch (bufferType) {
				case Float: {
					final String normalizedText = Text.normalizeString(text);
					if (normalizedText.length() == 0) {
						return new Text("");
					}
					final StringTokenizer tokenizer = new StringTokenizer(normalizedText, " ");
					final float[] floatArray = new float[count];
					for (int i = 0; i < count; i++) {
						floatArray[i] = Float.parseFloat(tokenizer.nextToken().replace(",", "."));
					}

					dataCache.getFloatArrays().put(currentElement, floatArray);

					return new Text("");
				}
				case Double: {
					final String normalizedText = Text.normalizeString(text);
					if (normalizedText.length() == 0) {
						return new Text("");
					}
					final StringTokenizer tokenizer = new StringTokenizer(normalizedText, " ");
					final double[] doubleArray = new double[count];
					for (int i = 0; i < count; i++) {
						doubleArray[i] = Double.parseDouble(tokenizer.nextToken().replace(",", "."));
					}

					dataCache.getDoubleArrays().put(currentElement, doubleArray);

					return new Text("");
				}
				case Int: {
					final String normalizedText = Text.normalizeString(text);
					if (normalizedText.length() == 0) {
						return new Text("");
					}
					final StringTokenizer tokenizer = new StringTokenizer(normalizedText, " ");
					final int[] intArray = new int[count];
					int i = 0;
					while (tokenizer.hasMoreTokens()) {
						intArray[i++] = Integer.parseInt(tokenizer.nextToken());
					}

					dataCache.getIntArrays().put(currentElement, intArray);

					return new Text("");
				}
				case P: {
					list.clear();
					final String normalizedText = Text.normalizeString(text);
					if (normalizedText.length() == 0) {
						return new Text("");
					}
					final StringTokenizer tokenizer = new StringTokenizer(normalizedText, " ");
					while (tokenizer.hasMoreTokens()) {
						list.add(tokenizer.nextToken());
					}
					final int listSize = list.size();
					final int[] intArray = new int[listSize];
					for (int i = 0; i < listSize; i++) {
						intArray[i] = Integer.parseInt(list.get(i));
					}

					dataCache.getIntArrays().put(currentElement, intArray);

					return new Text("");
				}
			}
			return new Text(Text.normalizeString(text));
		}

		@Override
		public void setAttribute(final Element parent, final Attribute a) {
			if ("id".equals(a.getName())) {
				dataCache.getIdCache().put(a.getValue(), parent);
			} else if ("sid".equals(a.getName())) {
				dataCache.getSidCache().put(a.getValue(), parent);
			} else if ("count".equals(a.getName())) {
				try {
					count = a.getIntValue();
				} catch (final DataConversionException e) {
					e.printStackTrace();
				}
			}

			super.setAttribute(parent, a);
		}

		@Override
		public Element element(final String name, final Namespace namespace) {
			currentElement = super.element(name);
			handleTypes(name);
			return currentElement;
		}

		@Override
		public Element element(final String name, final String prefix, final String uri) {
			currentElement = super.element(name);
			handleTypes(name);
			return currentElement;
		}

		@Override
		public Element element(final String name, final String uri) {
			currentElement = super.element(name);
			handleTypes(name);
			return currentElement;
		}

		@Override
		public Element element(final String name) {
			currentElement = super.element(name);
			handleTypes(name);
			return currentElement;
		}

		private void handleTypes(final String name) {
			if ("float_array".equals(name)) {
				bufferType = BufferType.Float;
			} else if ("double_array".equals(name)) {
				bufferType = BufferType.Double;
			} else if ("int_array".equals(name)) {
				bufferType = BufferType.Int;
			} else if ("p".equals(name)) {
				bufferType = BufferType.P;
			} else {
				bufferType = BufferType.None;
			}
		}
	}
}
