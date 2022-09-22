package edu.isi.karma.controller.command.worksheet;

import edu.isi.karma.controller.command.CommandException;
import edu.isi.karma.controller.command.CommandType;
import edu.isi.karma.controller.command.WorksheetSelectionCommand;
import edu.isi.karma.controller.command.selection.SuperSelection;
import edu.isi.karma.controller.update.AddColumnUpdate;
import edu.isi.karma.controller.update.ErrorUpdate;
import edu.isi.karma.controller.update.UpdateContainer;
import edu.isi.karma.controller.update.WorksheetUpdateFactory;
import edu.isi.karma.rep.*;
import edu.isi.karma.util.Util;
import edu.isi.karma.webserver.KarmaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * extended version of {@link AddValuesCommand} which support Java data structure instead of {@link org.json.JSONArray}
 * which makes it ideal if command is triggered from within karma backend as it doesn't require the data to be
 * serialized in json format. which increase the efficiency of the system.
 *
 * Command to create a new column next to the column where the request was triggered or modify a existing one
 * if column name already exist and add new set of values replacing old one. it requires a map of key-value
 * where key is rowId and value is the type of one of the class extended from {@link CellValue} class.
 *
 * @author Danish danishasghar.cheema@studenti.unitn.it
 */
public class AddCellValueCommand extends WorksheetSelectionCommand {

	public enum JsonKeys {
		updateType, hNodeId, worksheetId
	}

	private final String hNodeId;
	//add column to this table
	private String hTableId;
	//the id of the new column that was created
	//needed for undo
	private String newHNodeId;
	private HNode.HNodeType type;
	private String newColumnName;
	/**
	 * a map each key-value consist of row id and related CellValue to be attached to it.
	 */
	private HashMap<String,CellValue> inputValue;

	private static Logger logger = LoggerFactory
			.getLogger(AddValuesCommand.class);

	protected AddCellValueCommand(HashMap<String, CellValue> inputValue, String id, String model, String worksheetId,
								  String hTableId, String hNodeId, String newColumnName,
								  HNode.HNodeType transformation, String selectionId) {
		super(id, model, worksheetId, selectionId);
		this.inputValue = inputValue;
		this.hNodeId = hNodeId;
		this.hTableId = hTableId;
		this.newColumnName = newColumnName;
		this.type = transformation;
		addTag(CommandTag.Transformation);
	}

	@Override
	public String getCommandName() {
		return AddValuesCommand.class.getSimpleName();
	}

	@Override
	public String getTitle() {
		return "Add Values Command";
	}

	@Override
	public String getDescription() {
		return "";
	}

	@Override
	public CommandType getCommandType() {
		return CommandType.undoable;
	}

	@Override
	public UpdateContainer doIt(Workspace workspace) throws CommandException {
		Worksheet worksheet = workspace.getWorksheet(worksheetId);
		inputColumns.clear();
		outputColumns.clear();
		HNode ndid = null;
		try{
			if (inputValue != null){
				ndid = addColumn(workspace, worksheet);
			}
			WorksheetUpdateFactory.detectSelectionStatusChange(worksheetId, workspace, this);
			UpdateContainer c =  new UpdateContainer(new AddColumnUpdate(newHNodeId, worksheetId));
			c.append(WorksheetUpdateFactory.createRegenerateWorksheetUpdates(worksheetId,
					getSuperSelection(worksheet),
					workspace.getContextId()));
			if (ndid == null) {
				System.err.println("error: ndid");
			}
			c.append(computeAlignmentAndSemanticTypesAndCreateUpdates(workspace));
			return c;
		} catch (Exception e) {
			logger.error("Error in AddColumnCommand" + e.toString());
			Util.logException(logger, e);
			e.printStackTrace();
			return new UpdateContainer(new ErrorUpdate(e.getMessage()));
		}
	}

	@Override
	public UpdateContainer undoIt(Workspace workspace) {
		UpdateContainer c = new UpdateContainer();
		Worksheet worksheet = workspace.getWorksheet(worksheetId);

		HTable currentTable = workspace.getFactory().getHTable(hTableId);
		HNode ndid = workspace.getFactory().getHNode(newHNodeId);
		ndid.removeNestedTable();
		//remove the new column
		currentTable.removeHNode(newHNodeId, worksheet);
		c.append(WorksheetUpdateFactory.createRegenerateWorksheetUpdates(worksheetId, getSuperSelection(worksheet), workspace.getContextId()));
		c.append(computeAlignmentAndSemanticTypesAndCreateUpdates(workspace));
		return c;
	}


	public String getNewHNodeId() {
		return newHNodeId;
	}

	private HNode addColumn(Workspace workspace, Worksheet worksheet) throws KarmaException {
		if(hTableId==null || hTableId.isEmpty()){
			//get table id based on the hNodeId
			if(hNodeId==null)
				hTableId = worksheet.getHeaders().getId();
			else
				hTableId = workspace.getFactory().getHNode(hNodeId).getHTableId();
		}
		HTable hTable = workspace.getFactory().getHTable(hTableId);
		if(hTable == null)
		{
			logger.error("No HTable for id "+ hTableId);
			throw new KarmaException("No HTable for id "+ hTableId );
		}

		//add new column to this table
		//add column after the column with hNodeId
		HNode ndid;
		if (newColumnName != null && !newColumnName.trim().isEmpty()) {
			if (hTable.getHNodeFromColumnName(newColumnName) != null) {
				ndid = hTable.getHNodeFromColumnName(newColumnName);
				outputColumns.add(ndid.getId());
			}
			else {
				ndid = hTable.addNewHNodeAfter(hNodeId, type, workspace.getFactory(), newColumnName, worksheet,true);
				outputColumns.add(ndid.getId());
//				isNewNode = true;
			}
		}
		else {
			ndid = hTable.addNewHNodeAfter(hNodeId, type, workspace.getFactory(), hTable.getNewColumnName("default"), worksheet,true);
			outputColumns.add(ndid.getId());
//			isNewNode = true;
		}
		newHNodeId = ndid.getId();
		//add as first column in the table if hNodeId is null
		//HNode ndid = currentTable.addNewHNodeAfter(null, vWorkspace.getRepFactory(), newColumnName, worksheet,true);

		populateRowsWithDefaultValues(worksheet, workspace.getFactory());
		//save the new hNodeId for undo

		return ndid;
	}

	private void populateRowsWithDefaultValues(Worksheet worksheet, RepFactory factory) {
		SuperSelection selection = getSuperSelection(worksheet);
		HNodePath selectedPath = null;
		List<HNodePath> columnPaths = worksheet.getHeaders().getAllPaths();
		for (HNodePath path : columnPaths) {
			if (path.contains(factory.getHNode(newHNodeId))) {
				if (path.getLeaf().getId().compareTo(newHNodeId) != 0) {
					HNodePath hp = new HNodePath();
					HNode hn = path.getFirst();
					while (hn.getId().compareTo(newHNodeId) != 0) {
						hp.addHNode(hn);
						path = path.getRest();
						hn = path.getFirst();
					}
					hp.addHNode(hn);
					selectedPath = hp;
				}
				else
					selectedPath = path;
			}
		}
		Collection<Node> nodes = new ArrayList<>(Math.max(1000, worksheet.getDataTable().getNumRows()));
		worksheet.getDataTable().collectNodes(selectedPath, nodes, selection);

		for (Node node : nodes) {
			CellValue valueToBeSet = inputValue.get(node.getBelongsToRow().getId());
			if(valueToBeSet != null){
				node.setValue(valueToBeSet, Node.NodeStatus.original, factory);
			}
			else{
				logger.debug("No value found for column: " + factory.getHNode(node.getHNodeId()).getColumnName()
					+ " -> row ID " + node.getBelongsToRow().getId());
			}
		}
	}

}
