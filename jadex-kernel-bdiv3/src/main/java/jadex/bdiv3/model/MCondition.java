package jadex.bdiv3.model;

import jadex.bdiv3.runtime.ChangeEvent;
import jadex.bridge.modelinfo.UnparsedExpression;
import jadex.commons.MethodInfo;
import jadex.javaparser.javaccimpl.ExpressionNode;
import jadex.javaparser.javaccimpl.Node;
import jadex.javaparser.javaccimpl.ParameterNode;
import jadex.javaparser.javaccimpl.ReflectNode;
import jadex.rules.eca.EventType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 */
public class MCondition extends MElement
{
	/** The events this condition depends on. */
//	protected Set<String> events;
	protected List<EventType> events;
	
	//-------- pojo part --------
	
	/** The target method. */
	protected MethodInfo mtarget;
	
	/** The target constructor. */
	protected ConstructorInfo ctarget;
	
	//-------- additional xml properties --------
	
	/** Expression. */
	protected UnparsedExpression expression;
	
	/**
	 *	Bean Constructor. 
	 */
	public MCondition()
	{
	}
	
	/**
	 *  Create a new mcondition. 
	 */
	public MCondition(String name, List<EventType> events)
	{
		super(name);
		this.events = events;
	}

	/**
	 *  Get the mtarget.
	 *  @return The mtarget.
	 */
	public MethodInfo getMethodTarget()
	{
		return mtarget;
	}

	/**
	 *  Set the mtarget.
	 *  @param mtarget The mtarget to set.
	 */
	public void setMethodTarget(MethodInfo mtarget)
	{
		this.mtarget = mtarget;
	}

	/**
	 *  Get the ctarget.
	 *  @return The ctarget.
	 */
	public ConstructorInfo getConstructorTarget()
	{
		return ctarget;
	}

	/**
	 *  Set the ctarget.
	 *  @param ctarget The ctarget to set.
	 */
	public void setConstructorTarget(ConstructorInfo ctarget)
	{
		this.ctarget = ctarget;
	}

	/**
	 *  Get the events.
	 *  @return The events.
	 */
	public List<EventType> getEvents()
	{
		if(events==null && expression!=null && expression.getParsed() instanceof ExpressionNode)
		{
			Set<String>	done	= new HashSet<String>();
			events	= new ArrayList<EventType>();
			ParameterNode[]	params	= ((ExpressionNode)expression.getParsed()).getUnboundParameterNodes();
			for(ParameterNode param: params)
			{
				if("$beliefbase".equals(param.getText()))
				{
					Node parent	= param.jjtGetParent();
					if(parent instanceof ReflectNode)
					{
						ReflectNode	ref	= (ReflectNode)parent;
						if(ref.getType()==ReflectNode.FIELD && !done.contains(ref.getText()))
						{
							// Todo: differentiate between beliefs/sets
							events.add(new EventType(ChangeEvent.FACTCHANGED+"."+ref.getText()));
							events.add(new EventType(ChangeEvent.FACTADDED+"."+ref.getText()));
							events.add(new EventType(ChangeEvent.FACTREMOVED+"."+ref.getText()));
							
							done.add(ref.getText());
						}
						// todo: getBelief("xxx")
					}
				}
			}
		}
		return events;
	}
	
	/**
	 *  Get the expression.
	 */
	public UnparsedExpression getExpression()
	{
		return expression;
	}
	
	/**
	 *  Set the expression.
	 */
	public void setExpression(UnparsedExpression expression)
	{
		this.expression = expression;
	}
}
