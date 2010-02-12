/**
 * 
 */
package jadex.tools.bpmn.editor.properties;

import jadex.tools.bpmn.diagram.Messages;
import jadex.tools.bpmn.editor.JadexBpmnEditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.gmf.runtime.common.core.command.CommandResult;
import org.eclipse.gmf.runtime.draw2d.ui.figures.FigureUtilities;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.stp.bpmn.diagram.part.BpmnDiagramEditorPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * Property section Tab to enable Jadex specific properties
 * 
 * @author Claas Altschaffel
 */
public abstract class AbstractMultiColumnTablePropertySection extends AbstractJadexPropertySection
{
	
	
	
	// ---- attributes ----
	
	/** The viewer/editor for parameter */ 
	private TableViewer tableViewer;
	
	/** The table add element button */
	private Button addButton;
	
	/** The table delete element button */
	private Button delButton;
	
	/** The label string for the tableViewer */
	private String tableViewerLabel;
	
	/** the column names */
	private String[] columnNames;
	
	/** the column weights */
	private int[] columWeights;
	
	private String[] defaultListElementAttributeValues;
	
	private int uniqueColumnIndex;
	
	private HashMap<EModelElement, HashSet<String>> uniqueColumnValuesMap;

	// ---- constructor ----
	
	/**
	 * Protected constructor for subclasses
	 * @param containerEAnnotationName @see {@link AbstractJadexPropertySection}
	 * @param annotationDetailName @see {@link AbstractJadexPropertySection}
	 * @param tableLabel the name of table
	 * @param columns the column of table
	 * @param columnsWeight the weight of columns
	 * @param defaultListElementAttributeValues default columnValues for new elements, may be <code>null</code>
	 */
	protected AbstractMultiColumnTablePropertySection(String containerEAnnotationName, String annotationDetailName, String tableLabel, String[] columns, int[] columnsWeight, String[] defaultListElementAttributeValues, int uniqueColumnIndex)
	{
		super(containerEAnnotationName, annotationDetailName);

		assert (columns != null && columnsWeight != null);
		assert (columns.length == columnsWeight.length);
		assert (uniqueColumnIndex != -1 && uniqueColumnIndex < columns.length);
		
		this.tableViewerLabel = tableLabel;
		this.columWeights = columnsWeight;
		this.columnNames = columns;
		
		this.uniqueColumnValuesMap = new HashMap<EModelElement, HashSet<String>>();
		
		this.uniqueColumnIndex = uniqueColumnIndex;

		if (defaultListElementAttributeValues != null)
		{
			assert (columns.length == defaultListElementAttributeValues.length);
			this.defaultListElementAttributeValues = defaultListElementAttributeValues;
		}
		else
		{
			this.defaultListElementAttributeValues = columns;
		}
	}


	// ---- methods ----

	/**
	 * Creates the UI of the section.
	 */
	@Override
	public void createControls(Composite parent,
			TabbedPropertySheetPage aTabbedPropertySheetPage)
	{
		super.createControls(parent, aTabbedPropertySheetPage);
		
		Group sectionGroup = getWidgetFactory().createGroup(sectionComposite, tableViewerLabel);
		sectionGroup.setLayout(new FillLayout(SWT.VERTICAL));
		createTableViewer(sectionGroup);
	}

	
	/**
	 * Manages the input.
	 */
	@Override
	public void setInput(IWorkbenchPart part, ISelection selection)
	{
		super.setInput(part, selection);

		if (modelElement != null)
		{
			tableViewer.setInput(modelElement);
			addButton.setEnabled(true);
			delButton.setEnabled(true);

			getUniqueColumnValueCash(modelElement);
			
			return;
		}

		// fall through
		tableViewer.setInput(null);
		addButton.setEnabled(false);
		delButton.setEnabled(false);

	}
	
	private HashSet<String> getUniqueColumnValueCash(EModelElement element)
	{
		if (uniqueColumnValuesMap.containsKey(modelElement))
		{
			return uniqueColumnValuesMap.get(modelElement);
		}
		else
		{
			HashSet<String> newSet = new HashSet<String>();
			uniqueColumnValuesMap.put(modelElement, newSet);
			return newSet;
		}
	}
	

	/**
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#refresh()
	 */
	@Override
	public void refresh()
	{
		super.refresh();
		if (tableViewer != null && modelElement != null)				
		{
			tableViewer.refresh();
		}
		
	}

	
	// ---- control creation methods ----

	/**
	 * Creates the controls of the Parameter page section. Creates a table
	 * containing all Parameter of the selected {@link ParameterizedVertex}.
	 * 
	 * We use our own layout
	 * 
	 * @generated NOT
	 */
	protected TableViewer createTableViewer(Composite parent)
	{
		Composite tableComposite = getWidgetFactory().createComposite(parent/*, SWT.BORDER*/);

		// The layout of the table composite
		GridLayout layout = new GridLayout(3, false);
		tableComposite.setLayout(layout);
		
		GridData tableLayoutData = new GridData(GridData.FILL_BOTH);
		tableLayoutData.grabExcessHorizontalSpace = true;
		tableLayoutData.grabExcessVerticalSpace = true;
		tableLayoutData.minimumHeight = 150;
		tableLayoutData.heightHint = 150;
		tableLayoutData.horizontalSpan = 3;

		// create the table
		TableViewer viewer = createTable(tableComposite, tableLayoutData);

		// create cell modifier command
		setupTableNavigation(viewer);

		// create buttons
		createButtons(tableComposite);
		
		return tableViewer = viewer;

	}
	
	/**
	 * Create the parameter edit table
	 * @param parent
	 * 
	 */
	protected TableViewer createTable(Composite parent, GridData tableLayoutData)
	{
		//String[] columns = columnNames;
		//int[] weight = columWeights;

		// the displayed table
		TableViewer viewer = new TableViewer(getWidgetFactory().createTable(parent,
				SWT.FULL_SELECTION | SWT.BORDER));

		viewer.getTable().setLinesVisible(true);
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLayoutData(tableLayoutData);

		createColumns(viewer);
		
		Font tableFont = viewer.getTable().getFont();
		TableLayout tableLayout = new TableLayout();
		for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++)
		{
			tableLayout.addColumnData(new ColumnWeightData(columWeights[columnIndex],
					FigureUtilities.getTextWidth(columnNames[columnIndex], tableFont), true));
		}
		viewer.getTable().setLayout(tableLayout);

		viewer.setContentProvider(new MultiColumnTableContentProvider());
		// FIX ME: use column specific label provider (see above)
		//viewer.setLabelProvider(new MultiColumnTableLabelProvider());
		viewer.setColumnProperties(columnNames);

		return viewer;
	}
	
	protected void createColumns(TableViewer viewer)
	{
		for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++)
		{
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.LEFT);
			column.getColumn().setText(columnNames[columnIndex]);

			column.setEditingSupport(new MultiColumnTableEditingSupport(viewer, columnIndex));
			column.setLabelProvider(new MultiColumnTableLabelProvider(columnIndex));

//			column.setLabelProvider(new ColumnLabelProvider()
//			{
//				public String getText(Object element)
//				{
//					return ((Person) element).email;
//				}
//			});
		}

	}
	
	/**
	 * Create the cell modifier command to update {@link EAnnotation}
	 */
	private void setupTableNavigation(final TableViewer viewer)
	{
		
//		CellNavigationStrategy naviStrat = new CellNavigationStrategy();
//
//		// from Snippet059CellNavigationIn33 
//		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(
//				viewer, new FocusCellOwnerDrawHighlighter(viewer));
//		try
//		{
//			Field f = focusCellManager.getClass().getSuperclass().getDeclaredField("navigationStrategy");
//			f.setAccessible(true);
//			f.set(focusCellManager, naviStrat);
//		}
//		catch (SecurityException e)
//		{
//			e.printStackTrace();
//		}
//		catch (NoSuchFieldException e)
//		{
//			e.printStackTrace();
//		}
//		catch (IllegalArgumentException e)
//		{
//			e.printStackTrace();
//		}
//		catch (IllegalAccessException e)
//		{
//			e.printStackTrace();
//		}

		
		ColumnViewerEditorActivationStrategy editorActivationSupport = new ColumnViewerEditorActivationStrategy(
				viewer)
		{
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event)
			{
				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION
						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
						;
			}
		};

		TableViewerEditor.create(viewer,/* focusCellManager,*/ editorActivationSupport,
				TableViewerEditor.TABBING_HORIZONTAL
						| TableViewerEditor.KEYBOARD_ACTIVATION
						| TableViewerEditor.KEEP_EDITOR_ON_DOUBLE_CLICK
						//| TableViewerEditor.TABBING_CYCLE_IN_ROW
						| TableViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
						);
		
//		viewer.getColumnViewerEditor().addEditorActivationListener(
//				new ColumnViewerEditorActivationListener() {
//
//					public void afterEditorActivated(
//							ColumnViewerEditorActivationEvent event) {
//
//					}
//
//					public void afterEditorDeactivated(
//							ColumnViewerEditorDeactivationEvent event) {
//
//					}
//
//					public void beforeEditorActivated(
//							ColumnViewerEditorActivationEvent event) {
//						ViewerCell cell = (ViewerCell) event.getSource();
//						viewer.getTable().showColumn(
//								viewer.getTable().getColumn(cell.getColumnIndex()));
//					}
//
//					public void beforeEditorDeactivated(
//							ColumnViewerEditorDeactivationEvent event) {
//
//					}
//
//				});

	}
	
	/**
	 * Create the Add and Delete button
	 * @param parent
	 * @generated NOT
	 */
	private void createButtons(Composite parent)
	{

		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gridData.widthHint = 80;

		// Create and configure the "Add" button
		Button add = new Button(parent, SWT.PUSH | SWT.CENTER);
		add.setText(Messages.JadexCommonPropertySection_ButtonAdd_Label);
		add.setLayoutData(gridData);
		add.addSelectionListener(new SelectionAdapter()
		{
			/** 
			 * Add a ContextElement to the Context and refresh the view
			 * @generated NOT 
			 */
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				// modify the EModelElement annotation
				ModifyJadexEAnnotationCommand command = new ModifyJadexEAnnotationCommand(
						modelElement,
						"Add EModelElement parameter element")
				{
					@Override
					protected CommandResult doExecuteWithResult(
							IProgressMonitor monitor, IAdaptable info)
							throws ExecutionException
					{
						AbstractMultiColumnTableRow newRow = new AbstractMultiColumnTableRow(
								defaultListElementAttributeValues, uniqueColumnIndex);

						HashSet<String> uniqueValueCash = getUniqueColumnValueCash(modelElement);
						synchronized (uniqueValueCash)
						{
							List<AbstractMultiColumnTableRow> tableRows = getTableRowList();
							String uniqueValue = createUniqueRowValue(newRow, tableRows);
							newRow.getColumnValues()[uniqueColumnIndex] = uniqueValue;
							addUniqueRowValue(uniqueValue);
							tableRows.add(newRow);
							updateTableRowList(tableRows);
						}

						return CommandResult.newOKCommandResult(newRow);
					}
				};
				try
				{
					command.execute(null, null);
					
					refresh();
					refreshSelectedEditPart();
				}
				catch (ExecutionException ex)
				{
					BpmnDiagramEditorPlugin.getInstance().getLog().log(
							new Status(IStatus.ERROR,
									JadexBpmnEditor.ID, IStatus.ERROR,
									ex.getMessage(), ex));
				}
			}
		});
		addButton = add;

		// Create and configure the "Delete" button
		Button delete = new Button(parent, SWT.PUSH | SWT.CENTER);
		delete.setText(Messages.JadexCommonPropertySection_ButtonDelete_Label);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gridData.widthHint = 80;
		delete.setLayoutData(gridData);
		delete.addSelectionListener(new SelectionAdapter()
		{
			/** 
			 * Remove selected ContextElement from the Context and refresh the view
			 * @generated NOT 
			 */
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				// modify the EModelElement annotation
				ModifyJadexEAnnotationCommand command = new ModifyJadexEAnnotationCommand(
						modelElement,
						"Delete EModelElement parameter element")
				{
					@Override
					protected CommandResult doExecuteWithResult(
							IProgressMonitor monitor, IAdaptable info)
							throws ExecutionException
					{
						HashSet<String> uniqueValueCash = getUniqueColumnValueCash(modelElement);
						synchronized (uniqueValueCash)
						{
							AbstractMultiColumnTableRow rowToRemove = (AbstractMultiColumnTableRow) ((IStructuredSelection) tableViewer
									.getSelection()).getFirstElement();
							
							List<AbstractMultiColumnTableRow> tableRowList = getTableRowList();
							//modelElementUniqueColumnValueCash.remove(rowToRemove.columnValues[uniqueColumnIndex]);
							uniqueValueCash.remove(rowToRemove.getColumnValues()[uniqueColumnIndex]);
							tableRowList.remove(rowToRemove);
							updateTableRowList(tableRowList);
						}

						return CommandResult.newOKCommandResult(null);
					}
				};
				try
				{
					command.execute(null, null);
					
					refresh();
					refreshSelectedEditPart();
				}
				catch (ExecutionException ex)
				{
					BpmnDiagramEditorPlugin.getInstance().getLog().log(
							new Status(IStatus.ERROR,
									JadexBpmnEditor.ID, IStatus.ERROR,
									ex.getMessage(), ex));
				}
			}
		});
		
		delButton = delete;
	}
	
	// ---- converter and help methods ----

	
	private String createUniqueRowValue(AbstractMultiColumnTableRow row, List<AbstractMultiColumnTableRow> table)
	{
		assert (row != null && table != null);
		
		HashSet<String> uniqueValueCash = getUniqueColumnValueCash(modelElement);
		synchronized (uniqueValueCash)
		{
			String uniqueColumnValue = row.getColumnValues()[uniqueColumnIndex];

			int counter = 1;
			String uniqueValueToUse = uniqueColumnValue;
			while (uniqueValueCash.contains(uniqueValueToUse))
			{
				uniqueValueToUse = uniqueColumnValue + counter;
				counter++;
			}

			return uniqueValueToUse;
		}

	}
	
	private boolean addUniqueRowValue(String uniqueValue)
	{
		HashSet<String> uniqueValueCash = getUniqueColumnValueCash(modelElement);
		synchronized (uniqueValueCash)
		{
			return uniqueValueCash.add(uniqueValue);
		}
	}
	
	private boolean removeUniqueRowValue(String uniqueValue)
	{
		HashSet<String> uniqueValueCash = getUniqueColumnValueCash(modelElement);
		synchronized (uniqueValueCash)
		{
			return uniqueValueCash.remove(uniqueValue);
		}
	}
	
	/**
	 * Retrieve the EAnnotation from the modelElement and converts it to a {@link AbstractMultiColumnTableRow} list
	 * @param act
	 * @return
	 */
	private List<AbstractMultiColumnTableRow> getTableRowList()
	{
		EAnnotation ea = modelElement.getEAnnotation(containerEAnnotationName);
		if (ea != null)
		{
			String value = (String) ea.getDetails().get(annotationDetailName);
			if (value != null)
			{
				List<AbstractMultiColumnTableRow> rowList = convertMultiColumnTableString(value, columnNames.length, uniqueColumnIndex);
				for (AbstractMultiColumnTableRow abstractMultiColumnTableRow : rowList)
				{
					addUniqueRowValue(abstractMultiColumnTableRow.getColumnValueAt(uniqueColumnIndex));
				}
				return rowList;
			}
		}

		return new ArrayList<AbstractMultiColumnTableRow>(0);
	}
	
	/**
	 * Updates the EAnnotation for the modelElement task parameter list
	 * @param params
	 */
	private void updateTableRowList(List<AbstractMultiColumnTableRow> params)
	{
		updateJadexEAnnotation(annotationDetailName, convertMultiColumnRowList(params));
	}
	
	
	/**
	 * Convert a string representation of a AbstractMultiColumnTableRow list into a
	 * AbstractMultiColumnTableRow list
	 * 
	 * @param stringToConvert
	 * @param columnCount
	 * @param uniqueColumn
	 * @return
	 */
	public static List<AbstractMultiColumnTableRow> convertMultiColumnTableString(
			String stringToConvert, int columnCount, int uniqueColumn)
	{
		StringTokenizer listTokens = new StringTokenizer(stringToConvert, JadexCommonPropertySection.LIST_ELEMENT_DELIMITER);
		List<AbstractMultiColumnTableRow> tableRowList = new ArrayList<AbstractMultiColumnTableRow>(listTokens.countTokens());
		while (listTokens.hasMoreTokens())
		{
			String parameterElement = listTokens.nextToken();
			StringTokenizer parameterTokens = new StringTokenizer(
					parameterElement,
					JadexCommonPropertySection.LIST_ELEMENT_ATTRIBUTE_DELIMITER,
					true);

			// number of columns is the index that will be used.
			// initialize array with empty strings because we 
			// don't check the values
			String[] attributes = new String[columnCount];
			for (int index = 0; index < attributes.length; index++)
			{
				attributes[index] = attributes[index] != null ? attributes[index] : "";
			}
			
			int attributeIndexCounter = 0;	
			String lastToken = null;

			while (parameterTokens.hasMoreTokens())
			{
				String attributeToken = parameterTokens.nextToken();

				if (!attributeToken.equals(LIST_ELEMENT_ATTRIBUTE_DELIMITER))
				{
					attributes[attributeIndexCounter] = attributeToken;
					attributeIndexCounter++;
				}
				// we found a delimiter
				else
				{
					if (	// we found a delimiter at the first position
							lastToken == null 
							// we found a delimiter at the last position, 
							|| !parameterTokens.hasMoreTokens()
							// we found two delimiter without any content between
							|| attributeToken.equals(lastToken))
					{
						attributes[attributeIndexCounter] = "";
						attributeIndexCounter++;
					}
					
				}

				// remember last token
				lastToken = attributeToken;

			} // end while paramTokens

			AbstractMultiColumnTableRow newRow = new AbstractMultiColumnTableRow(attributes, uniqueColumn);
			//addUniqueRowValue(newRow.getColumnValueAt(uniqueColumnIndex));
			tableRowList.add(newRow);

		} // end while listTokens
		
		return tableRowList;
	}
	
	/**
	 * Convert a list of AbstractMultiColumnTableRow into a string representation using  
	 * <code>LIST_ELEMENT_DELIMITER</code> from {@link AbstractJadexPropertySection}
	 * as delimiter
	 * 
	 * @param params
	 * @return
	 */
	public static String convertMultiColumnRowList(List<AbstractMultiColumnTableRow> params)
	{
		StringBuffer buffer = new StringBuffer();
		for (AbstractMultiColumnTableRow abstractMultiColumnTableRow : params)
		{
			if (buffer.length() != 0)
			{
				buffer.append(LIST_ELEMENT_DELIMITER);
			}

			buffer.append(convertMultiColumnRowToString(abstractMultiColumnTableRow));
		}
		return buffer.toString();
	}
	
	/**
	 * Convert a row of the table to a String representation using 
	 * <code>LIST_ELEMENT_ATTRIBUTE_DELIMITER</code> from 
	 * {@link AbstractJadexPropertySection} as delimiter
	 * @param row to convert
	 * @return String representation of row
	 */
	public static String convertMultiColumnRowToString(AbstractMultiColumnTableRow row)
	{
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < row.getColumnValues().length; i++)
		{
			buffer.append(row.getColumnValues()[i]);
			buffer.append(LIST_ELEMENT_ATTRIBUTE_DELIMITER);
		}
		// remove last delimiter
		buffer.delete(buffer.length()
				- LIST_ELEMENT_ATTRIBUTE_DELIMITER.length(), buffer.length());
		//System.out.println(buffer.toString());
		return buffer.toString();
	}
	
	// ---- internal used model classes ----
	
	/**
	 * Simple content provider that reflects the ContextElements 
	 * of an Context given as an input. Marked as dynamic / static.
	 */
	protected class MultiColumnTableContentProvider implements IStructuredContentProvider {

		/**
		 * Generate the content for the table.
		 * 
		 * @return Object[] that contains AbstractMultiColumnTableRow objects.
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement)
		{
			if (inputElement instanceof EModelElement)
			{
				EAnnotation ea = ((EModelElement)inputElement).getEAnnotation(containerEAnnotationName);
				inputElement = ea;
			}
			
			if (inputElement instanceof EAnnotation)
			{
				String parameterListString = ((EAnnotation) inputElement).getDetails().get(annotationDetailName);
				if(parameterListString!=null)
				{
					return convertMultiColumnTableString(parameterListString, columnNames.length, uniqueColumnIndex).toArray();
					// add to unique map not need, its only a content provider
				}
					
			}
			
			return new Object[] {};
		}

		/**
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose()
		{
			// nothing to dispose.
		}

		/**
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
		 *      java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
		{
			// no actions taken.
		}

	}
	
	/**
	 * Label provider in charge of rendering the keys and columnValues of the annotation
	 * attached to the object. Currently based on CommonLabelProvider.
	 */
	protected class MultiColumnTableLabelProvider extends ColumnLabelProvider
			implements ITableLabelProvider
	{

		// ---- attributes ----
		
		private int columIndex;
		
		// ---- constructors ----
		
		/**
		 * @param columIndex
		 */
		protected MultiColumnTableLabelProvider()
		{
			super();
			this.columIndex = -1;
		}
		
		/**
		 * @param columIndex
		 */
		protected MultiColumnTableLabelProvider(int columIndex)
		{
			super();
			assert columIndex >= 0 : "column index < 0";
			this.columIndex = columIndex;
		}
		
		// ---- ColumnLabelProvider overrides ----
		
		@Override
		public Image getImage(Object element)
		{
			if (columIndex >= 0)
			{
				return this.getColumnImage(element, columIndex);
			}
			else
			{
				return super.getImage(element);
			}
		}

		@Override
		public String getText(Object element)
		{
			if (columIndex >= 0)
			{
				return this.getColumnText(element, columIndex);
			}
			else
			{
				return super.getText(element);
			}
		}
		
		// ---- ITableLabelProvider implementation ----
		
		/**
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
		 */
		public Image getColumnImage(Object element, int columnIndex)
		{
			return super.getImage(element);
		}

		/**
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
		 */
		public String getColumnText(Object element, int columnIndex)
		{
			if (element instanceof AbstractMultiColumnTableRow)
			{
				return ((AbstractMultiColumnTableRow) element).getColumnValues()[columnIndex];
			}
			return super.getText(element);
		}
		
	}
	
//	/**
//	 * Internal representation of a jadex runtime AbstractMultiColumnTableRow
//	 * 
//	 * @author Claas Altschaffel
//	 */
//	public class AbstractMultiColumnTableRow {
//		
//		// ---- attributes ----
//
//		private String[] columnValues;
//		private int uniqueColumnIndex;
//		
//		// ---- constructors ----
//		
//		/** default constructor */
//		public AbstractMultiColumnTableRow(String[] columnValues, int uniqueColumnIndex)
//		{
//			super();
//			
//			this.uniqueColumnIndex = uniqueColumnIndex;
//			
//			this.columnValues = new String[columnValues.length];
//			for (int i = 0; i < columnValues.length; i++)
//			{
//				assert columnValues[i] != null : "Value for column index '"+i+"' is null";
//				this.columnValues[i] = new String(columnValues[i]);
//			}
//			
//			//this.columnValues = columnValues;
//			
//		}
//		
//		// ---- methods ----
//		
//		/** check if the unique column index is valid and can be used */
//		private boolean useUniqueColumn()
//		{
//			return uniqueColumnIndex >= 0 && uniqueColumnIndex < columnValues.length;
//		}
//
//		// ---- overrides ----
//		
//		/**
//		 * @see java.lang.Object#equals(java.lang.Object)
//		 */
//		@Override
//		public boolean equals(Object obj)
//		{
//			if (!(obj instanceof AbstractMultiColumnTableRow))
//			{
//				return false;
//			}
//			
//			boolean returnValue = true;
//			if (useUniqueColumn())
//			{
//				returnValue = this.columnValues[uniqueColumnIndex].equals(((AbstractMultiColumnTableRow) obj).columnValues[uniqueColumnIndex]);
//			}
//			else
//			{
//				for (int i = 0; returnValue && i < this.columnValues.length; i++)
//				{
//					returnValue =  returnValue &&  this.columnValues[i].equals(((AbstractMultiColumnTableRow) obj).columnValues[i]);
//				}
//			}
//
//			return returnValue;
//		}
//
//		/**
//		 * @see java.lang.Object#hashCode()
//		 */
//		@Override
//		public int hashCode()
//		{
//			int returnHash = 31;
//			
//			if (useUniqueColumn())
//			{
//				returnHash = this.columnValues[uniqueColumnIndex].hashCode();
//			}
//			else
//			{
//				for (int i = 0; i < this.columnValues.length; i++)
//				{
//					returnHash = returnHash + this.columnValues[i].hashCode() * 31;
//				}
//			}
//			return returnHash;
//		}
//
//		/**
//		 * @see java.lang.Object#toString()
//		 */
//		@Override
//		public String toString()
//		{
//			StringBuffer buffer = new StringBuffer();
//			buffer.append("AbstractMultiColumnTableRow(");
//			for (int i = 0; i < this.columnValues.length; i++)
//			{
//				buffer.append("`");
//				buffer.append(columnValues[i]);
//				buffer.append("´" + ", ");
//			}
//			// remove last delimiter
//			buffer.delete(buffer.length()-", ".length(), buffer.length());
//			buffer.append(")");
//			System.out.println(buffer.toString());
//			return buffer.toString();
//		}
//		
//		// ---- getter / setter ----
//		
//		/**
//		 * @return the columnValues
//		 */
//		public String[] getColumnValues()
//		{
//			return columnValues;
//		}
//
//		/**
//		 * @param columnValues the columnValues to set
//		 */
//		public void setColumnValues(String[] values)
//		{
//			this.columnValues = values;
//		}
//
//		/**
//		 * @param columnIndex to get value
//		 * @return the value at index
//		 */
//		public String getColumnValueAt(int columnIndex)
//		{
//			return columnValues[columnIndex];
//		}
//
//		/**
//		 * @param columnIndex to set the value
//		 * @param value the value to set
//		 * 
//		 */
//		public void setColumnValueAt(int columnIndex, String value)
//		{
//			this.columnValues[columnIndex] = value;
//		}
//
//	}
	
	protected class MultiColumnTableEditingSupport extends EditingSupport {
		
		private CellEditor editor;
		private int attributeIndex;

		public MultiColumnTableEditingSupport(TableViewer viewer, int attributeIndex)
		{
			super(viewer);
			this.editor = new TextCellEditor(viewer.getTable());
			this.attributeIndex = attributeIndex;
		}
		
		public MultiColumnTableEditingSupport(TableViewer viewer, int attributeIndex, CellEditor editor)
		{
			super(viewer);
			this.editor = editor;
			this.attributeIndex = attributeIndex;
		}

		/**
		 * Can edit all columns.
		 * @generated NOT
		 */
		public boolean canEdit(Object element)
		{
			if (element instanceof AbstractMultiColumnTableRow)
			{
				return true;
			}
			return false;
		}
		
		protected CellEditor getCellEditor(Object element)
		{
			return editor;
		}

		protected void setValue(Object element, Object value)
		{
			doSetValue(element, value);
			// refresh the table viewer element
			getViewer().update(element, null);
			// refresh the graphical edit part
			refreshSelectedEditPart();
		}

		/**
		 * Get the value for atttributeIndex
		 * @param element to get the value from
		 */
		protected Object getValue(Object element)
		{
			return ((AbstractMultiColumnTableRow) element).getColumnValues()[attributeIndex];
		}
		
		/**
		 * Update the element value with transactional command
		 * @param element to update
		 * @param value to set
		 */
		protected void doSetValue(Object element, Object value)
		{
			
			final AbstractMultiColumnTableRow tableViewerRow = (AbstractMultiColumnTableRow) element;
			final String newValue = value.toString();
			
			// modify the Model
			ModifyJadexEAnnotationCommand command = new ModifyJadexEAnnotationCommand(
					modelElement,
					"Update EModelElement parameter list")
			{
				@Override
				protected CommandResult doExecuteWithResult(
						IProgressMonitor monitor,
						IAdaptable info)
						throws ExecutionException
				{

					List<AbstractMultiColumnTableRow> tableRowList = getTableRowList();
					AbstractMultiColumnTableRow rowToEdit = (AbstractMultiColumnTableRow) tableRowList.get(tableRowList.indexOf(tableViewerRow));

					
					
					if (attributeIndex == uniqueColumnIndex)
					{
						if (!newValue.equals(rowToEdit.getColumnValues()[attributeIndex]))
						{
							HashSet<String> uniqueValueCash = getUniqueColumnValueCash(modelElement);
							synchronized (uniqueValueCash)
							{
								removeUniqueRowValue(rowToEdit.getColumnValues()[uniqueColumnIndex]);
								
								rowToEdit.getColumnValues()[attributeIndex] = newValue;
								String newUniqueValue = createUniqueRowValue(rowToEdit, tableRowList);
								rowToEdit.getColumnValues()[attributeIndex] = newUniqueValue;
								
								addUniqueRowValue(newUniqueValue);
								
								updateTableRowList(tableRowList);

								// update the corresponding table element
								tableViewerRow.getColumnValues()[attributeIndex] = newUniqueValue;
							}

							return CommandResult.newOKCommandResult();
						}
					}
					
					rowToEdit.getColumnValues()[attributeIndex] = newValue;
					updateTableRowList(tableRowList);
					
					// update the corresponding table element
					tableViewerRow.getColumnValues()[attributeIndex] = newValue;
										
					return CommandResult.newOKCommandResult();
				}
			};
			
			try
			{
				command.setReuseParentTransaction(true);
				command.execute(null, null);
				
			}
			catch (ExecutionException e)
			{
				BpmnDiagramEditorPlugin
						.getInstance()
						.getLog()
						.log(
								new Status(
										IStatus.ERROR,
										JadexBpmnEditor.ID,
										IStatus.ERROR, e
												.getMessage(),
										e));
			}

		}
	}

}


