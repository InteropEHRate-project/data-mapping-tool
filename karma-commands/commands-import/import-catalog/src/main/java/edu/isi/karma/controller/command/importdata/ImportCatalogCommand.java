package edu.isi.karma.controller.command.importdata;

import edu.isi.karma.config.KnowDiveServicesConfigurationRegistry;
import edu.isi.karma.controller.update.ErrorUpdate;
import edu.isi.karma.controller.update.InfoUpdate;
import edu.isi.karma.controller.update.UpdateContainer;
import edu.isi.karma.imp.Import;
import edu.isi.karma.rep.Workspace;
import edu.isi.karma.web.services.catalog.rep.CatalogDataset;
import edu.isi.karma.web.services.catalog.rep.CatalogsDownloadManager;
import edu.isi.karma.web.services.catalog.util.CkanClientUtil;
import edu.isi.karma.webserver.ContextParametersRegistry;
import edu.isi.karma.webserver.ServletContextParameterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * command use to handel CKAN dataset to karma import request.
 * It will download selected resources to the backend of the karma.
 *
 * @author Danish
 * @date 2020/06/20
 */
public class ImportCatalogCommand extends ImportCommand {

	private static Logger logger = LoggerFactory.getLogger(ImportCatalogCommand.class);
	private String selectedDatasetId;
	private String selectedResourceIds;
	private String apiUrl;
	private String apiKey;

	protected ImportCatalogCommand(String id, String model, String datasetId, String selectedResources) {
		super(id, model);
		this.selectedDatasetId = datasetId;
		this.selectedResourceIds = selectedResources;
	}

	@Override
	public String getCommandName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String getTitle() {
		return "Import Service";
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public UpdateContainer doIt(Workspace workspace) {
		try {
			logger.info("setting up to start the catalog import");

			this.saveInHistory(false);
			this.apiUrl = KnowDiveServicesConfigurationRegistry.getInstance()
				.getKnowDiveConfiguration(workspace.getContextId())
				.getCatalog_base_url();
			this.apiKey = KnowDiveServicesConfigurationRegistry.getInstance()
				.getKnowDiveConfiguration(workspace.getContextId())
				.getCatalog_instance_api_key();

			ServletContextParameterMap contextParameters
				= ContextParametersRegistry.getInstance().getContextParameters(workspace.getContextId());
			String catalogDownloadDirectory
				= contextParameters.getParameterValue(ServletContextParameterMap.ContextParameter.USER_CATALOG_DOWNLOAD_DIRECTORY);

			CatalogsDownloadManager catalogDownloadManager = CatalogsDownloadManager.getInstance();
			//setting karma metadata
			catalogDownloadManager.setDownloadDirectory(catalogDownloadDirectory);
			//setup the download
			CatalogDataset catalogDataset = catalogDownloadManager.addNewDatasetForDownload(
				CkanClientUtil.getInstance().getCkanDataset(
					this.apiUrl, this.apiKey, this.selectedDatasetId),
				CkanClientUtil.getInstance().getCkanResources(
					this.apiUrl, this.apiKey, this.selectedDatasetId, this.selectedResourceIds),
				this.apiKey
			);
			//start the download
			if (!catalogDownloadManager.startDatasetDownload(catalogDataset)) {
				throw new Exception("Error in download starting.");
			}

			UpdateContainer c = new UpdateContainer(new InfoUpdate("Download Started"));
			return c;

		} catch (Exception e) {
			logger.error("Error occurred while importing catalog: " + e);
			return new UpdateContainer(new ErrorUpdate("Error occurred while importing catalog: " + e));
		}

	}

	@Override
	protected Import createImport(Workspace workspace) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
