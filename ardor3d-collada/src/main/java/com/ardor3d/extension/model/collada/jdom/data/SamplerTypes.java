package com.ardor3d.extension.model.collada.jdom.data;

import com.ardor3d.image.Texture;

public class SamplerTypes {
	public enum WrapModeType {
		WRAP(Texture.WrapMode.Repeat),
		MIRROR(Texture.WrapMode.MirroredRepeat),
		CLAMP(Texture.WrapMode.EdgeClamp),
		BORDER(Texture.WrapMode.BorderClamp),
		NONE(Texture.WrapMode.BorderClamp);

		final Texture.WrapMode wm;

		private WrapModeType(final Texture.WrapMode ardorWrapMode) {
			wm = ardorWrapMode;
		}

		public Texture.WrapMode getArdor3dWrapMode() {
			return wm;
		}
	}

	/**
	 * Enum matching Collada's texture minification modes to Ardor3D's.
	 */
	public enum MinFilterType {
		NONE(Texture.MinificationFilter.NearestNeighborNoMipMaps),
		NEAREST(Texture.MinificationFilter.NearestNeighborNoMipMaps),
		LINEAR(Texture.MinificationFilter.BilinearNoMipMaps),
		NEAREST_MIPMAP_NEAREST(Texture.MinificationFilter.NearestNeighborNearestMipMap),
		LINEAR_MIPMAP_NEAREST(Texture.MinificationFilter.BilinearNearestMipMap),
		NEAREST_MIPMAP_LINEAR(Texture.MinificationFilter.NearestNeighborLinearMipMap),
		LINEAR_MIPMAP_LINEAR(Texture.MinificationFilter.Trilinear);

		final Texture.MinificationFilter mf;

		private MinFilterType(final Texture.MinificationFilter ardorFilter) {
			mf = ardorFilter;
		}

		public Texture.MinificationFilter getArdor3dFilter() {
			return mf;
		}
	}

	/**
	 * Enum matching Collada's texture magnification modes to Ardor3D's.
	 */
	public enum MagFilterType {
		NONE(Texture.MagnificationFilter.NearestNeighbor),
		NEAREST(Texture.MagnificationFilter.NearestNeighbor),
		LINEAR(Texture.MagnificationFilter.Bilinear),
		NEAREST_MIPMAP_NEAREST(Texture.MagnificationFilter.NearestNeighbor),
		LINEAR_MIPMAP_NEAREST(Texture.MagnificationFilter.Bilinear),
		NEAREST_MIPMAP_LINEAR(Texture.MagnificationFilter.NearestNeighbor),
		LINEAR_MIPMAP_LINEAR(Texture.MagnificationFilter.Bilinear);

		final Texture.MagnificationFilter mf;

		private MagFilterType(final Texture.MagnificationFilter ardorFilter) {
			mf = ardorFilter;
		}

		public Texture.MagnificationFilter getArdor3dFilter() {
			return mf;
		}
	}
}
