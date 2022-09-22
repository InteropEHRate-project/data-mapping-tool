/*******************************************************************************
 * Copyright 2012 University of Southern California
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This code was developed by the Information Integration Group as part
 * of the Karma project at the Information Sciences Institute of the
 * University of Southern California.  For more information, publications,
 * and related projects, please see: http://www.isi.edu/integration
 ******************************************************************************/
package edu.isi.karma.webserver;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import edu.isi.karma.config.*;
import edu.isi.karma.metadata.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.isi.karma.controller.command.alignment.R2RMLAlignmentFileSaver;
import edu.isi.karma.controller.command.selection.SuperSelectionManager;
import edu.isi.karma.controller.history.CommandHistory;
import edu.isi.karma.controller.update.AbstractUpdate;
import edu.isi.karma.controller.update.UpdateContainer;
import edu.isi.karma.controller.update.WorksheetListUpdate;
import edu.isi.karma.controller.update.WorksheetUpdateFactory;
import edu.isi.karma.er.helper.PythonRepository;
import edu.isi.karma.er.helper.PythonRepositoryRegistry;
import edu.isi.karma.rep.Worksheet;
import edu.isi.karma.rep.Workspace;
import edu.isi.karma.rep.WorkspaceManager;
import edu.isi.karma.rep.metadata.Tag;
import edu.isi.karma.rep.metadata.TagsContainer.Color;
import edu.isi.karma.rep.metadata.TagsContainer.TagName;
import edu.isi.karma.view.VWorkspace;
import edu.isi.karma.view.VWorkspaceRegistry;
import edu.isi.karma.webserver.ServletContextParameterMap.ContextParameter;

public class KarmaServlet extends HttpServlet {
	private enum Arguments {
		hasPreferenceId, workspacePreferencesId, karmaHome
	}

	private static final long serialVersionUID = 1L;

	private static Logger logger = LoggerFactory.getLogger(KarmaServlet.class);

	protected void doGet(HttpServletRequest request,
						 HttpServletResponse response) throws ServletException, IOException {

		UpdateContainer updateContainer = new UpdateContainer();

		String karmaHomeDir = request.getParameter(Arguments.karmaHome.name());


		ContextParametersRegistry contextParametersRegistry = ContextParametersRegistry.getInstance();
		ServletContextParameterMap contextParameters = contextParametersRegistry.getContextParameters(karmaHomeDir);

		try {
			ServerStart.initContextParameters(this.getServletContext(), contextParameters);
		} catch (Exception e) {
			logger.error("Unable to initalize parameters using servlet context", e);
		}
		KarmaMetadataManager metadataManager = null;
		try {
			metadataManager = new KarmaMetadataManager(contextParameters);
			metadataManager.register(new UserUploadedMetadata(contextParameters), updateContainer);
			metadataManager.register(new UserPreferencesMetadata(contextParameters), updateContainer);
			metadataManager.register(new UserConfigMetadata(contextParameters), updateContainer);
		} catch (KarmaException e) {
			logger.error("Unable to complete Karma set up: ", e);
		}

		/* Check if any workspace id is set in cookies. */
		boolean hasWorkspaceCookieId = false;
		String hasPrefId = request.getParameter(Arguments.hasPreferenceId.name());
		if (hasPrefId != null && hasPrefId.equals("true"))
			hasWorkspaceCookieId = true;
		Workspace workspace = null;
		VWorkspace vwsp = null;

		/* If set, pick the right preferences and CRF Model file */
		if (hasWorkspaceCookieId) {
			String cachedWorkspaceId = request.getParameter(Arguments.workspacePreferencesId.name());
			workspace = WorkspaceManager.getInstance().createWorkspaceWithPreferencesId(cachedWorkspaceId, contextParameters.getId());
			vwsp = new VWorkspace(workspace, cachedWorkspaceId);
		} else {
			workspace = WorkspaceManager.getInstance().createWorkspace(contextParameters.getId());
			vwsp = new VWorkspace(workspace);
		}
		WorkspaceKarmaHomeRegistry.getInstance().register(workspace.getId(), contextParameters.getKarmaHome());
		WorkspaceRegistry.getInstance().register(new ExecutionController(workspace));
		VWorkspaceRegistry.getInstance().registerVWorkspace(workspace.getId(), vwsp);

		logger.info("Start Metadata Setup");
		try {
			metadataManager.register(new TextualSemanticTypeModelMetadata(contextParameters), updateContainer);
			metadataManager.register(new NumericSemanticTypeModelMetadata(contextParameters), updateContainer);
			metadataManager.register(new SemanticTypeModelMetadata(contextParameters), updateContainer);
			metadataManager.register(new OntologyMetadata(contextParameters), updateContainer);
			metadataManager.register(new JSONModelsMetadata(contextParameters), updateContainer);
			metadataManager.register(new PythonTransformationMetadata(contextParameters), updateContainer);
			metadataManager.register(new CatalogDownloadMetadata(contextParameters), updateContainer);
			metadataManager.register(new GraphVizMetadata(contextParameters), updateContainer);
			metadataManager.register(new ModelLearnerMetadata(contextParameters), updateContainer);
			metadataManager.register(new R2RMLMetadata(contextParameters), updateContainer);
			metadataManager.register(new R2RMLPublishedMetadata(contextParameters), updateContainer);
			metadataManager.register(new RDFMetadata(contextParameters), updateContainer);
			metadataManager.register(new EMLMetadata(contextParameters), updateContainer);
			metadataManager.register(new CSVMetadata(contextParameters), updateContainer);
			metadataManager.register(new JSONMetadata(contextParameters), updateContainer);
			metadataManager.register(new ReportMetadata(contextParameters), updateContainer);
			metadataManager.register(new AvroMetadata(contextParameters), updateContainer);
			metadataManager.register(new KMLPublishedMetadata(contextParameters), updateContainer);
			metadataManager.register(new WebPluginsMetadata(contextParameters), updateContainer);
			PythonRepository pythonRepository = new PythonRepository(true, contextParameters.getParameterValue(ContextParameter.USER_PYTHON_SCRIPTS_DIRECTORY));
			PythonRepositoryRegistry.getInstance().register(pythonRepository);

			String protocol = request.getProtocol();
			if (protocol != null) {

				String port = contextParameters.getParameterValue(ContextParameter.JETTY_PORT);
				String host = contextParameters.getParameterValue(ContextParameter.JETTY_HOST);

				if(Strings.isNullOrEmpty(port) || Strings.isNullOrEmpty(host)){
					port = String.valueOf(request.getServerPort());
					protocol = protocol.split("/")[0];
					host = protocol.toLowerCase() + "://" + request.getServerName();

					//Set JETTY_PORT and HOST
					contextParameters.setParameterValue(ContextParameter.JETTY_PORT, port);
					logger.info("JETTY_PORT initialized to " + port);

					contextParameters.setParameterValue(ContextParameter.JETTY_HOST, host);
					logger.info("JETTY_HOST initialized to " + host);
				}

				// also set PUBLIC_RDF_ADDRESS
				contextParameters.setParameterValue(
					ContextParameter.PUBLIC_RDF_ADDRESS, host + ":" + port + "/publish/RDF/"
				);

				// also set PUBLIC_EML_ADDRESS
				contextParameters.setParameterValue(
					ContextParameter.PUBLIC_EML_ADDRESS, host + ":" + port + "/publish/EML/"
				);

				// also set CLEANING_SERVICE_URL
				String cleaningServiceUrl = host + ":" + port
					+ contextParameters.getParameterValue(ServletContextParameterMap.ContextParameter.CLEANING_SERVICE_URL);
				logger.info("CLEANING SERVICE initialized to " + cleaningServiceUrl);
				contextParameters.setParameterValue(ServletContextParameterMap.ContextParameter.CLEANING_SERVICE_URL, cleaningServiceUrl);

				//and the CLUSTER_SERVICE url
				String clusterServiceUrl = host + ":" + port
					+ contextParameters.getParameterValue(ServletContextParameterMap.ContextParameter.CLUSTER_SERVICE_URL);
				logger.info("CLUSTER SERVICE initialized to " + clusterServiceUrl);
				contextParameters.setParameterValue(ServletContextParameterMap.ContextParameter.CLUSTER_SERVICE_URL, clusterServiceUrl);
			}
		} catch (KarmaException e) {
			logger.error("Unable to complete Karma set up: ", e);
		}
		metadataManager.setup(workspace, updateContainer);
		CommandHistory.setIsHistoryEnabled(true);
		CommandHistory.setHistorySaver(workspace.getId(), new R2RMLAlignmentFileSaver(workspace));

		// Initialize the Outlier tag
		Tag outlierTag = new Tag(TagName.Outlier, Color.Red);
		workspace.getTagsContainer().addTag(outlierTag);

		// updating cache is already done in OntologyMetadata setup
//		workspace.getOntologyManager().updateCache();

		// Put all created worksheet models in the view.
		updateContainer.add(new WorksheetListUpdate());

		for (Worksheet w : vwsp.getWorkspace().getWorksheets()) {
			updateContainer.append(WorksheetUpdateFactory.createWorksheetHierarchicalUpdates(w.getId(), SuperSelectionManager.DEFAULT_SELECTION, workspace.getContextId()));
		}

		updateContainer.add(new AbstractUpdate() {

			@Override
			public void generateJson(String prefix, PrintWriter pw,
									 VWorkspace vWorkspace) {
				//1. Load all configurations

				UIConfiguration uiConfiguration = UIConfigurationRegistry.getInstance().getUIConfiguration(vWorkspace.getWorkspace().getContextId());
				uiConfiguration.loadConfig();
				ModelingConfigurationRegistry.getInstance().register(vWorkspace.getWorkspace().getContextId());
				KnowDiveServicesConfigurationRegistry.getInstance().register(vWorkspace.getWorkspace().getContextId());

				//2 Return all settings related updates
				pw.println("{");
				pw.println("\"updateType\": \"UISettings\", ");
				pw.println("\"settings\": {");
				pw.println("  \"googleEarthEnabled\" : " + uiConfiguration.isGoogleEarthEnabled() + ",");
				pw.println("  \"maxLoadedClasses\" : " + uiConfiguration.getMaxClassesToLoad() + ",");
				pw.println("  \"maxLoadedProperties\" : " + uiConfiguration.getMaxPropertiesToLoad());
				pw.println("  }");
				pw.println("}");
			}

		});
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		updateContainer.applyUpdates(vwsp);
		updateContainer.generateJson("", pw, vwsp);

		// initialize the plugins
//		if(initializeWebPlugin(contextParameters, "batch-mode"))
//			logger.info("batch-mode plugin Active");

		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println(sw.toString());
	}

	/**
	 * method used to send contextParameters object to web plugins to be used to access the karma servlet parameters
	 * @param contextParameters
	 * @param plugin should be same as the name of the web-plug.
	 *               example to the final url http://example.com/plugin/rest/helper/initilize
	 */
	private boolean initializeWebPlugin(ServletContextParameterMap contextParameters, String plugin) {
		try {
			Gson gson = new Gson();
			String urlParameters = gson.toJson(contextParameters);
			byte[] input = urlParameters.getBytes(StandardCharsets.UTF_8);

			//setup url
			StringBuilder urlString = new StringBuilder();
			urlString.append(contextParameters.getParameterValue(ContextParameter.JETTY_HOST));
			String port = contextParameters.getParameterValue(ContextParameter.JETTY_PORT);
			if (!Strings.isNullOrEmpty(port))
				urlString.append(":").append(port);
			urlString.append("/").append(plugin).append("/rest/helper/initialize");

			//create request
			URL url = new URL(urlString.toString());
			logger.info("sending request to: " + url.toString());
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent", "Java client");
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			//send request
			try (OutputStream os = con.getOutputStream()) {
				os.write(input);
			}
			//get response
			try (BufferedReader br = new BufferedReader(
				new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
				StringBuilder resp = new StringBuilder();
				String responseLine;
				while ((responseLine = br.readLine()) != null) {
					resp.append(responseLine.trim());
				}
				logger.info("Response: " + resp.toString());
				return con.getResponseCode() == 200 || con.getResponseCode() == 201;
			}
		} catch (IOException e) {
			logger.error("unable to pass context parameter to plugin: " + plugin);
			e.printStackTrace();
			return false;
		}
	}
}
