package jadex.adapter.base.envsupport.observer.perspective;

import jadex.adapter.base.envsupport.dataview.IDataView;
import jadex.adapter.base.envsupport.math.IVector1;
import jadex.adapter.base.envsupport.math.IVector2;
import jadex.adapter.base.envsupport.math.Vector1Double;
import jadex.adapter.base.envsupport.math.Vector2Double;
import jadex.adapter.base.envsupport.observer.graphics.IViewport;
import jadex.adapter.base.envsupport.observer.graphics.IViewportListener;
import jadex.adapter.base.envsupport.observer.graphics.ViewportJ2D;
import jadex.adapter.base.envsupport.observer.graphics.ViewportJOGL;
import jadex.adapter.base.envsupport.observer.graphics.drawable.DrawableCombiner;
import jadex.adapter.base.envsupport.observer.graphics.drawable.IDrawable;
import jadex.adapter.base.envsupport.observer.graphics.drawable.TexturedRectangle;
import jadex.adapter.base.envsupport.observer.graphics.layer.ILayer;
import jadex.adapter.base.envsupport.observer.gui.ObserverCenter;
import jadex.adapter.base.envsupport.observer.gui.SObjectInspector;
import jadex.bridge.ILibraryService;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

public class Perspective2D implements IPerspective
{
	/** Name of the presentation */
	protected String name;
	
	/** The ObserverCenter */
	protected ObserverCenter obscenter;
	
	/** The viewport */
	protected IViewport viewport;
	
	/** Selection controller
	 */
	protected SelectionController selectioncontroller;
	
	/** The selected object */
	protected Object selectedobject;
	
	/** Order in which objects are displayed */
	protected Comparator displayorder;
	
	/** The object shift */
	protected IVector2 objectShift;
	
	/** Maximum selection distance */
	protected IVector1 selectorDistance;
	
	/** Flag if the x-axis should be inverted */
	protected boolean invertxaxis;
	
	/** Flag if the y-axis should be inverted */
	protected boolean invertyaxis;
	
	/** Try OpenGL if true */
	protected boolean tryopengl;
	
	/** The visuals (DrawableCombiners) */
	protected Map visuals;
	
	/** The prelayers */
	protected ILayer[] prelayers;
	
	/** The postlayers */
	protected ILayer[] postlayers;
	
	/** The marker drawable combiner */
	protected DrawableCombiner marker;
	
	/**
	 * Creates a 2D-Perspective.
	 */
	public Perspective2D()
	{
		this.visuals = Collections.synchronizedMap(new HashMap());
		this.prelayers = new ILayer[0];
		this.postlayers = new ILayer[0];
		
		this.objectShift = new Vector2Double();
		this.selectorDistance = new Vector1Double(1.0);
		this.tryopengl = true;
		
		this.displayorder = null;
		
		this.name = getClass().getName();
		viewport = null;
		selectioncontroller = new SelectionController();
	}
	
	/**
	 * Returns the name of the perspective
	 * @return name of the perspective
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Sets the name of the perspective
	 * @param name name of the perspective
	 */
	public void setName(String name)
	{
		this.name = name;
	}
	
	/** Returns the currently selected object.
	 * 
	 *  @return currently selected object
	 */
	public Object getSelectedObject()
	{
		return selectedobject;
	}
	
	/**
	 * Sets the selected object.
	 * 
	 *  @param obj selected object
	 */
	public void setSelectedObject(Object obj)
	{
		selectedobject = obj;
	}
	
	/**
	 * Sets the ObserverCenter.
	 * @param obscenter the ObserverCenter
	 */
	public void setObserverCenter(ObserverCenter obscenter)
	{
		this.obscenter = obscenter;
	}
	
	/**
	 * Adds a new visual object.
	 * @param id identifier of the object
	 * @param visual the visual object
	 */
	public void addVisual(Object id, Object visual)
	{
		visuals.put(id, visual);
	}
	
	/**
	 * Removes a new visual object.
	 * @param id identifier of the object
	 */
	public void removeVisual(Object id)
	{
		visuals.remove(id);
	}
	
	/**
	 * Returns the prelayers.
	 * @return the prelayers
	 */
	public ILayer[] getPrelayers()
	{
		return prelayers;
	}
	
	/**
	 * Sets the prelayers.
	 * @param prelayers the prelayers
	 */
	public void setPrelayers(ILayer[] prelayers)
	{
		this.prelayers = prelayers;
	}
	
	/**
	 * Returns the Postlayers.
	 * @return the Postlayers
	 */
	public ILayer[] getPostlayers()
	{
		return postlayers;
	}
	
	/**
	 * Sets the Postlayers.
	 * @param Postlayers the Postlayers
	 */
	public void setPostlayers(ILayer[] postlayers)
	{
		this.postlayers = postlayers;
	}
	
	/**
	 * Gets the drawable combiner object for the object marker
	 * @return the marker drawable
	 */
	public DrawableCombiner getMarkerDrawCombiner()
	{
		return marker;
	}
	
	/**
	 * Sets the drawable combiner object for the object marker
	 * @param marker the marker drawable
	 */
	public void setMarkerDrawCombiner(DrawableCombiner marker)
	{
		this.marker = marker;
	}
	
	/**
	 * Gets the view of the perspective.
	 * @return the view
	 */
	public Component getView()
	{
		if (viewport == null)
		{
			if (marker == null)
			{
				marker = new DrawableCombiner();
				IDrawable markerDrawable = new TexturedRectangle(getClass().getPackage().getName().replaceAll("perspective", "").concat("images.").replaceAll("\\.", "/").concat("selection_marker.png"));
				marker.addDrawable(markerDrawable, Integer.MAX_VALUE);
			}
			viewport = createViewport(obscenter.getLibraryService(), tryopengl);
			viewport.setSize(obscenter.getAreaSize());
			viewport.addViewportListener(selectioncontroller);
		}
		return viewport.getCanvas();
	}
	
	/**
	 * Sets whether to try to use OpenGL.
	 * @param opengl true, if attempt should be made to use OpenGL
	 */
	public void setOpenGl(boolean opengl)
	{
		this.tryopengl = opengl;
	}
	
	/**
	 * Gets x-axis inversion.
	 * @return true, if the x-axis should be inverted.
	 */
	public boolean getInvertXAxis()
	{
		return invertxaxis;
	}
	
	/**
	 * Sets x-axis inversion.
	 * @param invert true, if the x-axis should be inverted.
	 */
	public void setInvertXAxis(boolean invert)
	{
		invertxaxis = invert;
	}
	
	/**
	 * Gets y-axis inversion.
	 * @return true, if the y-axis should be inverted.
	 */
	public boolean getInvertYAxis()
	{
		return invertyaxis;
	}
	
	/**
	 * Sets y-axis inversion.
	 * @param invert true, if the y-axis should be inverted.
	 */
	public void setInvertYAxis(boolean invert)
	{
		invertyaxis = invert;
	}
	
	/** 
	 * Sets the maximum distance for selecting objects.
	 * 
	 * @param maxDist selections distance
	 */
	public synchronized void setSelectorDistance(IVector1 maxDist)
	{
		selectorDistance = maxDist;
	}
	
	/** 
	 * Gets the maximum distance for selecting objects.
	 * 
	 * @returns selections distance
	 */
	public synchronized IVector1 getSelectorDistance()
	{
		return selectorDistance;
	}
	
	/**
	 * Gets the display order.
	 * @return the display order
	 */
	public synchronized Comparator getDisplayOrder()
	{
		return displayorder;
	}
	
	/**
	 * Sets the display order.
	 * @param order the display order
	 */
	public synchronized void setDisplayOrder(Comparator order)
	{
		displayorder = order;
	}
	
	/**
	 * Gets the object shift.
	 * @return the object shift
	 */
	public synchronized IVector2 getObjectShift()
	{
		return objectShift;
	}
	
	/**
	 * Sets the object shift.
	 * @param shift the object shift
	 */
	public synchronized void setObjectShift(IVector2 shift)
	{
		objectShift = shift.copy();
	}
	
	/**
	 * Refreshes the perspective.
	 */
	public void refresh()
	{
		final IDataView dataview = obscenter.getSelectedDataView();
		if (dataview == null)
		{
			return;
		}
		
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				viewport.setInvertX(invertxaxis);
				viewport.setInvertY(invertyaxis);
				viewport.setObjectShift(objectShift);
				
				// Set pre- and postlayers
				viewport.setPreLayers(prelayers);
				viewport.setPostLayers(postlayers);
				
				Object[] objects = dataview.getObjects();

				List objectList = null;
				objectList = new ArrayList(objects.length + 1);
				for (int j = 0; j < objects.length; ++j )
				{
					Object obj = objects[j];
					DrawableCombiner d = (DrawableCombiner) visuals.get(SObjectInspector.getType(obj));
					if (d == null)
					{
						continue;
					}
					Object[] viewObj = new Object[2];
					viewObj[0] = obj;
					viewObj[1] = d;
					objectList.add(viewObj);
				}

				if (selectedobject != null)
				{
					Object size = ((DrawableCombiner)visuals.get(SObjectInspector.getType(selectedobject))).getSize();
					size = SObjectInspector.getVector2(selectedobject, size).copy().multiply(2.0);
					Object[] viewObj = new Object[2];
					marker.setSize((IVector2) size);
					viewObj[0] = selectedobject;
					viewObj[1] = marker;
					objectList.add(viewObj);
				}
				else
				{
					selectedobject = null;
				}

				if (displayorder != null)
				{
					Collections.sort(objectList, displayorder);
				}

				viewport.setObjectList(objectList);
				viewport.refresh();
			}
		});
	}
	
	private IViewport createViewport(ILibraryService libService, boolean tryopengl)
	{
		final JFrame frame = new JFrame("");
		frame.setLayout(new BorderLayout());
		frame.setUndecorated(true);
		frame.pack();
		frame.setSize(1, 1);
		
		if (tryopengl)
		{
			// Try OpenGL...
			try
			{
				ViewportJOGL vp = new ViewportJOGL(libService);
				frame.add(vp.getCanvas());
				frame.setVisible(true);
				if (!vp.isValid())
				{
					System.err.println("OpenGL support insufficient, using Java2D fallback...");
					tryopengl = false;
				}
				frame.dispose();
			}
			catch (RuntimeException e0)
			{
				System.err.println("OpenGL initialization failed, using Java2D fallback...");
				System.err.println(e0);
				tryopengl = false;
			}
			catch (Error e1)
			{
				System.err.println("OpenGL initialization failed, using Java2D fallback...");
				System.err.println(e1);
				tryopengl = false;
			}
		}
		
		IViewport viewport = null;
		if (tryopengl)
		{
			viewport = new ViewportJOGL(libService);
		}
		else
		{
			viewport = new ViewportJ2D(libService);
		}
		return viewport;
	}
	
	private class SelectionController implements IViewportListener
	{
		public void leftClicked(IVector2 position)
		{
			IDataView dataview = obscenter.getSelectedDataView();
			if (dataview == null)
				return;
			position = position.copy().subtract(objectShift);
			
			IVector1 minDist = null;
			Object closest = null;
			Object[] objects = dataview.getObjects();
			for (int i = 0; i < objects.length; ++i)
			{
				DrawableCombiner d = (DrawableCombiner) visuals.get(SObjectInspector.getType(objects[i]));
				Object pBind = d.getPosition();
				IVector2 objPos = SObjectInspector.getVector2(objects[i], pBind);
				if (objPos == null)
				{
					continue;
				}
				if((closest == null) || (position.getDistance(objPos).less(minDist)))
				{
					closest = objects[i];
					minDist = position.getDistance(objPos);
				}
			}
			
			if ((closest != null) && (minDist.less(selectorDistance)))
			{
				selectedobject = closest;
			}
		}
		
		public void rightClicked(IVector2 position)
		{
		}
	}
}
