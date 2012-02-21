package jadex.commons.transformation.binaryserializer;

import jadex.commons.SReflect;
import jadex.commons.gui.SGUI;
import jadex.commons.transformation.traverser.ITraverseProcessor;
import jadex.commons.transformation.traverser.Traverser;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 *  Codec for encoding and decoding Image objects.
 *
 */
public class ImageCodec implements ITraverseProcessor, IDecoderHandler
{
	/**
	 *  Tests if the decoder can decode the class.
	 *  @param clazz The class.
	 *  @return True, if the decoder can decode this class.
	 */
	public boolean isApplicable(Class clazz)
	{
		return SReflect.isSupertype(Image.class, clazz);
	}
	
	/**
	 *  Decodes an object.
	 *  @param clazz The class of the object.
	 *  @param context The decoding context.
	 *  @return The decoded object.
	 */
	public Object decode(Class clazz, DecodingContext context)
	{
		Image ret = null;
		byte[] encimage = (byte[]) BinarySerializer.decodeObject(context);
		
		String classname = SReflect.getClassName(clazz);
		if(classname.indexOf("Toolkit")!=-1)
		{
			Toolkit t = Toolkit.getDefaultToolkit();
			ret = t.createImage(encimage);
		}
		else
		{
			try
			{
				ret = ImageIO.read(new ByteArrayInputStream(encimage));
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		return ret;
	}
	
	/**
	 *  Test if the processor is applicable.
	 *  @param object The object.
	 *  @return True, if is applicable. 
	 */
	public boolean isApplicable(Object object, Class<?> clazz, boolean clone)
	{
		return isApplicable(clazz);
	}
	
	/**
	 *  Process an object.
	 *  @param object The object.
	 *  @return The processed object.
	 */
	public Object process(Object object, Class<?> clazz, List<ITraverseProcessor> processors, 
		Traverser traverser, Map<Object, Object> traversed, boolean clone, Object context)
	{
		EncodingContext ec = (EncodingContext) context;
		
		object = ec.runPreProcessors(object, clazz, processors, traverser, traversed, clone, context);
		clazz = object == null? null : object.getClass();
		
		ec.writeClass(clazz);
		
		try
		{
			byte[] encimg = SGUI.imageToStandardBytes((Image) object, "image/png");
			traverser.traverse(encimg, encimg.getClass(), traversed, processors, clone, context);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		} 
		
		return object;
	}
}
