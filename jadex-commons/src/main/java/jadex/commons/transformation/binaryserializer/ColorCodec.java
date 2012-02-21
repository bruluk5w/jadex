package jadex.commons.transformation.binaryserializer;

import jadex.commons.transformation.traverser.ITraverseProcessor;
import jadex.commons.transformation.traverser.Traverser;

import java.awt.Color;
import java.util.List;
import java.util.Map;

/**
 *  Codec for encoding and decoding Color objects.
 *
 */
public class ColorCodec implements ITraverseProcessor, IDecoderHandler
{
	/**
	 *  Tests if the decoder can decode the class.
	 *  @param clazz The class.
	 *  @return True, if the decoder can decode this class.
	 */
	public boolean isApplicable(Class clazz)
	{
		return Color.class.equals(clazz);
	}
	
	/**
	 *  Decodes an object.
	 *  @param clazz The class of the object.
	 *  @param context The decoding context.
	 *  @return The decoded object.
	 */
	public Object decode(Class clazz, DecodingContext context)
	{
		byte[] ccomps = context.read(4);
		Color ret = new Color(ccomps[0] & 0xFF, ccomps[1] & 0xFF, ccomps[2] & 0xFF, ccomps[3] & 0xFF);
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
		
		Color c = (Color) object;
		
		byte[] ccomps = new byte[4];
		ccomps[0] = (byte) c.getRed();
		ccomps[1] = (byte) c.getGreen();
		ccomps[2] = (byte) c.getBlue();
		ccomps[3] = (byte) c.getAlpha();
		
		ec.writeClass(clazz);
		ec.write(ccomps);
		
		return object;
	}
}
