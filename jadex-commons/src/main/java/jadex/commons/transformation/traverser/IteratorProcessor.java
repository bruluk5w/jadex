package jadex.commons.transformation.traverser;

import jadex.commons.SReflect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *  Processor for handling iterators.
 */
public class IteratorProcessor implements ITraverseProcessor
{
	/**
	 *  Test if the processor is appliable.
	 *  @param object The object.
	 *  @return True, if is applicable. 
	 */
	public boolean isApplicable(Object object, Class clazz, boolean clone)
	{
		return SReflect.isSupertype(Iterator.class, clazz);
	}
	
	/**
	 *  Process an object.
	 *  @param object The object.
	 *  @return The processed object.
	 */
	public Object process(Object object, Class<?> clazz, List<ITraverseProcessor> processors, 
		Traverser traverser, Map<Object, Object> traversed, boolean clone, Object context)
	{
		Iterator it = (Iterator)object;
		List copy = new ArrayList();
		Iterator ret = new UncheckedIterator(copy);

		traversed.put(object, ret);

		for(; it.hasNext(); )
		{
			Object val = it.next();
			Class valclazz = val!=null? val.getClass(): null;
			Object newval = traverser.traverse(val, valclazz, traversed, processors, clone, context);
			copy.add(newval);
		}
		
		return ret;
	}
	
//	/**
//	 * 
//	 */
//	public static void main(String[] args)
//	{
//		ArrayList list = new ArrayList();
////		Vector list = new Vector();
//		
//		Iterator it= new UncheckedIterator(list);
////		Enumeration it = list.elements();
//		
//		list.add("a");
//		list.add("b");
//		list.add("c");
//		
//		for(; it.hasNext();)
//		{
//			System.out.println("elem: "+it.next());
//		}
//	}
	
}

class UncheckedIterator<E> implements Iterator<E>
{
	protected List<E> source;
	
	protected int pos;
	
	/**
	 *  Create a new Iterator.
	 */
	public UncheckedIterator(List<E> source)
	{
		this.source = source;
	}
	
	/**
	 *  Test if has next element.
	 */
    public boolean hasNext()
    {
    	return pos<source.size();
    }

    /**
     * 
     */
    public E next()
    {
    	return source.get(pos++);
    }

    /**
     * 
     */
    public void remove()
    {
    	source.remove(--pos);
    }
}

