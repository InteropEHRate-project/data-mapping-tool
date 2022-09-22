package edu.isi.karma.controller.command;

import edu.isi.karma.controller.history.HistoryJsonUtil;
import edu.isi.karma.rep.Workspace;
import edu.isi.karma.webserver.KarmaException;
import org.json.JSONArray;
import org.json.JSONException;

import javax.servlet.http.HttpServletRequest;

/**
 * use to create an update command to override knowdive services configuration file.
 *
 * @author Danish
 */
public class UpdateKnowDiveServicesConfigurationCommandFactory extends JSONInputCommandFactory {

	// have to be the same names used for frontend newInfo variable in the settings.js
	enum Arguments {
		new_scroll_service_url,
		new_eml_service_url,
		new_kos_service_url,
		new_kg_importer_url,
		new_catalog_base_url,
		new_catalog_instance_api_key,
		reset_to_default
	}


	@Override
	public Command createCommand(HttpServletRequest request, Workspace workspace) {
		return null;
	}

	@Override
	public Command createCommand(JSONArray inputJson, String model, Workspace workspace)
			throws JSONException, KarmaException {
		String new_scroll_service_url = null;
		String new_eml_service_url = null;
		String new_kos_service_url = null;
		String new_kg_importer_url = null;
		String new_catalog_base_url = null;
		String new_catalog_instance_api_key = null;
		boolean reset_to_default = false;
		if(HistoryJsonUtil.valueExits(Arguments.new_scroll_service_url.name(), inputJson))
			new_scroll_service_url = HistoryJsonUtil.getStringValue(Arguments.new_scroll_service_url.name(), inputJson);
		if(HistoryJsonUtil.valueExits(Arguments.new_eml_service_url.name(), inputJson))
			new_eml_service_url = HistoryJsonUtil.getStringValue(Arguments.new_eml_service_url.name(), inputJson);
		if(HistoryJsonUtil.valueExits(Arguments.new_kos_service_url.name(), inputJson))
			new_kos_service_url = HistoryJsonUtil.getStringValue(Arguments.new_kos_service_url.name(), inputJson);
		if(HistoryJsonUtil.valueExits(Arguments.new_kg_importer_url.name(), inputJson))
			new_kg_importer_url = HistoryJsonUtil.getStringValue(Arguments.new_kg_importer_url.name(), inputJson);
		if(HistoryJsonUtil.valueExits(Arguments.new_catalog_base_url.name(), inputJson))
			new_catalog_base_url = HistoryJsonUtil.getStringValue(Arguments.new_catalog_base_url.name(), inputJson);
		if(HistoryJsonUtil.valueExits(Arguments.new_catalog_instance_api_key.name(), inputJson))
			new_catalog_instance_api_key = HistoryJsonUtil.getStringValue(Arguments.new_catalog_instance_api_key.name(), inputJson);
		if(HistoryJsonUtil.valueExits(Arguments.reset_to_default.name(), inputJson))
			reset_to_default = Boolean.getBoolean(
					HistoryJsonUtil.getStringValue(Arguments.reset_to_default.name(), inputJson)
			);

		UpdateKnowDiveServicesConfigurationCommand cmd = new UpdateKnowDiveServicesConfigurationCommand(
				getNewId(workspace),
				model,
				new_scroll_service_url,
				new_eml_service_url,
				new_kos_service_url,
				new_kg_importer_url,
				new_catalog_base_url,
				new_catalog_instance_api_key,
				reset_to_default
		);
		cmd.setInputParameterJson(inputJson.toString());
		return cmd;
	}

	@Override
	public Class<? extends Command> getCorrespondingCommand() {
		return UpdateKnowDiveServicesConfigurationCommand.class;
	}
}
