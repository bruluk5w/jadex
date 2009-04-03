package jadex.adapter.base.envsupport.observer.graphics;

import jadex.adapter.base.envsupport.math.IVector2;
import jadex.adapter.base.envsupport.math.Vector2Double;
import jadex.adapter.base.envsupport.observer.graphics.drawable.DrawableCombiner;
import jadex.adapter.base.envsupport.observer.graphics.layer.ILayer;
import jadex.bridge.ILibraryService;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;


/**
 * This class manages the GUI and all user interaction.
 */
public class ViewportJ2D extends AbstractViewport implements ComponentListener
{
	private Map			imageCache_;

	/** Action that renders the frame. */
	private Runnable	renderFrameAction_;

	/**
	 * Creates a new Viewport.
	 * 
	 * @param libService the library service
	 */
	public ViewportJ2D(ILibraryService libService)
	{
		super();

		libService_ = libService;
		imageCache_ = Collections.synchronizedMap(new HashMap());

		canvas_ = new ViewportCanvas();
		canvas_.addComponentListener(this);

		canvas_.addMouseListener(new MouseController());

		renderFrameAction_ = new Runnable()
		{
			public void run()
			{
				canvas_.repaint();
			};
		};
	}

	/**
	 * Returns an image for texturing
	 * 
	 * @param path resource path of the image
	 */
	public BufferedImage getImage(String path)
	{
		BufferedImage image = (BufferedImage)imageCache_.get(path);

		if(image == null)
		{
			image = loadImage(path);
			imageCache_.put(path, image);
		}

		return image;
	}

	public void refresh()
	{
		EventQueue.invokeLater(renderFrameAction_);
	}

	/**
	 * Sets up the image transform.
	 * 
	 * @param sizeX image x-size
	 * @param sizeY image y-size
	 * @return the transform
	 */
	public AffineTransform getImageTransform(int sizeX, int sizeY)
	{
		AffineTransform imageTransform = new AffineTransform();
		imageTransform.translate(1.0 * inversionFlag_.getXAsInteger(),
				1.0 * inversionFlag_.getYAsInteger());
		imageTransform.scale(-((inversionFlag_.getXAsInteger() << 1) - 1)
				/ (double)sizeX, -((inversionFlag_.getYAsInteger() << 1) - 1)
				/ (double)sizeY);
		return imageTransform;
	}

	// Component events
	public void componentHidden(ComponentEvent e)
	{
	}

	public void componentMoved(ComponentEvent e)
	{
	}

	public void componentResized(ComponentEvent e)
	{
		setSize(size_);
	}

	public void componentShown(ComponentEvent e)
	{
	}

	/**
	 * Loads an image.
	 * 
	 * @param path resource path of the image
	 */
	private BufferedImage loadImage(String path)
	{
		ClassLoader cl = libService_.getClassLoader();

		BufferedImage image = null;
		try
		{
			image = ImageIO.read(cl.getResource(path));
			AffineTransform tf = AffineTransform.getScaleInstance(1, -1);
			tf.translate(0, -image.getHeight());
			AffineTransformOp op = new AffineTransformOp(tf,
					AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			image = op.filter(image, null);
		}
		catch(Exception e)
		{
		}

		return image;
	}

	private class ViewportCanvas extends Canvas
	{
		private BufferedImage		backBuffer_;

		private Rectangle.Double	clearRectangle_;

		private GeneralPath			scissorPolygon_;

		public ViewportCanvas()
		{
			backBuffer_ = new BufferedImage(1, 1,
					BufferedImage.TYPE_4BYTE_ABGR_PRE);
			scissorPolygon_ = new GeneralPath();
			setupScissorPolygon();
			clearRectangle_ = new Rectangle.Double();
			clearRectangle_.x = 0.0;
			clearRectangle_.y = 0.0;
			clearRectangle_.width = size_.getXAsDouble();
			clearRectangle_.height = size_.getYAsDouble();
		}

		public Dimension minimumSize()
		{
			return new Dimension(1, 1);
		}

		public Dimension getMinimumSize()
		{
			return new Dimension(1, 1);
		}

		public void paint(Graphics gfx)
		{
			try
			{
				if((backBuffer_.getWidth() != getWidth())
						|| (backBuffer_.getHeight() != getHeight()))
				{
					backBuffer_ = new BufferedImage(getWidth(), getHeight(),
							BufferedImage.TYPE_4BYTE_ABGR_PRE);
					setupScissorPolygon();
				}

				Graphics2D g = (Graphics2D)backBuffer_.getGraphics();
				g.setColor(java.awt.Color.BLACK);
				g.fillRect(0, 0, getWidth(), getHeight());
				setupTransform(g);

				while(!newDrawableCombiners_.isEmpty())
				{
					DrawableCombiner d = (DrawableCombiner)newDrawableCombiners_
							.remove(0);
					d.init(ViewportJ2D.this, g);
				}

				synchronized(preLayers_)
				{
					Iterator it = preLayers_.iterator();
					while(it.hasNext())
					{
						ILayer l = (ILayer)it.next();
						l.draw(size_, ViewportJ2D.this, g);
					}
				}

				synchronized(objectList_)
				{
					synchronized(objectLayers_)
					{
						AffineTransform tf = g.getTransform();
						g.translate(objShiftX_, objShiftY_);
						for(Iterator it = objectLayers_.iterator(); it
								.hasNext();)
						{
							Integer layer = (Integer)it.next();
							Iterator it2 = objectList_.iterator();
							while(it2.hasNext())
							{
								Object[] o = (Object[])it2.next();
								IVector2 pos = (IVector2)o[0];
								IVector2 vel = (IVector2)o[1];
								DrawableCombiner d = (DrawableCombiner)o[2];
								d.setPosition(pos);
								if(vel != null)
								{
									d.setVelocity(vel);
								}
								else
								{
									d.setVelocity(Vector2Double.ZERO);
								}
								d.draw(layer, ViewportJ2D.this, g);
							}
						}
						g.setTransform(tf);
					}
				}

				synchronized(postLayers_)
				{
					Iterator it = postLayers_.iterator();
					while(it.hasNext())
					{
						ILayer l = (ILayer)it.next();
						l.draw(size_, ViewportJ2D.this, g);
					}
				}

				// glScissor replacement
				g.setColor(java.awt.Color.BLACK);
				g.fill(scissorPolygon_);

				g.dispose();

				gfx.drawImage(backBuffer_, 0, 0, null);
				gfx.dispose();
			}
			catch(IllegalStateException e)
			{
			}
		}

		public void update(Graphics g)
		{
			paint(g);
		}

		private void setupTransform(Graphics2D g)
		{
			g.translate(
					backBuffer_.getWidth() * inversionFlag_.getXAsInteger(),
					backBuffer_.getHeight()
							* (inversionFlag_.getYAsInteger() ^ 1));
			g.scale((backBuffer_.getWidth() / paddedSize_.getXAsDouble())
					* -((inversionFlag_.getXAsInteger() << 1) - 1),
					(backBuffer_.getHeight() / paddedSize_.getYAsDouble())
							* ((inversionFlag_.getYAsInteger() << 1) - 1));
			g.translate(-posX_, -posY_);
		}

		private void setupScissorPolygon()
		{
			scissorPolygon_.reset();
			scissorPolygon_.moveTo(posX_, posY_);
			scissorPolygon_.lineTo(paddedSize_.getXAsFloat(), posY_);
			scissorPolygon_.lineTo(paddedSize_.getXAsFloat(), paddedSize_
					.getYAsFloat());
			scissorPolygon_.lineTo(posX_, paddedSize_.getYAsFloat());
			scissorPolygon_.lineTo(posX_, size_.getYAsFloat());
			scissorPolygon_.lineTo(size_.getXAsFloat(), size_.getYAsFloat());
			scissorPolygon_.lineTo(size_.getXAsFloat(), 0.0f);
			scissorPolygon_.lineTo(0.0f, 0.0f);
			scissorPolygon_.lineTo(0.0f, size_.getYAsFloat());
			scissorPolygon_.lineTo(posX_, size_.getYAsFloat());
			scissorPolygon_.closePath();
		}
	}
}
