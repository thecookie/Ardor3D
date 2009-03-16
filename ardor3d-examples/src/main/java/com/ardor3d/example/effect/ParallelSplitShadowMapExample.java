/** * Copyright (c) 2008-2009 Ardor Labs, Inc. * * This file is part of Ardor3D. * * Ardor3D is free software: you can redistribute it and/or modify it  * under the terms of its license which may be found in the accompanying * LICENSE file or at <http://www.ardor3d.com/LICENSE>. */package com.ardor3d.example.effect;import com.ardor3d.bounding.BoundingBox;import com.ardor3d.example.ExampleBase;import com.ardor3d.extension.shadow.map.ParallelSplitShadowMapPass;import com.ardor3d.framework.Canvas;import com.ardor3d.framework.FrameHandler;import com.ardor3d.image.Texture;import com.ardor3d.image.Texture2D;import com.ardor3d.image.Image.Format;import com.ardor3d.input.InputState;import com.ardor3d.input.Key;import com.ardor3d.input.logical.InputTrigger;import com.ardor3d.input.logical.KeyPressedCondition;import com.ardor3d.input.logical.LogicalLayer;import com.ardor3d.input.logical.TriggerAction;import com.ardor3d.light.PointLight;import com.ardor3d.math.Matrix3;import com.ardor3d.math.Vector3;import com.ardor3d.renderer.Renderer;import com.ardor3d.renderer.pass.BasicPassManager;import com.ardor3d.renderer.pass.RenderPass;import com.ardor3d.renderer.queue.RenderBucketType;import com.ardor3d.renderer.state.CullState;import com.ardor3d.renderer.state.MaterialState;import com.ardor3d.renderer.state.TextureState;import com.ardor3d.renderer.state.MaterialState.ColorMaterial;import com.ardor3d.scenegraph.Controller;import com.ardor3d.scenegraph.Node;import com.ardor3d.scenegraph.Spatial;import com.ardor3d.scenegraph.VBOInfo;import com.ardor3d.scenegraph.Spatial.CullHint;import com.ardor3d.scenegraph.Spatial.LightCombineMode;import com.ardor3d.scenegraph.Spatial.TextureCombineMode;import com.ardor3d.scenegraph.shape.Box;import com.ardor3d.scenegraph.shape.Quad;import com.ardor3d.scenegraph.shape.Torus;import com.ardor3d.scenegraph.visitor.UpdateModelBoundVisitor;import com.ardor3d.ui.text.BasicText;import com.ardor3d.util.ReadOnlyTimer;import com.ardor3d.util.TextureManager;import com.google.inject.Inject;/** * Example showing the parallel split shadow mapping technique. */public class ParallelSplitShadowMapExample extends ExampleBase {    /** Pssm shadow map pass. */    private ParallelSplitShadowMapPass _pssmPass;    /** Current number splits. */    private int _numberSplits = 3;    /** Current texture size. */    private int _shadowMapSize = 1024;    /** Pass manager. */    private BasicPassManager _passManager;    /** Quads used for debug showing shadowmaps. */    private Quad _orthoQuad[];    /** Flag for turning on/off light movement. */    private boolean _updateLight = false;    /** Temp vec for updating light pos. */    private final Vector3 lightPosition = new Vector3(10000, 5000, 10000);    /** Text fields used to present info about the example. */    private final BasicText _exampleInfo[] = new BasicText[9];    /** Flag to make sure quads are updated on reinitialization of shadow renderer */    private boolean _quadsDirty = true;    /**     * The main method.     *      * @param args     *            the arguments     */    public static void main(final String[] args) {        start(ParallelSplitShadowMapExample.class);    }    /**     * Instantiates a new parallel split shadow map example.     *      * @param layer     *            the layer     * @param frameWork     *            the frame work     * @param timer     *            the timer     */    @Inject    public ParallelSplitShadowMapExample(final LogicalLayer layer, final FrameHandler frameWork) {        super(layer, frameWork);    }    /**     * Update the passmanager.     *      * @param tpf     *            the tpf     */    @Override    protected void updateExample(final ReadOnlyTimer timer) {        _passManager.updatePasses(timer.getTimePerFrame());        if (_updateLight) {            final double time = timer.getTimeInSeconds() * 0.2;            lightPosition.set(Math.sin(time) * 10000.0, 5000.0, Math.cos(time) * 10000.0);        }    }    /**     * Initialize pssm if needed. Update light position. Render scene.     *      * @param renderer     *            the renderer     */    @Override    protected void renderExample(final Renderer renderer) {        if (!_pssmPass.isInitialised()) {            _pssmPass.init(renderer);        }        updateQuadTextures(renderer);        // Update the shadowpass "light" position. Iow it's camera.        _pssmPass.getCamera().setLocation(lightPosition);        ((PointLight) _lightState.get(0)).setLocation(lightPosition);        _passManager.renderPasses(renderer);    }    /**     * Initialize pssm pass and scene.     */    @Override    protected void initExample() {        // Setup main camera.        _canvas.setTitle("Parallel Split Shadow Maps - Example");        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(250, 200, -250));        _canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(0, 0, 0), Vector3.UNIT_Y);        _canvas.getCanvasRenderer().getCamera().setFrustumFar(10000);        _canvas.getCanvasRenderer().getCamera().setFrustumNear(1);        _controlHandle.setMoveSpeed(200);        // Setup some standard states for the scene.        final CullState cullFrontFace = new CullState();        cullFrontFace.setEnabled(true);        cullFrontFace.setCullFace(CullState.Face.Back);        _root.setRenderState(cullFrontFace);        final TextureState ts = new TextureState();        ts.setEnabled(true);        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,                Format.GuessNoCompression, true));        _root.setRenderState(ts);        final MaterialState ms = new MaterialState();        ms.setColorMaterial(ColorMaterial.Diffuse);        _root.setRenderState(ms);        _passManager = new BasicPassManager();        // setup some quads for debug viewing.        final RenderPass renderPass = new RenderPass();        final int quadSize = _canvas.getCanvasRenderer().getCamera().getWidth() / 10;        _orthoQuad = new Quad[_numberSplits];        for (int i = 0; i < _numberSplits; i++) {            _orthoQuad[i] = new Quad("OrthoQuad", quadSize, quadSize);            _orthoQuad[i].setTranslation(new Vector3((quadSize / 2 + 5) + (quadSize + 5) * i, (quadSize / 2 + 5), 1));            _orthoQuad[i].setRenderBucketType(RenderBucketType.Ortho);            _orthoQuad[i].setLightCombineMode(LightCombineMode.Off);            _orthoQuad[i].setTextureCombineMode(TextureCombineMode.Replace);            _orthoQuad[i].setCullHint(CullHint.Never);            renderPass.add(_orthoQuad[i]);        }        // Create scene objects.        setupTerrain();        final Node occluders = setupOccluders();        final RenderPass rootPass = new RenderPass();        rootPass.add(_root);        // Create pssm pass        _pssmPass = new ParallelSplitShadowMapPass(_shadowMapSize, _numberSplits);        _pssmPass.add(_root);        _pssmPass.addOccluder(occluders);        // Populate passmanager with passes.        _passManager.add(rootPass);        _passManager.add(_pssmPass);        _passManager.add(renderPass);        // Setyp textfields for presenting example info.        final Node textNodes = new Node("Text");        renderPass.add(textNodes);        textNodes.setRenderBucketType(RenderBucketType.Ortho);        textNodes.setLightCombineMode(LightCombineMode.Off);        final double infoStartY = _canvas.getCanvasRenderer().getCamera().getHeight() / 2;        for (int i = 0; i < _exampleInfo.length; i++) {            _exampleInfo[i] = BasicText.createDefaultTextLabel("Text", "", 16);            _exampleInfo[i].setTranslation(new Vector3(10, infoStartY - i * 20, 0));            textNodes.attachChild(_exampleInfo[i]);        }        textNodes.updateGeometricState(0.0);        updateText();        // Register keyboard triggers for manipulating example        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ZERO), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                _pssmPass.setDrawShaderDebug(!_pssmPass.isDrawShaderDebug());                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                _updateLight = !_updateLight;                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                _pssmPass.setUpdateMainCamera(!_pssmPass.isUpdateMainCamera());                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.THREE), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                _pssmPass.setDrawDebug(!_pssmPass.isDrawDebug());                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FOUR), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                if (_numberSplits > 1) {                    _numberSplits--;                    _pssmPass.setNumOfSplits(_numberSplits);                    updateText();                    _quadsDirty = true;                }            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.FIVE), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                if (_numberSplits < 3) {                    _numberSplits++;                    _pssmPass.setNumOfSplits(_numberSplits);                    updateText();                    _quadsDirty = true;                }            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SIX), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                if (_shadowMapSize > 1) {                    _shadowMapSize /= 2;                    _pssmPass.setShadowMapSize(_shadowMapSize);                    updateText();                    _quadsDirty = true;                }            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SEVEN), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                if (_shadowMapSize < 2048) {                    _shadowMapSize *= 2;                    _pssmPass.setShadowMapSize(_shadowMapSize);                    updateText();                    _quadsDirty = true;                }            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.EIGHT), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                final double maxShadowDistance = _pssmPass.getMaxShadowDistance();                if (maxShadowDistance > 200.0) {                    _pssmPass.setMaxShadowDistance(maxShadowDistance - 100.0);                    updateText();                }            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.NINE), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                final double maxShadowDistance = _pssmPass.getMaxShadowDistance();                _pssmPass.setMaxShadowDistance(maxShadowDistance + 100.0);                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.U), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                _numberSplits = 1;                _shadowMapSize = 1024;                _pssmPass.setNumOfSplits(_numberSplits);                _pssmPass.setShadowMapSize(_shadowMapSize);                updateText();                _quadsDirty = true;            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.I), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                _numberSplits = 3;                _shadowMapSize = 512;                _pssmPass.setNumOfSplits(_numberSplits);                _pssmPass.setShadowMapSize(_shadowMapSize);                updateText();                _quadsDirty = true;            }        }));        // Make sure all boundings are updated.        _root.acceptVisitor(new UpdateModelBoundVisitor(), false);    }    /**     * Setup debug quads to render pssm shadowmaps.     */    private void updateQuadTextures(final Renderer r) {        if (!_quadsDirty) {            return;        }        _quadsDirty = false;        _pssmPass.reinit(r);        for (int i = 0; i < _numberSplits; i++) {            final TextureState screen = new TextureState();            final Texture2D copy = new Texture2D();            copy.setTextureId(_pssmPass.getShadowMapTexture(i).getTextureId());            screen.setTexture(copy);            _orthoQuad[i].setRenderState(screen);            _orthoQuad[i].updateGeometricState(0.0);        }    }    /**     * Update text information.     */    private void updateText() {        _exampleInfo[0].setText("[0] Debug shader draw: " + _pssmPass.isDrawShaderDebug());        _exampleInfo[1].setText("[1] Update light: " + _updateLight);        _exampleInfo[2].setText("[2] Update main camera: " + _pssmPass.isUpdateMainCamera());        _exampleInfo[3].setText("[3] Debug draw: " + _pssmPass.isDrawDebug());        _exampleInfo[4].setText("[4/5] Number of splits: " + _numberSplits);        _exampleInfo[5].setText("[6/7] Shadow map size: " + _shadowMapSize);        _exampleInfo[6].setText("[8/9] Max shadow distance: " + _pssmPass.getMaxShadowDistance());        _exampleInfo[7].setText("[U] Setup 1 split of size 1024");        _exampleInfo[8].setText("[I] Setup 3 splits of size 512");    }    /**     * Setup terrain.     */    private void setupTerrain() {        final Box box = new Box("box", new Vector3(), 10000, 10, 10000);        box.setModelBound(new BoundingBox());        box.addController(new Controller() {            private static final long serialVersionUID = 1L;            double timer = 0;            @Override            public void update(final double time, final Spatial caller) {                timer += time;                caller.setTranslation(Math.sin(timer) * 20.0, 0, Math.cos(timer) * 20.0);            }        });        _root.attachChild(box);    }    /**     * Setup occluders.     */    private Node setupOccluders() {        final Node occluders = new Node("occs");        _root.attachChild(occluders);        for (int i = 0; i < 50; i++) {            final double w = Math.random() * 40 + 10;            final double y = Math.random() * 20 + 10;            final Box b = new Box("box", new Vector3(), w, y, w);            b.setModelBound(new BoundingBox());            final double x = Math.random() * 1000 - 500;            final double z = Math.random() * 1000 - 500;            b.setTranslation(new Vector3(x, y, z));            occluders.attachChild(b);        }        final Torus torus = new Torus("torus", 64, 12, 10.0f, 15.0f);        torus.setModelBound(new BoundingBox());        torus.setVBOInfo(new VBOInfo(true));        occluders.attachChild(torus);        torus.addController(new Controller() {            private static final long serialVersionUID = 1L;            double timer = 0;            Matrix3 rotation = new Matrix3();            @Override            public void update(final double time, final Spatial caller) {                timer += time;                caller.setTranslation(Math.sin(timer) * 40.0, Math.sin(timer) * 50.0 + 20.0, Math.cos(timer) * 40.0);                rotation.fromAngles(timer * 0.4, timer * 0.4, timer * 0.4);                caller.setRotation(rotation);            }        });        return occluders;    }}