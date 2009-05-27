/** * Copyright (c) 2008-2009 Ardor Labs, Inc. * * This file is part of Ardor3D. * * Ardor3D is free software: you can redistribute it and/or modify it  * under the terms of its license which may be found in the accompanying * LICENSE file or at <http://www.ardor3d.com/LICENSE>. */package com.ardor3d.example.effect;import java.awt.Color;import java.awt.image.BufferedImage;import com.ardor3d.example.ExampleBase;import com.ardor3d.extension.terrain.GeometryClipmapTerrain;import com.ardor3d.extension.terrain.HeightmapPyramid;import com.ardor3d.extension.terrain.basic.BasicHeightmap;import com.ardor3d.extension.terrain.basic.BasicHeightmapPyramid;import com.ardor3d.extension.terrain.heightmap.MidPointHeightMapGenerator;import com.ardor3d.framework.Canvas;import com.ardor3d.framework.FrameHandler;import com.ardor3d.image.Image;import com.ardor3d.image.Texture;import com.ardor3d.image.util.AWTImageLoader;import com.ardor3d.input.InputState;import com.ardor3d.input.Key;import com.ardor3d.input.logical.InputTrigger;import com.ardor3d.input.logical.KeyPressedCondition;import com.ardor3d.input.logical.LogicalLayer;import com.ardor3d.input.logical.TriggerAction;import com.ardor3d.light.DirectionalLight;import com.ardor3d.math.ColorRGBA;import com.ardor3d.math.MathUtils;import com.ardor3d.math.Vector3;import com.ardor3d.renderer.Camera;import com.ardor3d.renderer.queue.RenderBucketType;import com.ardor3d.renderer.state.CullState;import com.ardor3d.renderer.state.FogState;import com.ardor3d.renderer.state.TextureState;import com.ardor3d.renderer.state.FogState.DensityFunction;import com.ardor3d.scenegraph.Node;import com.ardor3d.scenegraph.extension.Skybox;import com.ardor3d.scenegraph.hint.LightCombineMode;import com.ardor3d.ui.text.BasicText;import com.ardor3d.util.ReadOnlyTimer;import com.ardor3d.util.TextureManager;import com.google.inject.Inject;/** * Example showing the parallel split shadow mapping technique. */public class GeometryClipmapTerrainExample extends ExampleBase {    final int SIZE = 2048;    private boolean updateTerrain = true;    private final float farPlane = 3500.0f;    private final float heightOffset = 3.0f;    // Clipmap parameters and a list of clipmaps that are used.    private final int clipLevelCount = 7;    private final int clipSideSize = 127;    private final float heightScale = 500;    private GeometryClipmapTerrain geometryClipmapTerrain;    private HeightmapPyramid heightmapPyramid;    private boolean groundCamera = true;    private Camera terrainCamera;    private final Vector3 lightDirection = new Vector3();    private Skybox skybox;    /** Text fields used to present info about the example. */    private final BasicText _exampleInfo[] = new BasicText[6];    /**     * The main method.     *      * @param args     *            the arguments     */    public static void main(final String[] args) {        start(GeometryClipmapTerrainExample.class);    }    /**     * Instantiates a new parallel split shadow map example.     *      * @param layer     *            the layer     * @param frameWork     *            the frame work     * @param timer     *            the timer     */    @Inject    public GeometryClipmapTerrainExample(final LogicalLayer layer, final FrameHandler frameWork) {        super(layer, frameWork);    }    private double counter = 0;    private int frames = 0;    /**     * Update the passmanager.     *      * @param tpf     *            the tpf     */    @Override    protected void updateExample(final ReadOnlyTimer timer) {        final Camera camera = _canvas.getCanvasRenderer().getCamera();        final double height = getHeight(camera.getLocation().getX(), camera.getLocation().getZ());        if (groundCamera || camera.getLocation().getY() < height + heightOffset) {            camera.setLocation(new Vector3(camera.getLocation().getX(), height + heightOffset, camera.getLocation()                    .getZ()));        }        if (updateTerrain) {            terrainCamera.set(camera);        }        geometryClipmapTerrain.setLightDirection(lightDirection);        skybox.setTranslation(camera.getLocation());                counter += timer.getTimePerFrame();        frames++;        if (counter > 1) {            final double fps = (frames / counter);            counter = 0;            frames = 0;            System.out.printf("%7.1f FPS\n", fps);        }    }    /**     * Initialize pssm pass and scene.     */    @Override    protected void initExample() {        // Setup main camera.        _canvas.setTitle("Geometry Clipmap Terrain - Example");        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 100, 0));        _canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(0, 100, 1), Vector3.UNIT_Y);        _canvas.getCanvasRenderer().getCamera().setFrustumPerspective(                65.0,                (float) _canvas.getCanvasRenderer().getCamera().getWidth()                        / _canvas.getCanvasRenderer().getCamera().getHeight(), 1.0f, farPlane);        _controlHandle.setMoveSpeed(5);        lightDirection.set(0.0, 1.0, 2.0).normalizeLocal();        _lightState.detachAll();        final DirectionalLight dLight = new DirectionalLight();        dLight.setEnabled(true);        dLight.setDiffuse(new ColorRGBA(1, 1, 1, 1));        dLight.setDirection(new Vector3(-1, -1, -1).normalizeLocal());        _lightState.attach(dLight);        _lightState.setEnabled(true);        final CullState cs = new CullState();        cs.setEnabled(true);        cs.setCullFace(CullState.Face.Back);        _root.setRenderState(cs);        final FogState fs = new FogState();        fs.setStart(farPlane / 2.0f);        fs.setEnd(farPlane);        fs.setColor(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));        fs.setDensityFunction(DensityFunction.Linear);        _root.setRenderState(fs);        try {            // Load an ugly procedural heightmap            final float[] heightMap = loadHeightMap();            // Create ugly procedural texturing            final TextureState ts = loadTextures(heightMap);            // Create a heightmap pyramid for the clipmap terrain to use            heightmapPyramid = new BasicHeightmapPyramid(new BasicHeightmap(heightMap, SIZE), clipLevelCount);            // Keep a separate camera to be able to freeze terrain update            final Camera camera = _canvas.getCanvasRenderer().getCamera();            terrainCamera = new Camera(camera);            // Create the monster terrain engine            geometryClipmapTerrain = new GeometryClipmapTerrain(terrainCamera, heightmapPyramid, clipSideSize,                    heightScale);            geometryClipmapTerrain.setRenderState(ts);            _root.attachChild(geometryClipmapTerrain);        } catch (final Exception ex1) {            ex1.printStackTrace();        }        skybox = buildSkyBox();        _root.attachChild(skybox);        // Setyp textfields for presenting example info.        final Node textNodes = new Node("Text");        _root.attachChild(textNodes);        textNodes.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);        textNodes.getSceneHints().setLightCombineMode(LightCombineMode.Off);        final double infoStartY = _canvas.getCanvasRenderer().getCamera().getHeight() / 2;        for (int i = 0; i < _exampleInfo.length; i++) {            _exampleInfo[i] = BasicText.createDefaultTextLabel("Text", "", 16);            _exampleInfo[i].setTranslation(new Vector3(10, infoStartY - i * 20, 0));            textNodes.attachChild(_exampleInfo[i]);        }        textNodes.updateGeometricState(0.0);        updateText();        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.U), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                updateTerrain = !updateTerrain;                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                _controlHandle.setMoveSpeed(5);                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                _controlHandle.setMoveSpeed(50);                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.THREE), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                _controlHandle.setMoveSpeed(400);                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                groundCamera = !groundCamera;                updateText();            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.I), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                if (geometryClipmapTerrain.getVisibleLevels() < clipLevelCount - 1) {                    geometryClipmapTerrain.setVisibleLevels(geometryClipmapTerrain.getVisibleLevels() + 1);                    updateText();                }            }        }));        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.K), new TriggerAction() {            public void perform(final Canvas source, final InputState inputState, final double tpf) {                if (geometryClipmapTerrain.getVisibleLevels() > 0) {                    geometryClipmapTerrain.setVisibleLevels(geometryClipmapTerrain.getVisibleLevels() - 1);                    updateText();                }            }        }));    }    private float[] loadHeightMap() {        // TODO: loading raw heightmap disabled for lower example media size        // final RawHeightMap raw = new RawHeightMap("heightmap.raw", SIZE);        // raw.setFormat(HeightMapFormat.UnsignedShort);        // raw.setLittleEndian(true);        // raw.setFlipY(true);        final MidPointHeightMapGenerator raw = new MidPointHeightMapGenerator(SIZE, 1.0f);        final float[] heightMap = raw.getHeightData();        return heightMap;    }    private TextureState loadTextures(final float[] heightMap) {        // TODO: loading generated texture/normals disabled for lower example media size        // final Texture colorTexture = TextureManager.load("texture.jpg",        // Texture.MinificationFilter.Trilinear, Format.GuessNoCompression, true);        final BufferedImage imgColor = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);        final BufferedImage imgNormal = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);        final ColorRGBA grass = new ColorRGBA(0.2f, 0.5f, 0.2f, 1.0f);        final ColorRGBA rock = new ColorRGBA(0.3f, 0.3f, 0.3f, 1.0f);        final ColorRGBA snow = new ColorRGBA(0.9f, 0.9f, 0.95f, 1.0f);        final ColorRGBA mud = new ColorRGBA(0.4f, 0.3f, 0.2f, 1.0f);        final ColorRGBA result = new ColorRGBA();        for (int i = 0; i < SIZE; i++) {            for (int j = 0; j < SIZE; j++) {                final float xDiff = heightMap[(i) * SIZE + wrap(j + 1, SIZE)]                        - heightMap[(i) * SIZE + wrap(j - 1, SIZE)];                final float yDiff = heightMap[wrap(i + 1, SIZE) * SIZE + (j)]                        - heightMap[wrap(i - 1, SIZE) * SIZE + (j)];                final Vector3 vec = new Vector3(xDiff, yDiff, 2f / heightScale).normalizeLocal();                final double dotAngle = vec.dot(0, 0, 1);                vec.multiplyLocal(0.5).addLocal(0.5, 0.5, 0.5);                imgNormal.setRGB(j, i, new Color(vec.getXf(), vec.getYf(), vec.getZf(), 0).getRGB());                ColorRGBA.lerp(rock, grass, (float) (dotAngle * dotAngle), result);                final float noise = (float) Math.random() * 0.06f - 0.03f;                result.addLocal(noise, noise, noise, 0.0f);                final float height = heightMap[i * SIZE + j];                ColorRGBA.lerp(result, snow, (float) Math.min(Math.max((height - 0.8) / 0.2, 0.0), 1.0), result);                ColorRGBA.lerp(result, mud, (float) Math.min(Math.max((0.2 - height) / 0.2, 0.0), 1.0), result);                imgColor.setRGB(j, i, new Color(result.getRed(), result.getGreen(), result.getBlue(), 0).getRGB());            }        }        final Image imageN = AWTImageLoader.makeArdor3dImage(imgNormal, false);        final Image imageT = AWTImageLoader.makeArdor3dImage(imgColor, false);        imgColor.flush();        imgNormal.flush();        final Texture colorTexture = TextureManager.loadFromImage(imageT, Texture.MinificationFilter.Trilinear,                Image.Format.GuessNoCompression, false);        final Texture normalTexture = TextureManager.loadFromImage(imageN, Texture.MinificationFilter.Trilinear,                Image.Format.GuessNoCompression, false);        final TextureState ts = new TextureState();        ts.setEnabled(true);        ts.setTexture(colorTexture, 0);        ts.setTexture(normalTexture, 1);        return ts;    }    private int wrap(final int value, final int size) {        int wrappedValue = value % size;        wrappedValue += wrappedValue < 0 ? size : 0;        return wrappedValue;    }    /**     * Builds the sky box.     */    private Skybox buildSkyBox() {        final Skybox skybox = new Skybox("skybox", 10, 10, 10);        final String dir = "images/skybox/";        final Texture north = TextureManager.load(dir + "1.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        final Texture south = TextureManager.load(dir + "3.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        final Texture east = TextureManager.load(dir + "2.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        final Texture west = TextureManager.load(dir + "4.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        final Texture up = TextureManager.load(dir + "6.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        final Texture down = TextureManager.load(dir + "5.jpg", Texture.MinificationFilter.BilinearNearestMipMap,                Image.Format.GuessNoCompression, true);        skybox.setTexture(Skybox.Face.North, north);        skybox.setTexture(Skybox.Face.West, west);        skybox.setTexture(Skybox.Face.South, south);        skybox.setTexture(Skybox.Face.East, east);        skybox.setTexture(Skybox.Face.Up, up);        skybox.setTexture(Skybox.Face.Down, down);        return skybox;    }    /**     * Update text information.     */    private void updateText() {        _exampleInfo[0].setText("Heightmap size: " + SIZE + "x" + SIZE);        _exampleInfo[1].setText("Spec: One meter per heightmap value");        _exampleInfo[2].setText("[1/2/3] Moving speed: " + _controlHandle.getMoveSpeed() + " m/s");        _exampleInfo[3].setText("[I/K] Max visible level: " + geometryClipmapTerrain.getVisibleLevels());        _exampleInfo[4].setText("[U] Update terrain: " + updateTerrain);        _exampleInfo[5].setText("[SPACE] Toggle fly/walk: " + (groundCamera ? "walk" : "fly"));    }    private double getHeight(double x, double z) {        x = x % SIZE;        x += x < 0 ? SIZE : 0;        z = z % SIZE;        z += z < 0 ? SIZE : 0;        final double col = Math.floor(x);        final double row = Math.floor(z);        final double intOnX = x - col, intOnZ = z - row;        double col1 = (col + 1) % SIZE;        col1 += col1 < 0 ? SIZE : 0;        double row1 = (row + 1) % SIZE;        row1 += row1 < 0 ? SIZE : 0;        // find the heightmap point closest to this position (but will always        // be to the left ( < x) and above (< z) of the spot.        final double topLeft = heightmapPyramid.getHeight(0, (int) col, (int) row);        // now find the next point to the right of topLeft's position...        final double topRight = heightmapPyramid.getHeight(0, (int) col1, (int) row);        // now find the next point below topLeft's position...        final double bottomLeft = heightmapPyramid.getHeight(0, (int) col, (int) row1);        // now find the next point below and to the right of topLeft's        // position...        final double bottomRight = heightmapPyramid.getHeight(0, (int) col1, (int) row1);        // Use linear interpolation to find the height.        return MathUtils.lerp(intOnZ, MathUtils.lerp(intOnX, topLeft, topRight), MathUtils.lerp(intOnX, bottomLeft,                bottomRight))                * heightScale;    }}