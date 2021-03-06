/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image;

import java.io.IOException;

import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class TextureCubeMap extends Texture {

    private WrapMode _wrapS = WrapMode.Repeat;
    private WrapMode _wrapT = WrapMode.Repeat;
    private WrapMode _wrapR = WrapMode.Repeat;

    /**
     * Face of the Cubemap as described by its directional offset from the origin.
     */
    public enum Face {
        PositiveX, NegativeX, PositiveY, NegativeY, PositiveZ, NegativeZ;
    }

    @Override
    public Texture createSimpleClone() {
        return createSimpleClone(new TextureCubeMap());
    }

    @Override
    public Texture createSimpleClone(final Texture rVal) {
        rVal.setWrap(WrapAxis.S, _wrapS);
        rVal.setWrap(WrapAxis.T, _wrapT);
        rVal.setWrap(WrapAxis.R, _wrapR);
        return super.createSimpleClone(rVal);
    }

    /**
     * <code>setWrap</code> sets the wrap mode of this texture for a particular axis.
     * 
     * @param axis
     *            the texture axis to define a wrapmode on.
     * @param mode
     *            the wrap mode for the given axis of the texture.
     * @throws IllegalArgumentException
     *             if axis or mode are null
     */
    @Override
    public void setWrap(final WrapAxis axis, final WrapMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode can not be null.");
        } else if (axis == null) {
            throw new IllegalArgumentException("axis can not be null.");
        }
        switch (axis) {
            case S:
                _wrapS = mode;
                break;
            case T:
                _wrapT = mode;
                break;
            case R:
                _wrapR = mode;
                break;
        }
    }

    /**
     * <code>setWrap</code> sets the wrap mode of this texture for all axis.
     * 
     * @param mode
     *            the wrap mode for the given axis of the texture.
     * @throws IllegalArgumentException
     *             if mode is null
     */
    @Override
    public void setWrap(final WrapMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode can not be null.");
        }
        _wrapS = mode;
        _wrapT = mode;
        _wrapR = mode;
    }

    /**
     * <code>getWrap</code> returns the wrap mode for a given coordinate axis on this texture.
     * 
     * @param axis
     *            the axis to return for
     * @return the wrap mode of the texture.
     * @throws IllegalArgumentException
     *             if axis is null
     */
    @Override
    public WrapMode getWrap(final WrapAxis axis) {
        switch (axis) {
            case S:
                return _wrapS;
            case T:
                return _wrapT;
            case R:
                return _wrapR;
        }
        throw new IllegalArgumentException("invalid WrapAxis: " + axis);
    }

    @Override
    public Type getType() {
        return Type.CubeMap;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof TextureCubeMap)) {
            return false;
        }
        final TextureCubeMap that = (TextureCubeMap) other;
        if (getWrap(WrapAxis.S) != that.getWrap(WrapAxis.S)) {
            return false;
        }
        if (getWrap(WrapAxis.T) != that.getWrap(WrapAxis.T)) {
            return false;
        }
        if (getWrap(WrapAxis.R) != that.getWrap(WrapAxis.R)) {
            return false;
        }
        return super.equals(other);
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(_wrapS, "wrapS", WrapMode.EdgeClamp);
        capsule.write(_wrapT, "wrapT", WrapMode.EdgeClamp);
        capsule.write(_wrapR, "wrapR", WrapMode.EdgeClamp);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        _wrapS = capsule.readEnum("wrapS", WrapMode.class, WrapMode.EdgeClamp);
        _wrapT = capsule.readEnum("wrapT", WrapMode.class, WrapMode.EdgeClamp);
        _wrapR = capsule.readEnum("wrapR", WrapMode.class, WrapMode.EdgeClamp);
    }
}
