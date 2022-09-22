package edu.isi.karma.web.plugins.batch.util;

import com.google.common.base.Strings;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.persistence.FilePersistenceStrategy;
import com.thoughtworks.xstream.persistence.PersistenceStrategy;
import com.thoughtworks.xstream.persistence.XmlMap;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;
import edu.isi.karma.config.*;
import edu.isi.karma.controller.update.UpdateContainer;
import edu.isi.karma.er.helper.PythonRepository;
import edu.isi.karma.er.helper.PythonRepositoryRegistry;
import edu.isi.karma.metadata.*;
import edu.isi.karma.modeling.semantictypes.SemanticTypeUtil;
import edu.isi.karma.webserver.ContextParametersRegistry;
import edu.isi.karma.webserver.KarmaException;
import edu.isi.karma.webserver.ServletContextParameterMap;
import edu.isi.karma.webserver.ServletContextParameterMap.ContextParameter;
import edu.isi.karma.web.plugins.batch.model.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Data access object to access {@link Process} objects from the file system.
 *
 * @author danish
 */
public class ProcessManager {

	private static final Logger logger = LoggerFactory.getLogger(ProcessManager.class);
	private static ProcessManager singleton;
	private ServletContextParameterMap contextMap;
	private String batchModeUserDirectory;
	private XmlMap historyProcessMap;
	private XmlMap scheduledProcessMap;
	private Map<String, Process> activeProcessMap;

	public static ProcessManager getInstance() {
		return singleton;
	}

	public static boolean initProcessManager(ServletContext ctx, UriInfo uriInfo) {
		if (ProcessManager.getInstance() != null) {
			return false;
		}
		ProcessManager pm = new ProcessManager();
		ServletContextParameterMap contextParameters;
		ContextParametersRegistry contextParametersRegistry = ContextParametersRegistry.getInstance();
		contextParameters = contextParametersRegistry.registerByKarmaHome(null);

		try {
			ProcessManager.initContextParameters(ctx, contextParameters);
		} catch (Exception e) {
			logger.error("Unable to initialize parameters using servlet context", e);
		}
		UpdateContainer updateContainer = new UpdateContainer();

		logger.info("Start Metadata Setup");
		try {
			KarmaMetadataManager metadataManager = new KarmaMetadataManager(contextParameters);
			metadataManager.register(new UserUploadedMetadata(contextParameters), updateContainer);
			metadataManager.register(new UserPreferencesMetadata(contextParameters), updateContainer);
			metadataManager.register(new UserConfigMetadata(contextParameters), updateContainer);
			metadataManager.register(new PythonTransformationMetadata(contextParameters), updateContainer);
			metadataManager.register(new TextualSemanticTypeModelMetadata(contextParameters), updateContainer);
			metadataManager.register(new NumericSemanticTypeModelMetadata(contextParameters), updateContainer);
			metadataManager.register(new SemanticTypeModelMetadata(contextParameters), updateContainer);
			metadataManager.register(new OntologyMetadata(contextParameters), updateContainer);
			metadataManager.register(new JSONModelsMetadata(contextParameters), updateContainer);
			metadataManager.register(new PythonTransformationMetadata(contextParameters), updateContainer);
			metadataManager.register(new CatalogDownloadMetadata(contextParameters), updateContainer);
			metadataManager.register(new GraphVizMetadata(contextParameters), updateContainer);
			metadataManager.register(new ModelLearnerMetadata(contextParameters), updateContainer);
			metadataManager.register(new WebPluginsMetadata(contextParameters), updateContainer);
			metadataManager.register(new R2RMLMetadata(contextParameters), updateContainer);
			metadataManager.register(new R2RMLPublishedMetadata(contextParameters), updateContainer);
			metadataManager.register(new RDFMetadata(contextParameters), updateContainer);
			metadataManager.register(new EMLMetadata(contextParameters), updateContainer);
			metadataManager.register(new CSVMetadata(contextParameters), updateContainer);
			metadataManager.register(new JSONMetadata(contextParameters), updateContainer);
			metadataManager.register(new ReportMetadata(contextParameters), updateContainer);
			metadataManager.register(new AvroMetadata(contextParameters), updateContainer);
			metadataManager.register(new KMLPublishedMetadata(contextParameters), updateContainer);

			URL url = uriInfo.getBaseUri().toURL();

			String port = "";
			String host = "";

			if (Strings.isNullOrEmpty(port) || Strings.isNullOrEmpty(host)) {
				host = url.getProtocol() + "://" + url.getHost();
				port = String.valueOf(url.getPort());

				//Set JETTY_PORT and HOST
				contextParameters.setParameterValue(ContextParameter.JETTY_PORT, port + ctx.getContextPath());
				logger.info("JETTY_PORT initialized to " + port + ctx.getContextPath());

				contextParameters.setParameterValue(ContextParameter.JETTY_HOST, host);
				logger.info("JETTY_HOST initialized to " + host);
			}

			// also set PUBLIC_RDF_ADDRESS
			contextParameters.setParameterValue(
				ContextParameter.PUBLIC_RDF_ADDRESS, host + ":" + port + ctx.getContextPath() + "/publish/RDF/"
			);

			// also set PUBLIC_EML_ADDRESS
			contextParameters.setParameterValue(
				ContextParameter.PUBLIC_EML_ADDRESS, host + ":" + port + ctx.getContextPath() + "/publish/EML/"
			);
		} catch (KarmaException | MalformedURLException e) {
			logger.error("Unable to complete Karma set up: ", e);
		}

		UIConfiguration uiConfiguration = UIConfigurationRegistry.getInstance().getUIConfiguration(contextParameters.getId());
		uiConfiguration.loadConfig();
		ModelingConfigurationRegistry.getInstance().register(contextParameters.getId());
		KnowDiveServicesConfigurationRegistry.getInstance().register(contextParameters.getId());

		/*********** setting up context setting *******/
		pm.contextMap = contextParameters;
		ContextParametersRegistry.getInstance().registerByServletContextParameterMap(pm.contextMap);
		PythonRepository pythonRepository = new PythonRepository(false,
			pm.contextMap.getParameterValue(ContextParameter.USER_PYTHON_SCRIPTS_DIRECTORY));
		PythonRepositoryRegistry.getInstance().register(pythonRepository);

		SemanticTypeUtil.setSemanticTypeTrainingStatus(false);

		ModelingConfiguration modelingConfiguration = ModelingConfigurationRegistry.getInstance().register(
			pm.contextMap.getId());
		modelingConfiguration.setLearnerEnabled(false); // disable automatic

		pm.batchModeUserDirectory = pm.contextMap.getParameterValue(ContextParameter.USER_WEB_PLUGIN_DIRECTORY)
			+ "BatchMode";
		File batchModeDirectory = makeDir(pm.batchModeUserDirectory);
		File historyProcessDir = makeDir(batchModeDirectory.getAbsolutePath() + "/HistoryProcess");
		File scheduledProcessDir = makeDir(batchModeDirectory.getAbsolutePath() + "/ScheduledProcess");

		/********************** setting up xStream ***************************/

		XStream xstream = new XStream(new DomDriver());
		// clear out existing permissions and set own ones
		xstream.addPermission(NoTypePermission.NONE);
		// allow some basics
		xstream.addPermission(NullPermission.NULL);
		xstream.addPermission(PrimitiveTypePermission.PRIMITIVES);
		xstream.allowTypeHierarchy(Collection.class);
		// allow any type from the same package
		xstream.allowTypesByWildcard(new String[]{
			"edu.isi.karma.web.plugins.batch" + ".*.*"
		});
		xstream.allowTypeHierarchy(String.class);
		xstream.autodetectAnnotations(true);

		PersistenceStrategy historyPersistenceStrategy = new FilePersistenceStrategy(historyProcessDir, xstream);
		PersistenceStrategy scheduledPersistenceStrategy = new FilePersistenceStrategy(scheduledProcessDir, xstream);
		pm.historyProcessMap = new XmlMap(historyPersistenceStrategy);
		pm.scheduledProcessMap = new XmlMap(scheduledPersistenceStrategy);
		pm.activeProcessMap = new HashMap<>();
		ProcessManager.singleton = pm;

		return true;
	}

	public static File makeDir(String dirAbsPath) {
		File dir = new File(dirAbsPath);
		if (dir.mkdirs()) {
			logger.debug("Directory " + dir.getAbsolutePath() + " has been created.");
		} else if (dir.isDirectory()) {
			logger.debug("Directory " + dir.getAbsolutePath() + " has already exists.");
		} else {
			logger.debug("Directory " + dir.getAbsolutePath() + " could not be created.");
			dir = null;
		}
		return dir;
	}

	/**
	 * add a process to list and persist it to storage
	 *
	 * @param process
	 * @return true if persistence strategy is defined and process doesn't exit. false otherwise.
	 */
	public boolean addToScheduled(Process process) {
		if (scheduledProcessMap != null && scheduledProcessMap.get(process.getProcessName()) == null) {
			scheduledProcessMap.put(process.getProcessName(), process);
			return true;
		} else {
			return false;
		}
	}

	public String getBatchModeUserDirectory() {
		return batchModeUserDirectory;
	}

	public String getAvailableScheduledProcessName() {
		int maxKey = 0;
		if (scheduledProcessMap != null && !scheduledProcessMap.isEmpty()) {
			for (Object processName : scheduledProcessMap.keySet()) {
				String id = (String) processName;
				maxKey = Math.max(maxKey, Integer.parseInt(id.replaceAll("\\D+", "")));
			}
		}
		maxKey = maxKey + 1;
		return "Batch-Scheduled-Process-" + maxKey;
	}

	public List<Process> getAllHistory() {
		List<Process> list = null;
		if (this.historyProcessMap != null) {
			list = new ArrayList<>(this.historyProcessMap.values());
			list.sort(Collections.reverseOrder());
		}
		return list;
	}

	public List<Process> getAllScheduled() {
		List<Process> list = null;
		if (this.scheduledProcessMap != null) {
			list = new ArrayList<>(this.scheduledProcessMap.values());
			list.sort(Collections.reverseOrder());
		}
		return list;
	}

	public boolean removeScheduledProcess(String processName) {
		if (scheduledProcessMap != null && scheduledProcessMap.get(processName) != null) {
			scheduledProcessMap.remove(processName);
			return true;
		}
		return false;
	}

	/**
	 * @param processName
	 * @return
	 */
	public Process getScheduledProcess(String processName) {
		if (scheduledProcessMap != null && scheduledProcessMap.get(processName) != null) {
			return (Process) scheduledProcessMap.get(processName);
		}
		return null;
	}

	public void updateScheduledProcess(Process newProcess) {
		Process oldProcess = getScheduledProcess(newProcess.getProcessName());
		if (oldProcess != null) {
			if (removeScheduledProcess(oldProcess.getProcessName())) {
				addToScheduled(newProcess);
			}
		}
	}

	public ServletContextParameterMap getContextMap() {
		return contextMap;
	}

	public boolean addToHistory(Process process) {
		if (historyProcessMap != null && historyProcessMap.get(process.getProcessName()) == null) {
			historyProcessMap.put(process.getProcessName(), process);
			return true;
		} else {
			return false;
		}
	}

	public boolean isActive() {
		return true;
	}

	public List<Process> getAllActive() {
		List<Process> list = null;
		if (this.activeProcessMap != null) {
			list = new ArrayList<>(this.activeProcessMap.values());
			list.sort(Collections.reverseOrder());
		}
		return list;
	}

	public synchronized boolean addToActive(Process process) {
		if (activeProcessMap != null && activeProcessMap.get(process.getProcessName()) == null) {
			activeProcessMap.put(process.getProcessName(), process);
			return true;
		} else {
			return false;
		}
	}

	public synchronized boolean removeFromActive(Process process) {
		if (activeProcessMap != null && activeProcessMap.get(process.getProcessName()) != null) {
			activeProcessMap.remove(process.getProcessName());
			return true;
		}
		return false;
	}

	public void runProcess(Process process) {
		addToActive(process);
//		ProcessManager.waitThread(1000 * 45);
		if (process.isDataInputStream()) {
			logger.info("generating workspace");
			process.setMessage("uploading file");
			process.generateWorkSpace();
		}
		logger.info("creating new process");
		Thread t = new Thread(process);
		t.start();
	}

	public static void waitThread(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	public static void initContextParameters(ServletContext ctx, ServletContextParameterMap contextParameters) {
		Enumeration<?> params = ctx.getInitParameterNames();
		ArrayList<String> validParams = new ArrayList<>();
		for (ContextParameter param : ContextParameter.values()) {
			validParams.add(param.name());
		}
		while (params.hasMoreElements()) {
			String param = params.nextElement().toString();
			if (validParams.contains(param)) {
				ContextParameter mapParam = ContextParameter.valueOf(param);
				String value = ctx.getInitParameter(param);
				contextParameters.setParameterValue(mapParam, value);
			}
		}

		//String contextPath = ctx.getRealPath(File.separator);
		String contextPath = ctx.getRealPath("/"); //File.separator was not working in Windows. / works
		contextParameters.setParameterValue(ContextParameter.WEBAPP_PATH, contextPath);
	}
}
