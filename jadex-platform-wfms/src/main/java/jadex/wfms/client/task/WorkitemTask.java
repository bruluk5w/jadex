package jadex.wfms.client.task;

import jadex.bpmn.model.MLane;
import jadex.bpmn.model.MParameter;
import jadex.bpmn.runtime.BpmnInterpreter;
import jadex.bpmn.runtime.ITask;
import jadex.bpmn.runtime.ITaskContext;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.service.IServiceContainer;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.SServiceProvider;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.wfms.client.IWorkitem;
import jadex.wfms.client.Workitem;
import jadex.wfms.service.IAAAService;
import jadex.wfms.service.IWorkitemHandlerService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class WorkitemTask implements ITask
{
	/**
	 *  Execute the task.
	 *  @param context	The accessible values.
	 *  @param process	The process instance executing the task.
	 *  @listener	To be notified, when the task has completed.
	 */
	public IFuture execute(final ITaskContext context, final BpmnInterpreter process)
	{
		final Future ret = new Future();
		IServiceContainer wfms = process.getServiceContainer();
		SServiceProvider.getService(wfms, IWorkitemHandlerService.class, RequiredServiceInfo.SCOPE_APPLICATION).addResultListener(new DefaultResultListener()
		{
			
			public void resultAvailable(Object result)
			{
				IWorkitemHandlerService wh = (IWorkitemHandlerService) result;
				wh.queueWorkitem(createWorkitem(process.getComponentIdentifier(), context), createListener(context, ret));
			}
		});
		return ret;
	}
	
	/**
	 * Creates a workitem.
	 * @param context The task context
	 * @param listener The result listener
	 * @return a new workitem
	 */
	private static IWorkitem createWorkitem(IComponentIdentifier process, ITaskContext context)
	{
		Map parameterTypes = new LinkedHashMap();
		Map parameterValues = new HashMap();
		Map metaProperties = new HashMap();
		Set readOnlyParameters = new HashSet();
		Map parameters = context.getModelElement().getParameters();
		if (parameters != null)
		{
			for (Iterator it = parameters.keySet().iterator(); it.hasNext(); )
			{
				String pName = (String) it.next();
				MParameter param = (MParameter) parameters.get(pName);
				//MParameter param = (MParameter) it.next();
				if (param.getName().startsWith("META_P_"))
				{
					String paramName = param.getName().substring(7);
					String propName = paramName.substring(paramName.indexOf(':') + 1);
					paramName = paramName.substring(0, paramName.indexOf(':'));
					Map propertyMap = (Map) metaProperties.get(paramName);
					if (propertyMap == null)
					{
						propertyMap = new HashMap();
						metaProperties.put(paramName, propertyMap);
					}
					propertyMap.put(propName, context.getParameterValue(param.getName()));
				}
				else if (param.getName().startsWith("META_"))
				{
					String propName = param.getName().substring(5);
					if (propName.startsWith("category"))
					{
						String catName = propName.substring(propName.indexOf(':') + 1);
						propName = propName.substring(0, propName.indexOf(':'));
						String[] members = (String[]) context.getParameterValue(param.getName());
						for (int i = 0; i < members.length; ++i)
						{
							if (!metaProperties.containsKey(members[i]))
								metaProperties.put(members[i], new HashMap());
							((Map) metaProperties.get(members[i])).put(propName, catName);
						}
					}
					else
					{
						if (!metaProperties.containsKey(null))
							metaProperties.put(null, new HashMap());
						((Map) metaProperties.get(null)).put(propName, context.getParameterValue(param.getName()));
					}
				}
				else if (!param.getName().startsWith("IGNORE_"))
				{
					parameterTypes.put(param.getName(), param.getClazz());
					if (context.getParameterValue(param.getName()) != null)
						parameterValues.put(param.getName(), context.getParameterValue(param.getName()));
					if (param.getDirection().equals(MParameter.DIRECTION_IN))
						readOnlyParameters.add(param.getName());
				}
			}
		}
		
		String role = null;
		
		Map workitemMetaProps = (Map) metaProperties.get(null);
		String name = null;
		if (workitemMetaProps != null)
		{
			name = (String) workitemMetaProps.get("name");
			role = (String) workitemMetaProps.get("role");
		}
		if (name == null)
			name = context.getModelElement().getName();
		
		if (role == null)
		{
			MLane lane = context.getModelElement().getLane();
			if (lane != null)
				role = context.getModelElement().getLane().getName();
		}
		
		if (role == null)
			role = IAAAService.ANY_ROLE;
		
		Workitem wi = new Workitem(process, name, role, parameterTypes, parameterValues, metaProperties, readOnlyParameters);
		wi.setId(context.getModelElement().getName() + "_" + String.valueOf(Integer.toHexString(System.identityHashCode(wi))));
		return wi;
	}
	
	private static IResultListener createListener(final ITaskContext context, final Future future)
	{
		IResultListener redirListener = new IResultListener()
		{
			
			public void resultAvailable(Object result)
			{
				Workitem wi = (Workitem) result;
				Map parameterValues = wi.getParameterValues();
				Set readOnlyParameters = wi.getReadOnlyParameters();
				for (Iterator it = parameterValues.entrySet().iterator(); it.hasNext(); )
				{
					Map.Entry paramEntry = (Map.Entry) it.next();
					if (!readOnlyParameters.contains(paramEntry.getKey()))
					{
						context.setParameterValue((String) paramEntry.getKey(), paramEntry.getValue());
					}
				}
				future.setResult(result);
				//listener.resultAvailable(source, result);
			}
			
			public void exceptionOccurred(Exception exception)
			{
				//listener.exceptionOccurred(source, exception);
				future.setException(exception);
			}
		};
		return redirListener;
	}
}