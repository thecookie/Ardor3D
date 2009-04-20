/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.jogl;

import java.nio.IntBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.glu.GLU;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.renderer.AbstractPbufferTextureRenderer;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRenderer;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.record.TextureRecord;
import com.ardor3d.renderer.state.record.TextureStateRecord;
import com.ardor3d.scene.state.jogl.JoglTextureStateUtil;
import com.ardor3d.scene.state.jogl.util.JoglTextureUtil;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;

public class JoglPbufferTextureRenderer extends AbstractPbufferTextureRenderer {
    private static final Logger logger = Logger.getLogger(JoglPbufferTextureRenderer.class.getName());

    /* Pbuffer instance */
    private GLPbuffer _pbuffer;

    private GLContext _context;

    // HACK: needed to get the parent context in here somehow...
    public static GLContext _parentContext;

    public JoglPbufferTextureRenderer(final DisplaySettings settings, final TextureRenderer.Target target,
            final Renderer parentRenderer) {
        super(settings, target, parentRenderer);
        setMultipleTargets(true);
    }

    /**
     * <code>setupTexture</code> initializes a new Texture object for use with TextureRenderer. Generates a valid gl
     * texture id for this texture and inits the data type for the texture.
     */
    public void setupTexture(final Texture2D tex) {
        final GL gl = GLU.getCurrentGL();

        final IntBuffer ibuf = BufferUtils.createIntBuffer(1);

        if (tex.getTextureId() != 0) {
            ibuf.put(tex.getTextureId());
            gl.glDeleteTextures(1, ibuf);
            ibuf.clear();
        }

        // Create the texture
        gl.glGenTextures(1, ibuf);
        tex.setTextureId(ibuf.get(0));
        TextureManager.registerForCleanup(tex.getTextureKey(), tex.getTextureId());

        JoglTextureStateUtil.doTextureBind(tex.getTextureId(), 0, Texture.Type.TwoDimensional);
        final int internalFormat = JoglTextureUtil.getGLInternalFormat(tex.getRenderToTextureFormat());
        final int pixFormat = JoglTextureUtil.getGLPixelFormat(tex.getRenderToTextureFormat());
        final int pixDataType = JoglTextureUtil.getGLPixelDataType(tex.getRenderToTextureFormat());

        // Initialize our texture with some default data.
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, _width, _height, 0, pixFormat, pixDataType, null);

        // Setup filtering and wrap
        final RenderContext context = ContextManager.getCurrentContext();
        final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(RenderState.StateType.Texture);
        final TextureRecord texRecord = record.getTextureRecord(tex.getTextureId(), tex.getType());

        JoglTextureStateUtil.applyFilter(tex, texRecord, 0, record, context.getCapabilities());
        JoglTextureStateUtil.applyWrap(tex, texRecord, 0, record, context.getCapabilities());

        logger.fine("setup pbuffer tex" + tex.getTextureId() + ": " + _width + "," + _height);
    }

    public void render(final Spatial spat, final Texture tex, final boolean doClear) {
        render(null, spat, tex, doClear);
    }

    public void render(final List<? extends Spatial> spat, final Texture tex, final boolean doClear) {
        render(spat, null, tex, doClear);
    }

    private void render(final List<? extends Spatial> toDrawA, final Spatial toDrawB, final Texture tex,
            final boolean doClear) {
        // clear the current states since we are rendering into a new location
        // and can not rely on states still being set.
        try {
            if (_pbuffer == null) {
                initPbuffer();
            }

            if (_useDirectRender && !tex.getRenderToTextureFormat().isDepthFormat()) {
                // setup and render directly to a 2d texture.
                _pbuffer.releaseTexture();
                activate();
                switchCameraIn(doClear);

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else {
                    doDraw(toDrawB);
                }

                deactivate();
                switchCameraOut();
                JoglTextureStateUtil.doTextureBind(tex.getTextureId(), 0, Texture.Type.TwoDimensional);
                _pbuffer.bindTexture();
            } else {
                // render and copy to a texture
                activate();
                switchCameraIn(doClear);

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else {
                    doDraw(toDrawB);
                }

                switchCameraOut();

                copyToTexture(tex, _width, _height);

                deactivate();
            }

        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "render(Spatial, Texture)", "Exception", e);
        }
    }

    public void render(final Spatial spat, final List<Texture> texs, final boolean doClear) {
        render(null, spat, texs, doClear);
    }

    public void render(final List<? extends Spatial> spat, final List<Texture> texs, final boolean doClear) {
        render(spat, null, texs, doClear);
    }

    private void render(final List<? extends Spatial> toDrawA, final Spatial toDrawB, final List<Texture> texs,
            final boolean doClear) {
        // clear the current states since we are rendering into a new location
        // and can not rely on states still being set.
        try {
            if (_pbuffer == null) {
                initPbuffer();
            }

            if (texs.size() == 1 && _useDirectRender && !texs.get(0).getRenderToTextureFormat().isDepthFormat()) {
                // setup and render directly to a 2d texture.
                JoglTextureStateUtil.doTextureBind(texs.get(0).getTextureId(), 0, Texture.Type.TwoDimensional);
                activate();
                switchCameraIn(doClear);
                _pbuffer.releaseTexture();

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else {
                    doDraw(toDrawB);
                }

                switchCameraOut();

                deactivate();
                _pbuffer.bindTexture();
            } else {
                // render and copy to a texture
                activate();
                switchCameraIn(doClear);

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else {
                    doDraw(toDrawB);
                }

                switchCameraOut();

                for (int i = 0; i < texs.size(); i++) {
                    copyToTexture(texs.get(i), _width, _height);
                }

                deactivate();
            }

        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "render(Spatial, Texture)", "Exception", e);
        }
    }

    public void copyToTexture(final Texture tex, final int width, final int height) {
        JoglTextureStateUtil.doTextureBind(tex.getTextureId(), 0, Texture.Type.TwoDimensional);

        final GL gl = GLU.getCurrentGL();

        gl.glCopyTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
    }

    @Override
    protected void clearBuffers() {
        final GL gl = GLU.getCurrentGL();

        gl.glDisable(GL.GL_SCISSOR_TEST);
        _parentRenderer.clearBuffers();
    }

    private void initPbuffer() {

        try {
            if (_pbuffer != null) {
                _context.destroy();
                _pbuffer.destroy();
                giveBackContext();
                ContextManager.removeContext(_pbuffer);
            }

            // Make our GLPbuffer...
            final GLDrawableFactory fac = GLDrawableFactory.getFactory();
            final GLCapabilities caps = new GLCapabilities();
            caps.setDoubleBuffered(false);
            _pbuffer = fac.createGLPbuffer(caps, null, _width, _height, _parentContext);
            _context = _pbuffer.getContext();

            _context.makeCurrent();

            final JoglContextCapabilities contextCaps = new JoglContextCapabilities(_pbuffer.getGL());
            ContextManager.addContext(_context, new RenderContext(_context, contextCaps, ContextManager
                    .getCurrentContext()));

        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "initPbuffer()", "Exception", e);

            if (_useDirectRender) {
                logger
                        .warning("Your card claims to support Render to Texture but fails to enact it.  Updating your driver might solve this problem.");
                logger.warning("Attempting to fall back to Copy Texture.");
                _useDirectRender = false;
                initPbuffer();
                return;
            }

            logger.log(Level.WARNING, "Failed to create Pbuffer.", e);
            return;
        }

        try {
            activate();

            _width = _pbuffer.getWidth();
            _height = _pbuffer.getHeight();

            deactivate();
        } catch (final Exception e) {
            logger.log(Level.WARNING, "Failed to initialize created Pbuffer.", e);
            return;
        }
    }

    private void activate() {
        if (_active == 0) {
            _oldContext = ContextManager.getCurrentContext();
            _context.makeCurrent();

            ContextManager.switchContext(_context);

            ContextManager.getCurrentContext().clearEnforcedStates();
            ContextManager.getCurrentContext().enforceStates(_enforcedStates);

            if (_bgColorDirty) {
                final GL gl = GLU.getCurrentGL();

                gl.glClearColor(_backgroundColor.getRed(), _backgroundColor.getGreen(), _backgroundColor.getBlue(),
                        _backgroundColor.getAlpha());
                _bgColorDirty = false;
            }
        }
        _active++;
    }

    private void deactivate() {
        if (_active == 1) {
            giveBackContext();
        }
        _active--;
    }

    private void giveBackContext() {
        _parentContext.makeCurrent();
        ContextManager.switchContext(_oldContext.getContextHolder());
    }

    public void cleanup() {
        ContextManager.removeContext(_pbuffer);
        _pbuffer.destroy();
    }

    public void setMultipleTargets(final boolean force) {
        if (force) {
            logger.fine("Copy Texture Pbuffer used!");
            _useDirectRender = false;
            if (_pbuffer != null) {
                giveBackContext();
                ContextManager.removeContext(_pbuffer);
            }
        } else {
            // XXX: Is this WGL specific query right?
            if (GLU.getCurrentGL().isExtensionAvailable("WGL_ARB_render_texture")) {
                logger.fine("Render to Texture Pbuffer supported!");
                _useDirectRender = true;
            } else {
                logger.fine("Copy Texture Pbuffer supported!");
            }
        }
    }
}
