package jadex.base.test.impl;


import jadex.base.test.ComponentTestSuite;
import jadex.base.test.TestReport;
import jadex.base.test.Testcase;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.modelinfo.IModelInfo;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.commons.future.ISuspendable;
import jadex.commons.future.ITuple2Future;
import jadex.commons.future.ThreadSuspendable;

import java.util.Iterator;
import java.util.Map;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestResult;

/**
 *  Test a component.
 */
public class ComponentTest extends TestCase
{
	//-------- attributes --------
	
	/** The component management system. */
	protected IComponentManagementService	cms;
	
	/** The component. */
	protected IModelInfo	comp;
	
	/** The test suite. */
	protected ComponentTestSuite	suite;
	
	//-------- constructors --------
	
	/**
	 *  Create a component test.
	 */
	public ComponentTest(IComponentManagementService cms, IModelInfo comp, ComponentTestSuite suite)
	{
		this.cms	= cms;
		this.comp	= comp;
		this.suite	= suite;
	}
	
	//-------- methods --------
	
	/**
	 *  The number of test cases.
	 */
	public int countTestCases()
	{
		return 1;
	}
	
	/**
	 *  Test the component.
	 */
	public void run(TestResult result)
	{
		System.out.println("Starting test...");
		System.out.println("Starting test: "+comp);
		
		if(suite.isAborted())
		{
			System.out.println("Aborted test: "+comp);
			return;
		}
		
		System.out.println("Starting...");
		try
		{
			result.startTest(this);
		}
		catch(IllegalStateException e)
		{
			// Hack: Android test runner tries to do getClass().getMethod(...) for test name, grrr.
			// See: http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/2.2.1_r1/android/test/InstrumentationTestRunner.java#767
		}
		System.out.println("Started...");
		
		try
		{
			// Start the component.
//			Map	args	= new HashMap();
//			args.put("timeout", new Long(3000000));
//			CreationInfo	ci	= new CreationInfo(args);
			ISuspendable.SUSPENDABLE.set(new ThreadSuspendable());
			System.out.println("Creating component: "+comp);
			ITuple2Future<IComponentIdentifier, Map<String, Object>>	fut	= cms.createComponent(null, comp.getFilename(), new CreationInfo(comp.getResourceIdentifier()));

			// Evaluate the results.
			System.out.println("Waiting for result: "+comp);
			Map<String, Object>	res	= fut.getSecondResult();
			System.out.println("Results received: "+comp);
			Testcase	tc	= null;
			for(Iterator<Map.Entry<String, Object>> it=res.entrySet().iterator(); it.hasNext(); )
			{
				Map.Entry<String, Object> tup = it.next();
				if(tup.getKey().equals("testresults"))
				{
					tc = (Testcase)tup.getValue();
					break;
				}
			}
			
			if(tc!=null && tc.getReports()!=null)
			{
				TestReport[]	reports	= tc.getReports();
				if(tc.getTestCount()!=reports.length)
				{
					result.addFailure(this, new AssertionFailedError("Number of testcases do not match. Expected "+tc.getTestCount()+" but was "+reports.length+"."));			
				}
				for(int i=0; i<reports.length; i++)
				{
					if(!reports[i].isSucceeded())
					{
						result.addFailure(this, new AssertionFailedError(reports[i].getDescription()+" Failed with reason: "+reports[i].getReason()));
					}
				}
			}
			else
			{
				result.addFailure(this,  new AssertionFailedError("No test results provided by component: "+res));
			}
		}
		catch(Throwable e)
		{
			e.printStackTrace();
			System.out.println("Exception: "+comp+", "+e);
			result.addError(this, e);
		}

		System.out.println("Ending test: "+comp);
		result.endTest(this);
		System.out.println("Test ended: "+comp);

		// Remove references to Jadex resources to aid GC cleanup.
		cms	= null;
		comp	= null;
		suite	= null;
	}
	
	public String getName()
	{
		return this.toString();
	}
	
	
	/**
	 *  Get a string representation of this test.
	 */
	public String toString()
	{
		return comp.getFullName();
	}
}
