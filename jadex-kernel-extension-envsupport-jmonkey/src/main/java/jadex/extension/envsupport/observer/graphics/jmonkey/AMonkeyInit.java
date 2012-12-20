package jadex.extension.envsupport.observer.graphics.jmonkey;

import jadex.extension.envsupport.observer.graphics.jmonkey.camera.DungeonMasterCamera;
import jadex.extension.envsupport.observer.graphics.jmonkey.camera.FlyCamera;
import jadex.extension.envsupport.observer.graphics.jmonkey.camera.FocusCamera;
import jadex.extension.envsupport.observer.graphics.jmonkey.controller.GuiController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.SceneProcessor;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.BasicShadowRenderer;
import com.jme3.shadow.PssmShadowRenderer;
import com.jme3.terrain.geomipmap.TerrainQuad;
//import com.jme3.post.*;
//import com.jme3.post.ssao.SSAOFilter;

import de.lessvoid.nifty.Nifty;

/**
 * The Second Abstract Application for the renders the 3d output for Jadex in the Jmonkey Engine
 * 
 * This Class has most of the Getter and Setter and init Methods from the MonkeyApp for better structure
 * 
 * @author 7willuwe
 */



public abstract class AMonkeyInit extends SimpleApplication implements AnimEventListener 
{
	// The Animation Channels for Animations
	protected HashMap<String, AnimChannel> animChannels;
	
	protected ArrayList<Light> lights = new ArrayList<Light>();
	
	protected FilterPostProcessor fpp;
	
	//The Terrain
	protected TerrainQuad terrain;
	
	//The Cameras
	protected FocusCamera focusCam;
	protected FlyCamera flyCamera;
	
	// The NiftyGUIDisplay 
	protected NiftyJmeDisplay niftyDisplay;
	
	// Helper Classes jop
	protected Node geometryNode;
	protected Node gridNode;
	protected Node staticNode;
	
	// Dimensions
	protected float appDimension;
	protected float spaceSize;
	
	// The Selected Object by the User as Spatial and ID
	protected Spatial selectedSpatial;
	protected int selectedTarget;
	
	// Shadow Renderer
	protected PssmShadowRenderer pssmRenderer;
	protected BasicShadowRenderer bsr;
	
	//Booleans
	protected boolean isGrid;
	protected boolean walkCam;
	protected boolean hudactive = false;
	protected boolean focusCamActive = false;
	protected boolean complexShadows = true;
	protected boolean cleanupPostFilter = false;

	public AMonkeyInit(float dim, float spaceSize, boolean isGrid)
	{
		//Set the Variables
		this.appDimension = dim;
		this.isGrid = isGrid;
		this.spaceSize = spaceSize;
		this.geometryNode = new Node("geometryNode");
		this.staticNode = new Node("staticNode");
		this.gridNode = new Node("gridNode");
		this.walkCam = false;
		this.selectedTarget = -1;
		this.selectedSpatial = null;
		

	}
	
	protected void simpleInit() {
		// Base Setup
		Logger.getLogger("").setLevel(Level.SEVERE);
		Logger.getLogger("de.lessvoid.nifty").setLevel(Level.SEVERE);
		Logger.getLogger("NiftyInputEventHandlingLog").setLevel(Level.SEVERE);
		viewPort.setBackgroundColor(ColorRGBA.Black);
		stateManager.getState(StatsAppState.class).toggleStats();
		
		initRoot();
		initCam();
		initRenderer(complexShadows);
		initAudio();
		initNifty();
	}
	
	private void initRoot() {
		this.rootNode.attachChild(this.staticNode);
		
		DirectionalLight sun = new DirectionalLight();
		sun.setColor(ColorRGBA.White.mult(0.2f));
		sun.setDirection(new Vector3f(0, -1f, 0).normalizeLocal());
		
		
		DirectionalLight sun1 = new DirectionalLight();
		sun1.setColor(ColorRGBA.White.mult(0.7f));
		sun1.setDirection(new Vector3f(-.5f, -.5f, -.5f).normalizeLocal());
		
		DirectionalLight sun2 = new DirectionalLight();
		sun2.setColor(ColorRGBA.White.mult(0.7f));
		sun2.setDirection(new Vector3f(.5f, .5f, .5f).normalizeLocal());
		
		
//		rootNode.addLight(sun);
		rootNode.addLight(sun1);
		rootNode.addLight(sun2);
//		rootNode.addLight(sun3);
//		rootNode.addLight(sun4);
//		rootNode.addLight(sun5);

		AmbientLight al = new AmbientLight();
		al.setColor(ColorRGBA.White.mult(2f));
		rootNode.addLight(al);
		
		monkeyApp_Grid gridHandler = new monkeyApp_Grid(appDimension, spaceSize, assetManager, isGrid);
		this.gridNode = gridHandler.getGrid();
		this.rootNode.attachChild(this.geometryNode);

	}
	
	public void initCam() {

		// The Focus Camera
		focusCam = new FocusCamera(cam, staticNode, inputManager);
		focusCam.setUpVector(Vector3f.UNIT_Y);
		focusCam.setSmoothMotion(true);
		focusCam.setDragToRotate(true);
		focusCam.setDefaultDistance(1000f);
		focusCam.setMaxDistance(2000f);
		focusCam.setMinDistance(50f);
		focusCam.setZoomSpeed(20f);
		focusCam.setEnabled(false);

		// The Fly Camera
		 flyCamera = new FlyCamera(cam);
		 flyCamera.setUpVector(Vector3f.UNIT_Y);
		 flyCamera.registerWithInput(inputManager);
		 flyCamera.setMoveSpeed(appDimension);
		 flyCamera.setDragToRotate(true);
		 flyCamera.setEnabled(true);
		 
		 
		 flyCamera.setEnabled(false);
		 
			/** Configure cam to look at scene */
			cam.setLocation(new Vector3f(appDimension / 2 , 0, appDimension / 2));
			cam.lookAt(new Vector3f(appDimension / 2, 0, appDimension / 2), Vector3f.UNIT_Y);
			cam.setFrustumNear(1f);
			cam.setFrustumFar(appDimension * 5);
			cam.resize(1024, 1024, true);
			Box b = new Box(1f,1f,1f);
			Node boxnode = new Node("boxnode");
			Geometry geo = new Geometry("box", b);
			boxnode.attachChild(geo);
			boxnode.setLocalTranslation(appDimension / 2, 0, appDimension / 2);
//			Geometry g = b;
		
//		 ((Spatial) b).setLocalTranslation(appDimension / 2 , 0, appDimension / 2);
		 DungeonMasterCamera dungeonCam = new DungeonMasterCamera(cam, inputManager, boxnode, rootNode);
		 dungeonCam.setEnabled(true);

		 

//			cam.

		// Change the mappings we don't want

		//
		// Camera cam_n = cam.clone();
		// cam.setViewPort( 0.0f , 1.0f , 0.0f , 1.0f );
		// cam_n.setViewPort( 0.8f , 1.0f , 0.8f , 1.0f );
		// cam_n.setLocation(new Vector3f(_appDimension/2, _appDimension*1.5f,
		// _appDimension/2));
		// cam_n.lookAt(new Vector3f(_appDimension/2, 0, _appDimension/2),
		// Vector3f.UNIT_Y);

		// ViewPort view = renderManager.createMainView("View of camera #1",
		// cam);
		// view.setEnabled(true);
		// view.setClearFlags(true, true, true);
		// view.attachScene(rootNode);
		// view.setBackgroundColor(ColorRGBA.Black);
		//
		// ViewPort view_n = renderManager.createMainView("View of camera #2",
		// cam_n);
		// view_n.setEnabled(true);
		// view_n.setClearFlags(true, true, true);
		// view_n.attachScene(rootNode);
		// view_n.setBackgroundColor(ColorRGBA.Black);

	}
	
	
	private void initRenderer(boolean complexShadows) {
		

		this.fpp = new FilterPostProcessor(assetManager);
		
		if(complexShadows)
		{		
			pssmRenderer = new PssmShadowRenderer(assetManager, 1024, 3);
			pssmRenderer.setDirection(new Vector3f(-.5f, -.5f, -.5f).normalizeLocal()); // light direction
			pssmRenderer.setShadowIntensity(0.6f);
			viewPort.addProcessor(pssmRenderer);
			
			
			SSAOFilter ssaoFilter = new SSAOFilter(10.94f, 30.92f, 0.3f, 0.61f);
			fpp.addFilter(ssaoFilter);
			viewPort.addProcessor(fpp);
		}
		else
		{ 
			bsr = new BasicShadowRenderer(assetManager, 256);
			bsr.setDirection(new Vector3f(-.5f,-.5f,-.5f).normalizeLocal()); //light direction
			viewPort.addProcessor(bsr);
		 }
//		

		
		// BLOOOM FILTER
		//		 FilterPostProcessor fpp=new FilterPostProcessor(assetManager);
		//		 BloomFilter bf=new BloomFilter(BloomFilter.GlowMode.Objects);
		//		 fpp.addFilter(bf);
		//		 viewPort.addProcessor(fpp);

		rootNode.setShadowMode(ShadowMode.Off);

	}
	private void initNifty() {
		niftyDisplay = new NiftyJmeDisplay(assetManager, inputManager,
				audioRenderer, guiViewPort);
		/** Create a new NiftyGUI object */
		Nifty nifty = niftyDisplay.getNifty();
		/** Read your XML and initialize your custom ScreenController */
		nifty.fromXml("jadex3d/interface/BaseHud.xml", "hud",
				new GuiController(this));
		nifty.fromXml("jadex3d/interface/BaseHud.xml", "default",
				new GuiController(this));
		nifty.gotoScreen("default");

		guiViewPort.addProcessor(niftyDisplay);
	}
	
	private void initAudio() {
		// unused
	}
	public void onAnimChange(AnimControl control, AnimChannel channel,
			String animName) {
	}

	public void onAnimCycleDone(AnimControl control, AnimChannel channel,
			String animName) {
		if (channel.getLoopMode() == LoopMode.DontLoop) {
			channel.reset(true);
		}
	}
	
	public void simpleUpdateAbstract(float tpf) {
		//Update the SoundListener to the CamLocation and Rotation
		listener.setLocation(cam.getLocation());
		listener.setRotation(cam.getRotation());
		
	}
	
	//Getter and Setter
	
	public void setChannels(HashMap<String, AnimChannel> animChannels) {
		this.animChannels = animChannels;
	}

	public HashMap<String, AnimChannel> getChannels() {
		return this.animChannels;
	}
	
	public Collection<com.jme3.renderer.Caps> getCaps() {
		return renderer.getCaps();
	}

	/**
	 * @return the animChannels
	 */
	public HashMap<String, AnimChannel> getAnimChannels() {
		return animChannels;
	}

	/**
	 * @param animChannels the animChannels to set
	 */
	public void setAnimChannels(HashMap<String, AnimChannel> animChannels) {
		this.animChannels = animChannels;
	}

	/**
	 * @return the terrain
	 */
	public TerrainQuad getTerrain() {
		return terrain;
	}

	/**
	 * @param terrain the terrain to set
	 */
	public void setTerrain(TerrainQuad terrain) {
		this.terrain = terrain;
	}

	/**
	 * @return the focusCam
	 */
	public FocusCamera getFocusCam() {
		return focusCam;
	}

	/**
	 * @param focusCam the focusCam to set
	 */
	public void setFocusCam(FocusCamera focusCam) {
		this.focusCam = focusCam;
	}

	/**
	 * @return the flyCamera
	 */
	public FlyCamera getFlyCamera() {
		return flyCamera;
	}

	/**
	 * @param flyCamera the flyCamera to set
	 */
	public void setFlyCamera(FlyCamera flyCamera) {
		this.flyCamera = flyCamera;
	}

	/**
	 * @return the niftyDisplay
	 */
	public NiftyJmeDisplay getNiftyDisplay() {
		return niftyDisplay;
	}

	/**
	 * @param niftyDisplay the niftyDisplay to set
	 */
	public void setNiftyDisplay(NiftyJmeDisplay niftyDisplay) {
		this.niftyDisplay = niftyDisplay;
	}

	/**
	 * @return the geometryNode
	 */
	public Node getGeometryNode() {
		return geometryNode;
	}

	/**
	 * @param geometryNode the geometryNode to set
	 */
	public void setGeometryNode(Node geometryNode) {
		this.geometryNode = geometryNode;
	}

	/**
	 * @return the gridNode
	 */
	public Node getGridNode() {
		return gridNode;
	}

	/**
	 * @param gridNode the gridNode to set
	 */
	public void setGridNode(Node gridNode) {
		this.gridNode = gridNode;
	}

	/**
	 * @return the staticNode
	 */
	public Node getStaticNode() {
		return staticNode;
	}

	/**
	 * @param staticNode the staticNode to set
	 */
	public void setStaticNode(Node staticNode) {
		this.staticNode = staticNode;
	}

	/**
	 * @return the appDimension
	 */
	public float getAppDimension() {
		return appDimension;
	}

	/**
	 * @param appDimension the appDimension to set
	 */
	public void setAppDimension(float appDimension) {
		this.appDimension = appDimension;
	}

	/**
	 * @return the spaceSize
	 */
	public float getSpaceSize() {
		return spaceSize;
	}

	/**
	 * @param spaceSize the spaceSize to set
	 */
	public void setSpaceSize(float spaceSize) {
		this.spaceSize = spaceSize;
	}

	/**
	 * @return the selectedSpatial
	 */
	public Spatial getSelectedSpatial() {
		return selectedSpatial;
	}

	/**
	 * @param selectedSpatial the selectedSpatial to set
	 */
	public void setSelectedSpatial(Spatial selectedSpatial) {
		this.selectedSpatial = selectedSpatial;
	}

	/**
	 * @return the selectedTarget
	 */
	public int getSelectedTarget() {
		return selectedTarget;
	}

	/**
	 * @param selectedTarget the selectedTarget to set
	 */
	public void setSelectedTarget(int selectedTarget) {
		this.selectedTarget = selectedTarget;
	}

	/**
	 * @return the pssmRenderer
	 */
	public PssmShadowRenderer getPssmRenderer() {
		return pssmRenderer;
	}

	/**
	 * @param pssmRenderer the pssmRenderer to set
	 */
	public void setPssmRenderer(PssmShadowRenderer pssmRenderer) {
		this.pssmRenderer = pssmRenderer;
	}

	/**
	 * @return the bsr
	 */
	public BasicShadowRenderer getBsr() {
		return bsr;
	}

	/**
	 * @param bsr the bsr to set
	 */
	public void setBsr(BasicShadowRenderer bsr) {
		this.bsr = bsr;
	}

	/**
	 * @return the isGrid
	 */
	public boolean isGrid() {
		return isGrid;
	}

	/**
	 * @param isGrid the isGrid to set
	 */
	public void setGrid(boolean isGrid) {
		this.isGrid = isGrid;
	}

	/**
	 * @return the walkCam
	 */
	public boolean isWalkCam() {
		return walkCam;
	}

	/**
	 * @param walkCam the walkCam to set
	 */
	public void setWalkCam(boolean walkCam) {
		this.walkCam = walkCam;
	}

	/**
	 * @return the hudactive
	 */
	public boolean isHudactive() {
		return hudactive;
	}

	/**
	 * @param hudactive the hudactive to set
	 */
	public void setHudactive(boolean hudactive) {
		this.hudactive = hudactive;
	}

	/**
	 * @return the focusCamActive
	 */
	public boolean isFocusCamActive() {
		return focusCamActive;
	}

	/**
	 * @param focusCamActive the focusCamActive to set
	 */
	public void setFocusCamActive(boolean focusCamActive) {
		this.focusCamActive = focusCamActive;
	}

	/**
	 * @return the complexShadows
	 */
	public boolean isComplexShadows() {
		return complexShadows;
	}

	/**
	 * @param complexShadows the complexShadows to set
	 */
	public void setComplexShadows(boolean complexShadows) {
		this.complexShadows = complexShadows;
	}
	
	/**
	 * @return the assetManager
	 */
	public AssetManager getAssetManager() {
		return assetManager;
	}

	/**
	 * @return the lights
	 */
	public ArrayList<Light> getLights() {
		return lights;
	}

	/**
	 * @param lights the lights to set
	 */
	public void setLights(ArrayList<Light> lights) {
		this.lights = lights;
		for(Light l : this.lights)
		{
//			rootNode.removeLight(light)
			System.out.println("add light! \n");
			System.out.println("light: " + l.getColor() + l.getType() + " pos: " + ((PointLight)l).getPosition() + "radius " + ((PointLight)l).getRadius());
			rootNode.addLight(l);
		}
	}

	/**
	 * @return the cleanupPostFilter
	 */
	public boolean isCleanupPostFilter() {
		return cleanupPostFilter;
	}

	/**
	 * @param cleanupPostFilter the cleanupPostFilter to set
	 */
	public void setCleanupPostFilter(boolean cleanupPostFilter) {
		this.cleanupPostFilter = cleanupPostFilter;
	}




}
