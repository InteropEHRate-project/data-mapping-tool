package edu.isi.karma.controller.command.worksheet;

import com.google.gson.reflect.TypeToken;
import edu.isi.karma.controller.command.CellValueInputCommandFactory;
import edu.isi.karma.controller.command.Command;
import edu.isi.karma.rep.CellValue;
import edu.isi.karma.rep.HNode;
import edu.isi.karma.rep.Workspace;
import edu.isi.karma.util.GsonUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * overrides two methods, first is form {@link CellValueInputCommandFactory} where it supports java data structure
 * like Map which is ideal if command is triggered from the karma backend and big size of data is need to transfer.
 * other is from {@link edu.isi.karma.controller.command.CommandFactory} which handel request directly from frontend.
 * note that it deSerialize the CellValue using Gson library and
 * {@link edu.isi.karma.gson.adopters.RuntimeTypeAdapterFactory} in order to restore the java polymorphism property.
 */
public class AddCellValueCommandFactory extends CellValueInputCommandFactory {

	private enum Arguments {
		inputValue, worksheetId, hTableId, hNodeId,
		newColumnName, selectionName,
	}

	@Override
	public Command createCommand(HashMap<String, CellValue> inputValue, String model, Workspace workspace,
								 String hNodeID, String worksheetId,
								 String hTableId, String newColumnName,
								 HNode.HNodeType type, String selectionId) {
		return new AddCellValueCommand(inputValue, getNewId(workspace), model, worksheetId,
				hTableId, hNodeID, newColumnName, type, selectionId);
	}

	/**	<p>
	 * Without additional type information, the serialized JSON is ambiguous. Is
	 * the cell value of type Concept or String?
	 * This method requires inputValue with additional type information to the
	 * serialized JSON and honoring that type information when the JSON is
	 * deserialized: <pre>   {@code
	 *   {
	 *     "cellValue": {
	 *       "type": "ConceptCellValue",
	 *       "concepts": [{
	 *			//concept1
	 *       },
	 *       {
	 *			//concept2
	 *       }]
	 *     },
	 *     "cellValue": {
	 *       "type": "StringCellValue",
	 *       "value": "string"
	 *     }
	 *   }}</pre>
	 *   </p>
	 **/
	@Override
	public Command createCommand(HttpServletRequest request, Workspace workspace) {
		HashMap<String,CellValue> inputValue = GsonUtil.INSTANCE.getCellValueGsonObj().fromJson(
				request.getParameter(AddCellValueCommandFactory.Arguments.inputValue.name()),
				new TypeToken<HashMap<String, CellValue>>() {}.getType());
		String hNodeId = request.getParameter(AddCellValueCommandFactory.Arguments.hNodeId.name());
		String hTableId = request.getParameter(AddCellValueCommandFactory.Arguments.hTableId.name());
		String worksheetId = request.getParameter(AddCellValueCommandFactory.Arguments.worksheetId.name());
		String selectionName = request.getParameter(AddCellValueCommandFactory.Arguments.selectionName.name());
		String newColumnName = request.getParameter(AddCellValueCommandFactory.Arguments.newColumnName.name());
		return new AddCellValueCommand(inputValue, getNewId(workspace), Command.NEW_MODEL, worksheetId,
				hTableId, hNodeId, newColumnName , HNode.HNodeType.Transformation,
				selectionName);
	}


	@Override
	public Class<? extends Command> getCorrespondingCommand() {
		return AddCellValueCommand.class;
	}
}
