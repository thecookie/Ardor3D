/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.lwjgl;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ARBMultisample;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.Scene;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.Camera.ProjectionMode;
import com.ardor3d.renderer.lwjgl.LwjglContextCapabilities;
import com.ardor3d.renderer.lwjgl.LwjglRenderer;
import com.google.inject.Inject;

public class LwjglCanvasRenderer implements CanvasRenderer {
    protected Scene _scene;
    protected Camera _camera;
    protected boolean _doSwap;
    protected LwjglRenderer _renderer;
    protected Object _context = new Object();
    private RenderContext _currentContext;

    // NOTE: This code commented out by Petter 090224, since it isn't really ready to be used,
    // and since it is at the moment more work than it is worth to get it ready. Later on, when
    // we have solved some more fundamental problems, it is probably time to revisit this.

    // ensure availability of LWJGL natives
    // {
    // final String[] libraryPaths = LwjglLibraryPaths.getLibraryPaths(System.getProperty("os.name"), System
    // .getProperty("os.arch"));
    //
    // try {
    // NativeLoader.makeLibrariesAvailable(libraryPaths);
    // } catch (final Exception e) {
    // ; // ignore
    // }
    // }

    @Inject
    public LwjglCanvasRenderer(final Scene scene) {
        _scene = scene;
    }

    @MainThread
    public void init(final DisplaySettings settings, final boolean doSwap) {
        _doSwap = doSwap;

        // Look up a shared context, if a shared LwjglCanvasRenderer is given.
        // XXX: Shared contexts will probably not work... lwjgl does not seem to have a way to make a new glcontext that
        // shares lists, textures, etc.
        RenderContext sharedContext = null;
        if (settings.getShareContext() != null) {
            sharedContext = ContextManager.getContextForKey(settings.getShareContext().getRenderContext()
                    .getContextKey());
        }

        setCurrentContext();

        final LwjglContextCapabilities caps = new LwjglContextCapabilities(GLContext.getCapabilities());
        _currentContext = new RenderContext(this, caps, sharedContext);

        ContextManager.addContext(this, _currentContext);

        _renderer = new LwjglRenderer();

        if (settings.getSamples() != 0 && caps.isMultisampleSupported()) {
            GL11.glEnable(ARBMultisample.GL_MULTISAMPLE_ARB);
        }

        _renderer.setBackgroundColor(ColorRGBA.BLACK);

        if (_camera == null) {
            /** Set up how our camera sees. */
            _camera = new Camera(settings.getWidth(), settings.getHeight());
            _camera.setFrustumPerspective(45.0f, (float) settings.getWidth() / (float) settings.getHeight(), 1, 1000);
            _camera.setProjectionMode(ProjectionMode.Perspective);

            final Vector3 loc = new Vector3(0.0f, 0.0f, 10.0f);
            final Vector3 left = new Vector3(-1.0f, 0.0f, 0.0f);
            final Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);
            final Vector3 dir = new Vector3(0.0f, 0f, -1.0f);
            /** Move our camera to a correct place and orientation. */
            _camera.setFrame(loc, left, up, dir);
        } else {
            // use new width and height to set ratio.
            _camera.setFrustumPerspective(_camera.getFovY(),
                    (float) settings.getWidth() / (float) settings.getHeight(), _camera.getFrustumNear(), _camera
                            .getFrustumFar());
        }
    }

    @MainThread
    public boolean draw() {

        // set up context for rendering this canvas
        ContextManager.switchContext(this);
        setCurrentContext();

        // render stuff, first apply our camera if we have one
        if (_camera != null) {
            if (Camera.getCurrentCamera() != _camera) {
                _camera.update();
            }
            _camera.apply(_renderer);
        }
        _renderer.clearBuffers(Renderer.BUFFER_COLOR_AND_DEPTH);
        final boolean drew = _scene.renderUnto(_renderer);
        _renderer.flushFrame(drew && _doSwap);
        return drew;
    }

    public Camera getCamera() {
        return _camera;
    }

    public Scene getScene() {
        return _scene;
    }

    public void setScene(final Scene scene) {
        _scene = scene;
    }

    public Renderer getRenderer() {
        return _renderer;
    }

    public void setCurrentContext() {
        try {
            GLContext.useContext(_context);
        } catch (final LWJGLException e) {
            throw new RuntimeException(e);
        }
    }

    public void releaseCurrentContext() {
        try {
            GLContext.useContext(null);
        } catch (final LWJGLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setCamera(final Camera camera) {
        _camera = camera;
    }

    public RenderContext getRenderContext() {
        return _currentContext;
    }
}
