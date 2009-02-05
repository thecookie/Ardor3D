/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.effect.particle;

import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.scenegraph.Point;
import com.ardor3d.scenegraph.TexCoords;
import com.ardor3d.util.geom.BufferUtils;

/**
 * ParticlePoints is a particle system that uses Point as its underlying geometric data.
 */
public class ParticlePoints extends ParticleSystem {

    private static final long serialVersionUID = 2L;

    public ParticlePoints() {}

    public ParticlePoints(final String name, final int numParticles) {
        super(name, numParticles);
    }

    @Override
    protected void initializeParticles(final int numParticles) {
        Vector2 sharedTextureData[];

        // setup texture coords
        sharedTextureData = new Vector2[] { new Vector2(0.0, 0.0) };

        final int verts = getVertsForParticleType(getParticleType());

        _geometryCoordinates = BufferUtils.createVector3Buffer(numParticles * verts);

        _appearanceColors = BufferUtils.createColorBuffer(numParticles * verts);
        _particles = new Particle[numParticles];

        if (_particleGeom != null) {
            detachChild(_particleGeom);
        }
        _particleGeom = new Point(getName() + "_points", _geometryCoordinates, null, _appearanceColors, null) {
            private static final long serialVersionUID = 1L;

            @Override
            public void updateWorldTransform(final boolean recurse) {
                ; // Do nothing.
            }
        };
        _particleGeom.getMeshData().setTextureCoords(new TexCoords(BufferUtils.createVector2Buffer(numParticles)), 0);
        attachChild(_particleGeom);
        setRenderBucketType(RenderBucketType.Opaque);
        setLightCombineMode(LightCombineMode.Off);
        setTextureCombineMode(TextureCombineMode.Replace);

        for (int k = 0; k < numParticles; k++) {
            _particles[k] = new Particle(this);
            _particles[k].init();
            _particles[k].setStartIndex(k * verts);
            for (int a = verts - 1; a >= 0; a--) {
                final int ind = (k * verts) + a;
                BufferUtils.setInBuffer(sharedTextureData[a],
                        getParticleGeometry().getMeshData().getTextureCoords(0)._coords, ind);
                BufferUtils.setInBuffer(_particles[k].getCurrentColor(), _appearanceColors, (ind));
            }

        }
        updateWorldRenderStates(true);
        _particleGeom.setCastsShadows(false);
    }

    @Override
    public ParticleType getParticleType() {
        return ParticleSystem.ParticleType.Point;
    }

    @Override
    public void draw(final Renderer r) {
        final Camera camera = ContextManager.getCurrentContext().getCurrentCamera();
        for (int i = 0; i < _particles.length; i++) {
            final Particle particle = _particles[i];
            if (particle.getStatus() == Particle.Status.Alive) {
                particle.updateVerts(camera);
            }
        }

        if (!_particlesInWorldCoords) {
            getParticleGeometry().setWorldTranslation(getWorldTranslation());
            getParticleGeometry().setWorldRotation(getWorldRotation());
        } else {
            getParticleGeometry().setWorldTranslation(Vector3.ZERO);
            getParticleGeometry().setWorldRotation(Matrix3.IDENTITY);
        }
        getParticleGeometry().setWorldScale(getWorldScale());

        getParticleGeometry().draw(r);
    }

    /**
     * @return true if points are to be drawn antialiased
     */
    public boolean isAntialiased() {
        return getParticleGeometry().isAntialiased();
    }

    /**
     * Sets whether the points should be antialiased. May decrease performance. If you want to enabled antialiasing, you
     * should also use an alphastate with a source of SourceFunction.SourceAlpha and a destination of
     * DB_ONE_MINUS_SRC_ALPHA or DB_ONE.
     * 
     * @param antialiased
     *            true if the line should be antialiased.
     */
    public void setAntialiased(final boolean antialiased) {
        getParticleGeometry().setAntialiased(antialiased);
    }

    /**
     * @return the pixel size of each point.
     */
    public float getPointSize() {
        return getParticleGeometry().getPointSize();
    }

    /**
     * Sets the pixel width of the points when drawn. Non anti-aliased point sizes are rounded to the nearest whole
     * number by opengl.
     * 
     * @param size
     *            The size to set.
     */
    public void setPointSize(final float size) {
        getParticleGeometry().setPointSize(size);
    }

    @Override
    public Point getParticleGeometry() {
        return (Point) _particleGeom;
    }

}
