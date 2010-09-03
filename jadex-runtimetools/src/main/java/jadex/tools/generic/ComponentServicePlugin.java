package jadex.tools.generic;

import jadex.base.gui.plugin.AbstractJCCPlugin;
import jadex.commons.Properties;
import jadex.commons.SGUI;
import jadex.commons.concurrent.SwingDefaultResultListener;
import jadex.commons.service.SServiceProvider;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIDefaults;

/**
 *  The generic plugin for a specified component or service. 
 */
public abstract class ComponentServicePlugin extends AbstractJCCPlugin
{
	//-------- constants --------

	/** The image icons. */
	protected static final UIDefaults icons = new UIDefaults(new Object[]
	{
		"conversation",	SGUI.makeIcon(ComponentServicePlugin.class, "/jadex/tools/common/images/libcenter.png"),
		"conversation_sel", SGUI.makeIcon(ComponentServicePlugin.class, "/jadex/tools/common/images/libcenter_sel.png"),
	});

	//-------- attributes --------
	
	//-------- methods --------
	
	/**
	 *  Get the service type.
	 *  @return The service type.
	 */
	public abstract Class getServiceType();
	
	/**
	 *  Get the model name.
	 *  @return the model name.
	 */
	public abstract String getModelName();
	
	/**
	 *  Test if this plugin should be initialized lazily.
	 *  @return True, if lazy.
	 */
	public boolean isLazy()
	{
		return true;
	}
	
	/**
	 * @return "Library Tool"
	 * @see jadex.base.gui.plugin.IControlCenterPlugin#getName()
	 */
	public String getName()
	{
		return getModelName()!=null? getModelName(): getServiceType().getName();
	}

	/**
	 * @return the conversation icon
	 * @see jadex.base.gui.plugin.IControlCenterPlugin#getToolIcon()
	 */
	public Icon getToolIcon(boolean selected)
	{
		return selected? icons.getIcon("conversation_sel"): icons.getIcon("conversation");
	}

	/**
	 *  Create main panel.
	 *  @return The main panel.
	 */
	public JComponent createView()
	{		
		JPanel mainp = new JPanel(new BorderLayout());
		
		JPanel northp = new JPanel(new FlowLayout());
		final JComboBox selcb = new JComboBox(); 
		final JButton refreshb = new JButton("Refresh");
		northp.add(new JLabel(getModelName()!=null? "Select component": "Select service"));
		northp.add(selcb);
		northp.add(refreshb);
		
		refreshb.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				refreshCombo(selcb);
			}
		});
		selcb.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				System.out.println("Selected : "+selcb.getSelectedItem());
			}
		});
		
		mainp.add(northp, BorderLayout.NORTH);
		
		return mainp;
	}
	
	/**
	 * 
	 */
	public void refreshCombo(final JComboBox selcb)
	{
		boolean remote = false;

		if(getModelName()!=null)
		{
			SServiceProvider.getService(getJCC().getServiceProvider(), , remote)
				.addResultListener(new SwingDefaultResultListener(getView()) 
			{
				public void customResultAvailable(Object source, Object result) 
				{
					
				}
			});
		}
		else
		{
			SServiceProvider.getServices(getJCC().getServiceProvider(), getServiceType(), remote)
				.addResultListener(new SwingDefaultResultListener(getView()) 
			{
				public void customResultAvailable(Object source, Object result) 
				{
					selcb.removeAllItems();
					Collection coll = (Collection)result;
					if(coll!=null)
					{
						for(Iterator it=coll.iterator(); it.hasNext(); )
						{
							selcb.addItem(it.next());
						}
					}
				}
			});
		}
	}

	/**
	 *  Set properties loaded from project.
	 */
	public void setProperties(Properties props)
	{
//		if(props.getProperty(getName())!=null);
//			((JSplitPane)getView()).setDividerLocation(props.getIntProperty("mainsplit_location"));
	}

	/**
	 *  Return properties to be saved in project.
	 */
	public Properties	getProperties()
	{
//		Properties	props	= new Properties();
//		props.addProperty(new Property("cp", urlstring));
//		return props;
		return null;
	}

	/** 
	 * @return the help id of the perspective
	 * @see jadex.base.gui.plugin.AbstractJCCPlugin#getHelpID()
	 */
	public String getHelpID()
	{
		return "tools."+getName();
	}
	
	/**
	 *  Reset the conversation center to an initial state
	 */
	public void	reset()
	{
	}
}

