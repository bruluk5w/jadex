package jadex.tools.security;

import jadex.base.gui.CMSUpdateHandler;
import jadex.base.gui.PlatformSelectorDialog;
import jadex.base.gui.componenttree.ComponentIconCache;
import jadex.base.gui.idtree.IdTreeModel;
import jadex.base.gui.idtree.IdTreeNode;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.types.security.ISecurityService;
import jadex.commons.ChangeEvent;
import jadex.commons.IChangeListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.commons.gui.PropertiesPanel;
import jadex.commons.gui.SGUI;
import jadex.commons.gui.TreeExpansionHandler;
import jadex.commons.gui.future.SwingExceptionDelegationResultListener;
import jadex.commons.gui.future.SwingResultListener;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

/**
 * 
 */
public class VirtualNamesPanel extends JPanel
{
	/** The external access of the platform. */
	protected IExternalAccess ea;
	
	/** The security service. */
	protected ISecurityService secser;
	
	/** The cmshandler. */
	protected CMSUpdateHandler cmshandler;
	
	/** The tree model. */
	protected JTree tree;
	protected IdTreeModel<String> model;
	
	/** The mode (virtual or platform first). */
	protected boolean platform;
	
	/** The change listeners. */
	protected List<IChangeListener<String>> listeners;
	
	/**
	 *  Create a new panel. 
	 */
	public VirtualNamesPanel(final IExternalAccess ea, final ISecurityService secser, final CMSUpdateHandler cmshandler, boolean platform)
	{
		this.ea = ea;
		this.secser = secser;
		this.cmshandler = cmshandler;
		this.platform = platform;
		this.listeners = new ArrayList<IChangeListener<String>>();
		
		setLayout(new BorderLayout());
		
		setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), platform? "Platform -> Virtual Name": "Virtual Name -> Platform"));
	
		model = new IdTreeModel<String>()
		{
			public void reload()
			{
				createTreeModel();
				super.reload();
			}
		};
		// Must be non-leaf to let expand call propagate :-(
		RootNode root = new RootNode();
		model.setRoot(root);
		
		tree = new JTree(model);
		new TreeExpansionHandler(tree);
		tree.setRootVisible(false);
		tree.expandPath(new TreePath(root.getPath()));
	
		add(new JScrollPane(tree), BorderLayout.CENTER);
		
		MouseAdapter ma = new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				popup(e);
			}
			
			public void mouseReleased(MouseEvent e)
			{
				popup(e);
			}
			
			protected void	popup(MouseEvent e)
			{
				if(e.isPopupTrigger())
				{
					JPopupMenu menu = new JPopupMenu("Virtuals menu");
					
					TreePath path = tree.getPathForLocation(e.getX(), e.getY());
					if(path==null)
					{
						path = new TreePath(model.getRoot());
					}
					tree.setSelectionPath(path);
					
					final IdTreeNode<String> node = (IdTreeNode<String>)tree.getLastSelectedPathComponent();
					
					if(node instanceof RootNode)
					{
						if(VirtualNamesPanel.this.platform)
						{
							menu.add(new SelectPlatformAction(node));
							menu.add(new AddPlatformAction(node));
						}
						else
						{
							menu.add(new AddVirtualPlatformAction(node));
						}
					}
					else if(node instanceof VirtualPlatformNode)
					{
						if(node.getParent() instanceof RootNode)
						{
							menu.add(new SelectPlatformAction(node));
							menu.add(new AddPlatformAction(node));
						}
						menu.add(new RemoveAction(node));
					}
					else if(node instanceof PlatformNode)
					{
						if(node.getParent() instanceof RootNode)
						{
							menu.add(new AddVirtualPlatformAction(node));
						}
						menu.add(new RemoveAction(node));
					}
					menu.add(new SwitchViewAction());
					
					menu.show(tree, e.getX(), e.getY());
				}
			}
		};
		
		tree.addMouseListener(ma);
		
		createTreeModel();
	}
	
	/**
	 *  Get the tree.
	 */
	protected JTree getTree()
	{
		return tree;
	}
	
	/**
	 * 
	 * @return
	 */
	protected IFuture<Void> createTreeModel()
	{
		if(platform)
		{
			return createPlatformsModel();
		}
		else
		{
			return createVirtualsModel();
		}
	}
	
	/**
	 * 
	 */
	protected IFuture<Void> createVirtualsModel()
	{
		final Future<Void> ret = new Future<Void>();
		
		secser.getVirtuals().addResultListener(new SwingExceptionDelegationResultListener<Map<String,Set<String>>, Void>(ret)
		{
			public void customResultAvailable(Map<String,Set<String>> virtuals) 
			{
				IdTreeNode<String> root = (IdTreeNode<String>)model.getRoot();
				root.removeAllChildren();
				vpcnt = 0;
				pcnt = 0;
				
				for(Map.Entry<String, Set<String>> virtual: virtuals.entrySet())
				{
					String v = virtual.getKey();
					VirtualPlatformNode vn = new VirtualPlatformNode(v);
					root.add(vn);
					if(virtual.getValue()!=null)
					{
						for(String pl: virtual.getValue())
						{
							PlatformNode pn = new PlatformNode(pl);
							vn.add(pn);
						}
					}
				}
				
				for(int i=0; i<tree.getRowCount(); i++)
				{
					tree.expandRow(i);
				}
				
				ret.setResult(null);
			}
		});
	
		return ret;
	}
	
	/**
	 * 
	 */
	protected IFuture<Void> createPlatformsModel()
	{
		final Future<Void> ret = new Future<Void>();
		
		secser.getVirtuals().addResultListener(new SwingExceptionDelegationResultListener<Map<String,Set<String>>, Void>(ret)
		{
			public void customResultAvailable(Map<String,Set<String>> virtuals) 
			{
				IdTreeNode<String> root = (IdTreeNode<String>)model.getRoot();
				root.removeAllChildren();
				vpcnt = 0;
				pcnt = 0;
			
				Map<String, Set<String>> plats = new HashMap<String, Set<String>>();
				
				for(Map.Entry<String, Set<String>> virtual: virtuals.entrySet())
				{
					String v = virtual.getKey();
					if(virtual.getValue()!=null)
					{
						for(String pl: virtual.getValue())
						{
							Set<String> vals = plats.get(pl);
							if(vals==null)
							{
								vals = new HashSet<String>();
								plats.put(pl, vals);
							}
							vals.add(v);
						}
					}
				}
				
				for(Map.Entry<String, Set<String>> plat: plats.entrySet())
				{
					String p = plat.getKey();
					PlatformNode pn = new PlatformNode(p);
					root.add(pn);
					if(plat.getValue()!=null)
					{
						for(String v: plat.getValue())
						{
							VirtualPlatformNode vn = new VirtualPlatformNode(v);
							pn.add(vn);
						}
					}
				}
				
				for(int i=0; i<tree.getRowCount(); i++)
				{
					tree.expandRow(i);
				}
				
				ret.setResult(null);
			}
		});
	
		return ret;
	}
	
	/**
	 * 
	 */
	class RootNode extends IdTreeNode<String>
	{	
		/**
		 *  Create a new RootNode.
		 */
		public RootNode()
		{
			super("rootnode", "rootnode", model, Boolean.FALSE, null, null, null);
		}
	}
	
	protected int vpcnt = 0;
	/**
	 * 
	 */
	class VirtualPlatformNode extends IdTreeNode<String>
	{
		/**
		 *  Create a new VirtualPlatformNode.
		 */
		public VirtualPlatformNode(String name)
		{
			super(name+"_"+vpcnt++, name, model, null, null, null, null);
		}
	}
	
	protected int pcnt = 0;
	/**
	 * 
	 */
	class PlatformNode extends IdTreeNode<String>
	{
		/**
		 *  Create a new PlatformNode.
		 */
		public PlatformNode(String name)
		{
			super(name+"_"+pcnt++, name, model, null, null, null, null);
		}
	}
	
	/**
	 * 
	 */
	class AddVirtualPlatformAction extends AbstractAction
	{
		protected IdTreeNode<String> node;
		
		public AddVirtualPlatformAction(IdTreeNode<String> node)
		{
			super("Add virtual platform");
			this.node = node;
		}
		
		public void actionPerformed(ActionEvent e)
		{
			PropertiesPanel pp = new PropertiesPanel();
			final JTextField tfname = pp.createTextField("Virtual name: ", null, true);
			
			int res	= JOptionPane.showOptionDialog(VirtualNamesPanel.this, pp, "Virtual Platform Name", JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, new Object[]{"OK", "Cancel"}, "OK");
			if(JOptionPane.YES_OPTION==res)
			{
				final String v = tfname.getText();
				String p = null;
				if(!(node instanceof RootNode))
				{
					p = node.getName();
				}
				
//				System.out.println("add: "+v+" "+p);
				
				secser.addVirtual(v, p).addResultListener(new SwingResultListener<Void>(new IResultListener<Void>()
				{
					public void resultAvailable(Void result) 
					{
						VirtualPlatformNode cn = new VirtualPlatformNode(v);
						node.add(cn);
						tree.setSelectionPath(new TreePath(cn.getPath()));
						notifyListeners(new ChangeEvent<String>(null));
//						TreePath tp = new TreePath(cn.getPath());
//						System.out.println("tp: "+tp+" "+Thread.currentThread());
//						tree.expandPath(new TreePath(cn.getPath()));
					}
					
					public void exceptionOccurred(Exception exception)
					{
					}
				}));
			}
		}
	};
	
	/**
	 * 
	 */
	class SelectPlatformAction extends AbstractAction
	{
		protected IdTreeNode<String> node;
		
		public SelectPlatformAction(IdTreeNode<String> node)
		{
			super("Select platform");
			this.node = node;
		}
		
		public void actionPerformed(ActionEvent e)
		{
			final PlatformSelectorDialog csd = new PlatformSelectorDialog(SGUI.getWindowParent(VirtualNamesPanel.this), ea, cmshandler, null, new ComponentIconCache(ea));
			
			IComponentIdentifier cid = csd.selectAgent(null);
			
			if(cid!=null)
			{
				final String name = cid.getPlatformPrefix();
				if(!(node instanceof RootNode))
				{
					secser.addVirtual(node.getName(), name).addResultListener(new SwingResultListener<Void>(new IResultListener<Void>()
					{
						public void resultAvailable(Void result) 
						{
							PlatformNode cn = new PlatformNode(name);
							node.add(cn);
							tree.setSelectionPath(new TreePath(cn.getPath()));
							notifyListeners(new ChangeEvent<String>(null));
						}
						
						public void exceptionOccurred(Exception exception)
						{
						}
					}));
				}
				else
				{
					PlatformNode cn = new PlatformNode(name);
					node.add(cn);
					tree.setSelectionPath(new TreePath(cn.getPath()));
				}
			}
		}
	};
	
	/**
	 * 
	 */
	class AddPlatformAction extends AbstractAction
	{
		protected IdTreeNode<String> node;
		
		public AddPlatformAction(IdTreeNode<String> node)
		{
			super("Add platform");
			this.node = node;
		}
		
		public void actionPerformed(ActionEvent e)
		{
			PropertiesPanel pp = new PropertiesPanel();
			final JTextField tfname = pp.createTextField("Platform name: ", null, true);			
			int res	= JOptionPane.showOptionDialog(VirtualNamesPanel.this, pp, "Platform Name", JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, new Object[]{"OK", "Cancel"}, "OK");
			if(JOptionPane.YES_OPTION==res)
			{
				final String name = tfname.getText();
				
				if(!(node instanceof RootNode))
				{
					secser.addVirtual(node.getName(), name).addResultListener(new SwingResultListener<Void>(new IResultListener<Void>()
					{
						public void resultAvailable(Void result) 
						{
							PlatformNode cn = new PlatformNode(name);
							node.add(cn);
							tree.setSelectionPath(new TreePath(cn.getPath()));
							notifyListeners(new ChangeEvent<String>(null));
	//						TreePath tp = new TreePath(cn.getPath());
	//						System.out.println("tp: "+tp+" "+Thread.currentThread());
	//						tree.expandPath(new TreePath(cn.getPath()));
						}
						
						public void exceptionOccurred(Exception exception)
						{
						}
					}));
				}
				else
				{
					PlatformNode cn = new PlatformNode(name);
					node.add(cn);
					tree.setSelectionPath(new TreePath(cn.getPath()));
				}
			}
		}
	};
	
	/**
	 * 
	 */
	class RemoveAction extends AbstractAction
	{
		protected IdTreeNode<String> node;
		
		public RemoveAction(IdTreeNode<String> node)
		{
			super("Remove platform");
			this.node = node;
		}
		
		public void actionPerformed(ActionEvent e)
		{
			String v = null;
			String p = null;
			
			if(node instanceof PlatformNode)
			{
				p = node.getName();
			}
			else if(node instanceof VirtualPlatformNode)
			{
				v = node.getName();
			}
			
			IdTreeNode<String> pa = (IdTreeNode<String>)node.getParent();
			if(pa instanceof VirtualPlatformNode)
			{
				v = pa.getName();
			}
			else if(pa instanceof PlatformNode)
			{
				p = pa.getName();
			}
			
			secser.removeVirtual(v, p).addResultListener(new SwingResultListener<Void>(new IResultListener<Void>()
			{
				public void resultAvailable(Void result) 
				{
					((IdTreeNode)node.getParent()).remove(node);
					notifyListeners(new ChangeEvent<String>(null));
				}
				
				public void exceptionOccurred(Exception exception)
				{
				}
			}));
		}
	}
	
	/**
	 * 
	 */
	class SwitchViewAction extends AbstractAction
	{
		public SwitchViewAction()
		{
			super(platform? "Switch to virtual view": "Switch to platform view");
		}
		
		public void actionPerformed(ActionEvent e)
		{
			platform = !platform;
			setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), platform? "Platform -> Virtual Name": "Virtual Name -> Platform"));

			if(platform)
			{
				createPlatformsModel();
			}
			else
			{
				createVirtualsModel();
			}
		}
	}
	
	/**
	 * 
	 */
	public void addChangeListener(IChangeListener<String> listener)
	{
		listeners.add(listener);
	}
	
	/**
	 * 
	 */
	public void removeChangeListener(IChangeListener<String> listener)
	{
		listeners.remove(listener);
	}
	
	/**
	 * 
	 */
	public void notifyListeners(ChangeEvent<String> e)
	{
		for(IChangeListener<String> lis: listeners)
		{
			lis.changeOccurred(e);
		}
	}
	
//	/**
//	 * 
//	 */
//	public static void main(String[] args)
//	{
//		IdTreeModel<String> model = new IdTreeModel<String>();
//		IdTreeNode<String> root = new IdTreeNode<String>("root", "root", model, null, null, "Root", null);
//		model.setRoot(root);
//		final JTree tree = new JTree(model);
//		
//		IdTreeNode<String> a = new IdTreeNode<String>("a", "a", model, null, null, VIRTUAL_NAME, VIRTUAL_NAME);
//		IdTreeNode<String> b = new IdTreeNode<String>("b", "b", model, null, null, VIRTUAL_NAME, VIRTUAL_NAME);
//		IdTreeNode<String> c = new IdTreeNode<String>("c", "c", model, null, null, VIRTUAL_NAME, VIRTUAL_NAME);
//		root.add(a);
//		root.add(b);
//		root.add(c);
//		
//		JFrame f = new JFrame();
//		JPanel p = new JPanel(new BorderLayout());
//		p.add(new JScrollPane(tree), BorderLayout.CENTER);
//		f.getContentPane().add(p, BorderLayout.CENTER);
//		
//		f.pack();
//		f.setVisible(true);
//		
////		root.remove(a);
//		
//		MouseAdapter ma = new MouseAdapter()
//		{
//			public void mousePressed(MouseEvent e)
//			{
//				popup(e);
//			}
//			
//			public void mouseReleased(MouseEvent e)
//			{
//				popup(e);
//			}
//			
//			protected void	popup(MouseEvent e)
//			{
//				if(e.isPopupTrigger())
//				{
//					JPopupMenu menu = new JPopupMenu("Virtuals menu");
//					
//					TreePath path = tree.getPathForLocation(e.getX(), e.getY());
//					
//					if(path!=null) 
//					{
//					    tree.setSelectionPath(path);
//						
//					    final IdTreeNode<String> node = (IdTreeNode<String>)tree.getLastSelectedPathComponent();
//					    
//					    System.out.println("node: "+node.getId());
//					    
//					    menu.add(new AbstractAction("Remove")
//						{
//							public void actionPerformed(ActionEvent e)
//							{
//								System.out.println("rem");
//								System.out.println("pa: "+node.getParent()+" n: "+node);
//								((IdTreeNode)node.getParent()).remove(node);
//							}
//						});
//						
//						menu.show(tree, e.getX(), e.getY());
//					}
//				}
//			}
//		};
//		
//		tree.addMouseListener(ma);
//	}
	
}
