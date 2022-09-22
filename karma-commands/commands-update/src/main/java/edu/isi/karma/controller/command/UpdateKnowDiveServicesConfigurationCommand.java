package edu.isi.karma.controller.command;

import edu.isi.karma.config.KnowDiveServicesConfiguration.KnowDiveEnvVariables;
import edu.isi.karma.config.KnowDiveServicesConfiguration;
import edu.isi.karma.config.KnowDiveServicesConfigurationRegistry;
import edu.isi.karma.controller.update.AbstractUpdate;
import edu.isi.karma.controller.update.ErrorUpdate;
import edu.isi.karma.controller.update.InfoUpdate;
import edu.isi.karma.controller.update.UpdateContainer;
import edu.isi.karma.rep.Workspace;
import edu.isi.karma.view.VWorkspace;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;

/**
 * command to be used to update knowdive configuration file.
 *
 * @author Danish
 */
public class UpdateKnowDiveServicesConfigurationCommand  extends Command {

	private final String new_scroll_service_url;
	private final String new_eml_service_url;
	private final String new_kos_service_url;
	private final String new_kg_importer_url;
	private final String new_catalog_base_url;
	private final String new_catalog_instance_api_key;
	private final boolean resetToDefault;

	private static final Logger logger = LoggerFactory.getLogger(UpdateKnowDiveServicesConfigurationCommand.class);

	protected UpdateKnowDiveServicesConfigurationCommand(
			String id,
			String model,
			String new_scroll_service_url,
			String new_eml_service_url,
			String new_kos_service_url,
			String new_kg_importer_url,
			String new_catalog_base_url,
			String new_catalog_instance_api_key,
			boolean resetToDefault
	){
		super(id, model);
		this.new_scroll_service_url = new_scroll_service_url;
		this.new_eml_service_url = new_eml_service_url;
		this.new_kos_service_url = new_kos_service_url;
		this.new_kg_importer_url = new_kg_importer_url;
		this.new_catalog_base_url = new_catalog_base_url;
		this.new_catalog_instance_api_key = new_catalog_instance_api_key;
		this.resetToDefault = resetToDefault;
	}

	@Override
	public String getCommandName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String getTitle() {
		return "Set KnowDive MicroService Configuration";
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public CommandType getCommandType() {
		return CommandType.notInHistory;
	}

	@Override
	public UpdateContainer doIt(Workspace workspace) throws CommandException {
		UpdateContainer uc = new UpdateContainer(new InfoUpdate("KnowDive config file update successfully"));

		try {

			uc.add(new AbstractUpdate() {

				@Override
				public void generateJson(String prefix, PrintWriter pw,
										 VWorkspace vWorkspace) {
				try {
					KnowDiveServicesConfiguration kdConfiguration =
							KnowDiveServicesConfigurationRegistry.getInstance().getKnowDiveConfiguration(
									vWorkspace.getWorkspace().getContextId()
							);
					JSONStringer jsonStr = new JSONStringer();
					JSONWriter writer = jsonStr.object();
					writer.key("updateType").value("UpdateKnowDiveServicesConfigurationUpdate");
					if(resetToDefault){
						kdConfiguration.resetToDefault(true);
						writer.key("knowdive_reset_default").value(true);
					}
					else {
						if(new_scroll_service_url != null) {
							kdConfiguration.setScroll_service_url(new_scroll_service_url);
							writer.key(KnowDiveEnvVariables.SCROLL_SERVICE_URL.name()).value(new_scroll_service_url);
						}
						if(new_eml_service_url != null) {
							kdConfiguration.setEml_service_url(new_eml_service_url);
							writer.key(KnowDiveEnvVariables.EML_SERVICE_URL.name()).value(new_eml_service_url);
						}
						if(new_kos_service_url != null) {
							kdConfiguration.setKos_service_url(new_kos_service_url);
							writer.key(KnowDiveEnvVariables.KOS_SERVICE_URL.name()).value(new_kos_service_url);
						}
						if(new_kg_importer_url != null) {
							kdConfiguration.setkg_importer_url(new_kg_importer_url);
							writer.key(KnowDiveEnvVariables.KG_IMPORTER_URL.name()).value(new_kg_importer_url);
						}
						if(new_catalog_base_url != null) {
							kdConfiguration.setCatalog_base_url(new_catalog_base_url);
							writer.key(KnowDiveEnvVariables.CATALOG_BASE_URL.name()).value(new_catalog_base_url);
						}
						if(new_catalog_instance_api_key != null) {
							kdConfiguration.setCatalog_instance_api_key(new_catalog_instance_api_key);
							writer.key(KnowDiveEnvVariables.CATALOG_INSTANCE_API_KEY.name()).value(new_catalog_instance_api_key);
						}
					}
					writer.endObject();
					pw.print(writer.toString());
				} catch (Exception e) {
					logger.error("Error updating knowdive services Configuration", e);
				}

				}

			});

			return uc;
		} catch (Exception e) {
			logger.error("Error updating knowdive services Configuration:" , e);
			uc.add(new ErrorUpdate("Error updating knowdive services Configuration"));
			return uc;
		}
	}

	@Override
	public UpdateContainer undoIt(Workspace workspace) {
		return null;
	}
}
