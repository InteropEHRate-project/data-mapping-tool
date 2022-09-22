package edu.isi.karma.controller.command.worksheet;

import edu.isi.karma.controller.command.Command;
import edu.isi.karma.controller.command.JSONInputCommandFactory;
import edu.isi.karma.controller.history.HistoryJsonUtil;
import edu.isi.karma.rep.Workspace;
import edu.isi.karma.webserver.KarmaException;
import org.json.JSONArray;
import org.json.JSONException;

import javax.servlet.http.HttpServletRequest;

/**
 * factory to create the {@link ExtractStructureCommand} command instance.
 * it handles the request coming from the frontend in json format.
 *
 * @author danish.cheema@unitn.it
 */
public class ExtractStructureCommandFactory extends JSONInputCommandFactory {

	public enum Arguments {
		worksheetId, hNodeId, rootLocale,
		selectionName, pipeline
	}

	@Override
	public Command createCommand(HttpServletRequest request,
								 Workspace workspace) {
		String hNodeId = request.getParameter(Arguments.hNodeId.name());
		String worksheetId = request.getParameter(Arguments.worksheetId.name());
		String selectionId = request.getParameter(Arguments.selectionName.name());
		String rootLocale = request.getParameter(Arguments.rootLocale.name());
		String pipeline = request.getParameter(Arguments.pipeline.name());
		return new ExtractStructureCommand(getNewId(workspace), Command.NEW_MODEL, worksheetId, hNodeId,
			selectionId, rootLocale, pipeline);
	}

	@Override
	public Command createCommand(JSONArray inputJson, String model, Workspace workspace)
		throws JSONException, KarmaException {
		String hNodeId = HistoryJsonUtil.getStringValue(Arguments.hNodeId.name(), inputJson);
		String worksheetId = HistoryJsonUtil.getStringValue(Arguments.worksheetId.name(), inputJson);
		String selectionName = HistoryJsonUtil.getStringValue(Arguments.selectionName.name(), inputJson);
		String rootLocale = HistoryJsonUtil.getStringValue(Arguments.rootLocale.name(), inputJson);
		String pipeline = HistoryJsonUtil.getStringValue(Arguments.pipeline.name(), inputJson);
		ExtractStructureCommand cmd = new ExtractStructureCommand(getNewId(workspace), model, worksheetId, hNodeId,
			selectionName, rootLocale, pipeline);

		cmd.setInputParameterJson(inputJson.toString());
		return cmd;
	}

	@Override
	public Class<? extends Command> getCorrespondingCommand() {
		return ExtractStructureCommand.class;
	}


}
