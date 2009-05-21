/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.renderer;

import java.util.Random;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.input.InputState;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.DataMode;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.google.inject.Inject;

public class VBOSpeedExample extends ExampleBase {

    private boolean useVBO = false;

    public static void main(final String[] args) {
        start(VBOSpeedExample.class);
    }

    @Inject
    public VBOSpeedExample(final LogicalLayer layer, final FrameHandler frameWork) {
        super(layer, frameWork);
    }

    double counter = 0;
    int frames = 0;

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        counter += timer.getTimePerFrame();
        frames++;
        if (counter > 1) {
            final double fps = (frames / counter);
            counter = 0;
            frames = 0;
            System.out.printf("%7.1f FPS\n", fps);
        }
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("VBOSpeedExample");

        final BasicText t = BasicText.createDefaultTextLabel("Text", "[SPACE] VBO off");
        t.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        t.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        t.setTranslation(new Vector3(0, 20, 0));
        _root.attachChild(t);

        final CullState cs = new CullState();
        cs.setCullFace(CullState.Face.Back);
        cs.setEnabled(true);
        _root.setRenderState(cs);

        final MaterialState ms = new MaterialState();
        ms.setColorMaterial(ColorMaterial.Diffuse);
        _root.setRenderState(ms);

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                Format.Guess, true));

        final Random rand = new Random(1337);
        for (int i = 0; i < 100; i++) {
            final Sphere sphere = new Sphere("Sphere", 32, 32, 2);
            sphere.setRandomColors();
            sphere.setModelBound(new BoundingBox());
            sphere.setRenderState(ts);
            sphere.setTranslation(new Vector3(rand.nextDouble() * 100.0 - 50.0, rand.nextDouble() * 100.0 - 50.0, rand
                    .nextDouble() * 100.0 - 250.0));

            _root.attachChild(sphere);
        }

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
            public void perform(final Canvas source, final InputState inputState, final double tpf) {
                useVBO = !useVBO;
                if (useVBO) {
                    t.setText("[SPACE] VBO on");
                    for (final Spatial spatial : _root.getChildren()) {
                        if (spatial instanceof Mesh) {
                            spatial.getSceneHints().setDataMode(DataMode.VBO);
                            // spatial.getSceneHints().setDataMode(DataMode.VBOInterleaved);
                        }
                    }
                } else {
                    t.setText("[SPACE] VBO off");
                    for (final Spatial spatial : _root.getChildren()) {
                        if (spatial instanceof Mesh) {
                            spatial.getSceneHints().setDataMode(DataMode.Arrays);
                        }
                    }
                }
            }
        }));

    }
}
