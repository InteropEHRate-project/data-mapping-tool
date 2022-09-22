package edu.isi.karma.controller.command;

import edu.isi.karma.rep.CellValue;
import edu.isi.karma.rep.HNode;
import edu.isi.karma.rep.Workspace;

import java.util.HashMap;

/**
 * abstract class which extend {@link CommandFactory} in oder to handel all the commands
 * with input type of {@link CellValue} and Map interface.
 *
 * @author Danish danishasghar.cheema@studenti.unitn.it
 */
public abstract class CellValueInputCommandFactory extends CommandFactory {

	public abstract Command createCommand(HashMap<String, CellValue> inputValue, String model, Workspace workspace,
										  String hNodeID, String worksheetId,
										  String hTableId, String newColumnName,
										  HNode.HNodeType type, String selectionId);
}
