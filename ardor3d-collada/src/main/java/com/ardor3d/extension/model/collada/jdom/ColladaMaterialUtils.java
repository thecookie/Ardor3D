package com.ardor3d.extension.model.collada.jdom;

import com.ardor3d.extension.model.collada.jdom.data.DataCache;
import com.ardor3d.extension.model.collada.jdom.data.SamplerTypes;
import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.resource.ResourceLocator;
import com.ardor3d.util.resource.ResourceSource;
import org.jdom.Element;

import java.util.List;
import java.util.logging.Logger;

public class ColladaMaterialUtils {
	private static final Logger logger = Logger.getLogger(ColladaMaterialUtils.class.getName());
	private final boolean loadTextures;
	private DataCache dataCache;
	private ColladaDOMUtil colladaDOMUtil;
	private ResourceLocator textureLocator;

	public ColladaMaterialUtils(boolean loadTextures, DataCache dataCache, ColladaDOMUtil colladaDOMUtil, ResourceLocator textureLocator) {
		this.loadTextures = loadTextures;
		this.dataCache = dataCache;
		this.colladaDOMUtil = colladaDOMUtil;
		this.textureLocator = textureLocator;
	}

	/**
	 * Find and apply the given material to the given Mesh.
	 *
	 * @param materialName our material name
	 * @param mesh			the mesh to apply material to.
	 */
	public void applyMaterial(final String materialName, final Mesh mesh) {
		if (materialName == null) {
			ColladaMaterialUtils.logger.warning("materialName is null");
			return;
		}

		Element mat = dataCache.getBoundMaterial(materialName);
		if (mat == null) {
			ColladaMaterialUtils.logger.warning("material not bound: " + materialName + ", trying search with id.");
			mat = colladaDOMUtil.findTargetWithId(materialName);
		}
		if (mat == null) {
			ColladaMaterialUtils.logger.warning("material not found: " + materialName);
			return;
		}

		final Element effectNode = colladaDOMUtil.findTargetWithId(mat.getChild("instance_effect").getAttributeValue("url"));
		if (effectNode == null) {
			logger.warning("material effect not found: " + mat.getChild("instance_material").getAttributeValue("url"));
		}

		if ("effect".equals(effectNode.getName())) {
			final Element effect = effectNode;
			// XXX: For now, just grab the common technique:
			final Element technique = effect.getChild("profile_COMMON").getChild("technique");
			if (technique.getChild("blinn") != null || technique.getChild("phong") != null
					|| technique.getChild("lambert") != null) {
				final Element blinnPhong = technique.getChild("blinn") != null ? technique.getChild("blinn")
						: technique.getChild("phong") != null ? technique.getChild("phong") : technique
						.getChild("lambert");
				final MaterialState mState = new MaterialState();

				Texture diffuseTexture = null;
				ColorRGBA transparent = new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f);
				float transparency = 1.0f;
				boolean useTransparency = false;
				String opaqueMode = "A_ONE";

				for (final Element property : (List<Element>) blinnPhong.getChildren()) {
					if ("diffuse".equals(property.getName())) {
						final Element propertyValue = (Element) property.getChildren().get(0);
						if ("color".equals(propertyValue.getName())) {
							final ColorRGBA color = colladaDOMUtil.getColor(propertyValue.getText());
							mState.setDiffuse(color);
						} else if ("texture".equals(propertyValue.getName()) && loadTextures) {
							TextureState tState = (TextureState) mesh.getLocalRenderState(RenderState.StateType.Texture);
							if (tState == null) {
								tState = new TextureState();
								mesh.setRenderState(tState);
							}
							diffuseTexture = populateTextureState(tState, propertyValue, effect);
						}
					} else if ("ambient".equals(property.getName())) {
						final Element propertyValue = (Element) property.getChildren().get(0);
						if ("color".equals(propertyValue.getName())) {
							final ColorRGBA color = colladaDOMUtil.getColor(propertyValue.getText());
							mState.setAmbient(color);
						}
					} else if ("transparent".equals(property.getName())) {
						final Element propertyValue = (Element) property.getChildren().get(0);
						if ("color".equals(propertyValue.getName())) {
							transparent = colladaDOMUtil.getColor(propertyValue.getText());
							// TODO: use this

							useTransparency = true;
						}
						opaqueMode = property.getAttributeValue("opaque", "A_ONE");
					} else if ("transparency".equals(property.getName())) {
						final Element propertyValue = (Element) property.getChildren().get(0);
						if ("float".equals(propertyValue.getName())) {
							transparency = Float.parseFloat(propertyValue.getText().replace(",", "."));
							// TODO: use this

							useTransparency = true;
						}
					} else if ("emission".equals(property.getName())) {
						final Element propertyValue = (Element) property.getChildren().get(0);
						if ("color".equals(propertyValue.getName())) {
							mState.setEmissive(colladaDOMUtil.getColor(propertyValue.getText()));
						}
					} else if ("specular".equals(property.getName())) {
						final Element propertyValue = (Element) property.getChildren().get(0);
						if ("color".equals(propertyValue.getName())) {
							mState.setSpecular(colladaDOMUtil.getColor(propertyValue.getText()));
						}
					} else if ("shininess".equals(property.getName())) {
						final Element propertyValue = (Element) property.getChildren().get(0);
						if ("float".equals(propertyValue.getName())) {
							float shininess = Float.parseFloat(propertyValue.getText().replace(",", "."));
							if (shininess >= 0.0f && shininess <= 1.0f) {
								final float oldShininess = shininess;
								shininess *= 128;
								ColladaMaterialUtils.logger.finest("Shininess - " + oldShininess
										+ " - was in the [0,1] range. Scaling to [0, 128] - " + shininess);
							} else if (shininess < 0 || shininess > 128) {
								final float oldShininess = shininess;
								shininess = (float) MathUtils.clamp(shininess, 0, 128);
								ColladaMaterialUtils.logger.warning("Shininess must be between 0 and 128. Shininess "
										+ oldShininess + " was clamped to " + shininess);
							}
							mState.setShininess(shininess);
						}
					}
				}

				// XXX: There are some issues with clarity on how to use alpha blending in OpenGL FFP.
				// The best interpretation I have seen is that if transparent has a texture == diffuse,
				// Turn on alpha blending and use diffuse alpha.

				if (diffuseTexture != null && useTransparency) {
					final BlendState blend = new BlendState();
					blend.setBlendEnabled(true);
					blend.setTestEnabled(true);
					blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
					blend.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
					mesh.setRenderState(blend);

					mesh.getSceneHints().setRenderBucketType(RenderBucketType.Transparent);
				}

				mesh.setRenderState(mState);
			}
		} else {
			ColladaMaterialUtils.logger.warning("material effect not found: " + mat.getChild("instance_material").getAttributeValue("url"));
		}
	}

	/**
	 * Convert a <texture> element to an Ardor3D representation and store in the given state.
	 *
	 * @param state		the Ardor3D TextureState to add of Texture to.
	 * @param daeTexture our <texture> element
	 * @param effect	  our <instance_effect> element
	 * @return the created Texture.
	 */
	private Texture populateTextureState(final TextureState state, final Element daeTexture, final Element effect) {
		// TODO: Use vert data to determine which texcoords and set to use.
		// final String uvName = daeTexture.getAttributeValue("texcoord");
		final int unit = 0;

		// Use texture attrib to find correct sampler
		final String textureReference = daeTexture.getAttributeValue("texture");
		Element node = colladaDOMUtil.findTargetWithSid(textureReference);
		if (node == null) {
			// Not sure if this is quite right, but spec seems to indicate looking for global id
			node = colladaDOMUtil.findTargetWithId("#" + textureReference);
		}

		if ("newparam".equals(node.getName())) {
			node = (Element) node.getChildren().get(0);
		}

		Element sampler = null;
		Element surface = null;
		Element image = null;

		Texture.MinificationFilter min = Texture.MinificationFilter.BilinearNoMipMaps;
		if ("sampler2D".equals(node.getName())) {
			sampler = node;
			if (sampler.getChild("minfilter") != null) {
				final String minfilter = sampler.getChild("minfilter").getText();
				min = Enum.valueOf(SamplerTypes.MinFilterType.class, minfilter).getArdor3dFilter();
			}
			// Use sampler to get correct surface
			node = colladaDOMUtil.findTargetWithSid(sampler.getChild("source").getText());
			// node = resolveSid(effect, sampler.getSource());
		}

		if ("newparam".equals(node.getName())) {
			node = (Element) node.getChildren().get(0);
		}

		if ("surface".equals(node.getName())) {
			surface = node;
			// image(s) will come from surface.
		} else if ("image".equals(node.getName())) {
			image = node;
		}

		// Ok, a few possibilities here...
		Texture texture = null;
		if (surface == null && image != null) {
			// Only an image found (no sampler). Assume 2d texture. Load.
			texture = loadTexture2D(image.getChild("init_from").getText(), min);
		} else if (surface != null) {
			// We have a surface, pull images from that.
			if ("2D".equals(surface.getAttributeValue("type"))) {
				// look for an init_from with lowest mip and use that. (usually 0)

				// TODO: mip?
				final Element lowest = (Element) surface.getChildren("init_from").get(0);
				// Element lowest = null;
				// for (final Element i : (List<Element>) surface.getChildren("init_from")) {
				// if (lowest == null || lowest.getMip() > i.getMip()) {
				// lowest = i;
				// }
				// }

				if (lowest == null) {
					ColladaMaterialUtils.logger.warning("surface given with no usable init_from: " + surface);
					return null;
				}

				image = colladaDOMUtil.findTargetWithId("#" + lowest.getText());
				// image = (DaeImage) root.resolveUrl("#" + lowest.getValue());
				if (image != null) {
					texture = loadTexture2D(image.getChild("init_from").getText(), min);
				}

				// TODO: add support for mip map levels other than 0.
			}
			// TODO: add support for the other texture types.
		} else {
			// No surface OR image... warn.
			ColladaMaterialUtils.logger.warning("texture given with no matching <sampler*> or <image> found.");
			return null;
		}

		if (texture != null) {
			if (sampler != null) {
				// Apply params from our sampler.
				applySampler(sampler, texture);
			}
			// Add to texture state.
			state.setTexture(texture, unit);
		} else {
			ColladaMaterialUtils.logger.warning("unable to load texture: " + daeTexture);
		}

		return texture;
	}

	private void applySampler(final Element sampler, final Texture texture) {
		if (sampler.getChild("minfilter") != null) {
			final String minfilter = sampler.getChild("minfilter").getText();
			texture.setMinificationFilter(Enum.valueOf(SamplerTypes.MinFilterType.class, minfilter).getArdor3dFilter());
		}
		if (sampler.getChild("magfilter") != null) {
			final String magfilter = sampler.getChild("magfilter").getText();
			texture
					.setMagnificationFilter(Enum.valueOf(SamplerTypes.MagFilterType.class, magfilter)
							.getArdor3dFilter());
		}
		if (sampler.getChild("wrap_s") != null) {
			final String wrapS = sampler.getChild("wrap_s").getText();
			texture.setWrap(Texture.WrapAxis.S, Enum.valueOf(SamplerTypes.WrapModeType.class, wrapS).getArdor3dWrapMode());
		}
		if (sampler.getChild("wrap_t") != null) {
			final String wrapT = sampler.getChild("wrap_t").getText();
			texture.setWrap(Texture.WrapAxis.T, Enum.valueOf(SamplerTypes.WrapModeType.class, wrapT).getArdor3dWrapMode());
		}
		if (sampler.getChild("border_color") != null) {
			texture.setBorderColor(colladaDOMUtil.getColor(sampler.getChild("border_color").getText()));
		}
	}

	@SuppressWarnings("unchecked")
	public void bindMaterials(final Element bindMaterial) {
		if (bindMaterial == null || bindMaterial.getChildren().isEmpty()) {
			return;
		}

		for (final Element instance : (List<Element>) bindMaterial.getChild("technique_common").getChildren("instance_material")) {
			final Element matNode = colladaDOMUtil.findTargetWithId(instance.getAttributeValue("target"));
			if (matNode != null && "material".equals(matNode.getName())) {
				dataCache.bindMaterial(instance.getAttributeValue("symbol"), matNode);
			} else {
				logger.warning("instance material target not found: " + instance.getAttributeValue("target"));
			}

			// TODO: need to store bound vert data as local data. (also unstore on unbind.)
		}
	}

	@SuppressWarnings("unchecked")
	public void unbindMaterials(final Element bindMaterial) {
		if (bindMaterial == null || bindMaterial.getChildren().isEmpty()) {
			return;
		}
		for (final Element instance : (List<Element>) bindMaterial.getChild("technique_common").getChildren("instance_material")) {
			dataCache.unbindMaterial(instance.getAttributeValue("symbol"));
		}
	}

	private Texture loadTexture2D(final String path, final Texture.MinificationFilter minFilter) {
		if (dataCache.containsTexture(path)) {
			return dataCache.getTexture(path);
		}

		final Texture texture;
		if (textureLocator == null) {
			texture = TextureManager.load(path, minFilter, Image.Format.Guess, true);
		} else {
			final ResourceSource source = textureLocator.locateResource(path);
			texture = TextureManager.load(source, minFilter, Image.Format.Guess, true);
		}
		dataCache.addTexture(path, texture);

		return texture;
	}

}
