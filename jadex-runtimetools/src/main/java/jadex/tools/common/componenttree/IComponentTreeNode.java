package jadex.tools.common.componenttree;

import javax.swing.Icon;

/**
 *  Node for the component tree panel.
 */
public interface IComponentTreeNode
{
	/**
	 *  Called when the node is removed or the tree is closed.
	 */
	public void	dispose();
	
	/**
	 *  Get the id used for lookup.
	 */
	public Object	getId();
	
	/**
	 *  Get the parent node.
	 */
	public IComponentTreeNode	getParent();
	
	/**
	 *  Get the child count.
	 */
	public int	getChildCount();
	
	/**
	 *  Get the given child.
	 */
	public IComponentTreeNode	getChild(int index);
	
	/**
	 *  Test if a node is a child.
	 */
	public boolean isChild(IComponentTreeNode node);
	
	/**
	 *  Get the index of a child.
	 */
	public int	getIndexOfChild(IComponentTreeNode child);
	
	/**
	 *  Check if the node is a leaf.
	 */
	public boolean	isLeaf();
	
	/**
	 *  Get the icon for a node.
	 */
	public Icon	getIcon();
	
	/**
	 *  Refresh the node.
	 *  @param recurse	Recursively refresh subnodes, if true.
	 */
	public void	refresh(boolean recurse);
	
	/**
	 *  Get the model.
	 */
	public ComponentTreeModel	getModel();
}
