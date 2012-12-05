package jadex.bpmn.editor.gui;

import jadex.bpmn.editor.gui.controllers.SValidation;
import jadex.bpmn.editor.gui.layouts.EventHandlerLayout;
import jadex.bpmn.editor.gui.layouts.LaneLayout;
import jadex.bpmn.editor.model.visual.VActivity;
import jadex.bpmn.editor.model.visual.VElement;
import jadex.bpmn.editor.model.visual.VExternalSubProcess;
import jadex.bpmn.editor.model.visual.VLane;
import jadex.bpmn.editor.model.visual.VPool;
import jadex.bpmn.editor.model.visual.VSequenceEdge;
import jadex.bpmn.editor.model.visual.VSubProcess;
import jadex.bpmn.model.MActivity;
import jadex.bpmn.model.MBpmnModel;

import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.mxStackLayout;
import com.mxgraph.model.mxICell;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxLayoutManager;
import com.mxgraph.view.mxStylesheet;

/**
 *  Graph for BPMN models.
 *
 */
public class BpmnGraph extends mxGraph
{
	/** The model container. */
	protected ModelContainer modelcontainer;
	
	/**
	 *  Creates the graph.
	 */
	public BpmnGraph(ModelContainer container, mxStylesheet sheet)
	{
		this.modelcontainer = container;
		setAllowDanglingEdges(false);
		setAllowLoops(true);
		setVertexLabelsMovable(false);
		setCellsCloneable(false);
		setAllowNegativeCoordinates(false);
		setGridEnabled(true);
		setGridSize(10);
		
		/*getModel().addListener(mxEvent.EXECUTE, access.getValueChangeController());
		getSelectionModel().addListener(mxEvent.CHANGE, access.getSelectionController());
		
		addListener(mxEvent.CONNECT_CELL, access.getEdgeReconnectController());
		
		addListener(mxEvent.CELLS_FOLDED, access.getFoldController());*/
		
		setStylesheet(sheet);
		
//		final BpmnLayoutManager layoutmanager = 
		new BpmnLayoutManager(this);
	}
	
	/**
	 * Returns true if the given cell is expandable. This implementation
	 * returns true if the cell has at least one child and its style
	 * does not specify mxConstants.STYLE_FOLDABLE to be 0.
	 *
	 * @param cell <mxCell> whose expandable state should be returned.
	 * @return Returns true if the given cell is expandable.
	 */
	public boolean isCellFoldable(Object cell, boolean collapse)
	{
		boolean ret = super.isCellFoldable(cell, collapse);
		if (cell instanceof VExternalSubProcess || cell instanceof VSubProcess)
		{
			ret = true;
		}
		return ret;
	}
	
	/*protected mxGraphView createGraphView()
	{
		return new BpmnGraphView(this);
	}*/
	
	/**
	 *  Gets the model container.
	 *
	 *  @return The model container.
	 */
	public ModelContainer getModelContainer()
	{
		return modelcontainer;
	}

	/**
	 * Returns true if the given cell is a valid drop target for the specified
	 * cells. This returns true if the cell is a swimlane, has children and is
	 * not collapsed, or if splitEnabled is true and isSplitTarget returns
	 * true for the given arguments
	 * 
	 * @param cell Object that represents the possible drop target.
	 * @param cells Objects that are going to be dropped.
	 * @return Returns true if the cell is a valid drop target for the given
	 * cells.
	 */
	public boolean isValidDropTarget(Object cell, Object[] cells)
	{
		boolean ret = false;
		
		/* Special case for internal subprocesses,
		 * they are a potential parent but not for themselves. */
		if (cell instanceof VSubProcess && cells != null)
		{
			
			boolean nomatch = true;
			for (int i = 0; i < cells.length; ++i)
			{
				if (cells[i].equals(cell))
				{
					nomatch = false;
					break;
				}
			}
			
			if (nomatch)
			{
				ret = (isSplitEnabled() && isSplitTarget(cell, cells)) ||
						(!model.isEdge(cell) && !isCellCollapsed(cell));
			}
		}
		else if (cell instanceof VActivity &&
				 ((VActivity) cell).getBpmnElement() != null &&
				 MBpmnModel.TASK.equals(((MActivity) ((VActivity) cell).getBpmnElement()).getActivityType()))
		{
			/* Tasks are never drop targets, even if they contain children, they will be all event handlers. */
			ret = false;
		}
		else
		{
			ret = cell != null
					&& ((isSplitEnabled() && isSplitTarget(cell, cells)) ||
							(!model.isEdge(cell) && (isSwimlane(cell) ||
							(model.getChildCount(cell) > 0 && !isCellCollapsed(cell)))));
		}
		
		return ret;
	}

	/**
	 *  Refreshes the view for a cell.
	 *  
	 *  @param cell The cell.
	 */
	public void refreshCellView(mxICell cell)
	{
		getView().clear(cell, true, false);
		getView().invalidate(cell);
		Object[] selcells = getSelectionModel().getCells();
		getSelectionModel().removeCells(selcells);
		getView().validate();
		setSelectionCells(selcells);
	}
	
	/**
	 * Returns the validation error message to be displayed when inserting or
	 * changing an edges' connectivity. A return value of null means the edge
	 * is valid, a return value of '' means it's not valid, but do not display
	 * an error message. Any other (non-empty) string returned from this method
	 * is displayed as an error message when trying to connect an edge to a
	 * source and target. This implementation uses the multiplicities, as
	 * well as multigraph and allowDanglingEdges to generate validation
	 * errors.
	 * 
	 * @param edge Cell that represents the edge to validate.
	 * @param source Cell that represents the source terminal.
	 * @param target Cell that represents the target terminal.
	 */
	public String getEdgeValidationError(Object edge, Object source,
			Object target)
	{
		String error = super.getEdgeValidationError(edge, source, target);
		if (error == null)
		{
			if (edge instanceof VSequenceEdge)
			{
				error = SValidation.getSequenceEdgeValidationError(source, target);
			}
		}
		
		return error;
	}
	
	/**
	 *  The layout manager.
	 *
	 */
	protected static class BpmnLayoutManager extends mxLayoutManager
	{
		/** Layout for lanes. */
		protected mxStackLayout lanelayout;
		
		/** Layout for event handlers. */
		protected EventHandlerLayout evtlayout; 
		
		/**
		 *  Creates new layout manager.
		 *  @param graph The graph.
		 */
		public BpmnLayoutManager(mxGraph graph)
		{
			super(graph);
			this.lanelayout = new LaneLayout(graph);
			this.evtlayout = new EventHandlerLayout(graph);
		}
		
		/**
		 *  Gets the layout.
		 */
		protected mxIGraphLayout getLayout(Object parent)
		{
			if (parent instanceof VPool &&
				graph.getModel().getChildCount(parent) > 0 &&
				graph.getModel().getChildAt(parent, 0) instanceof VLane)
			{
				return lanelayout;
			}
			
			if (parent instanceof VElement &&
				((VElement) parent).getBpmnElement() instanceof MActivity)
			{
				MActivity mparent = (MActivity) ((VElement) parent).getBpmnElement();
				if (MBpmnModel.TASK.equals(mparent.getActivityType()) ||
					MBpmnModel.SUBPROCESS.equals(mparent.getActivityType()))
				{
					return evtlayout;
				}
			}
			
			return null;
		}
	}
}
