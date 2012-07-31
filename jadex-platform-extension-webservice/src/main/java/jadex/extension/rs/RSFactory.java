package jadex.extension.rs;

import jadex.bridge.IInternalAccess;
import jadex.commons.SReflect;

import java.lang.reflect.InvocationHandler;

public abstract class RSFactory
{
	private static RSFactory INSTANCE = null;
	
	public static RSFactory getInstance() {
		if (INSTANCE == null) {
			Class<?> clazz = null;
			if (SReflect.isAndroid()) {
				clazz = SReflect.classForName0("jadex.extension.rs.RSFactoryAndroid", null);
			} else {
				clazz = SReflect.classForName0("jadex.extension.rs.RSFactoryDesktop", null);
			}
			if (clazz != null) {
				try
				{
					INSTANCE = (RSFactory) clazz.newInstance();
				}
				catch (InstantiationException e)
				{
					e.printStackTrace();
				}
				catch (IllegalAccessException e)
				{
					e.printStackTrace();
				}
			}
		}
		return INSTANCE;
	}

	public abstract InvocationHandler createRSWrapperInvocationHandler(IInternalAccess agent, Class<?> impl);
}
