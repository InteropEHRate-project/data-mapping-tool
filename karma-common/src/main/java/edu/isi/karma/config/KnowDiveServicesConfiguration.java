package edu.isi.karma.config;

import edu.isi.karma.controller.update.UpdateContainer;
import edu.isi.karma.metadata.KarmaMetadataManager;
import edu.isi.karma.metadata.UserConfigMetadata;
import edu.isi.karma.webserver.ContextParametersRegistry;
import edu.isi.karma.webserver.KarmaException;
import edu.isi.karma.webserver.ServletContextParameterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

/**
 * set the environment values for knowdive microservices for KarmaLinker app
 * first priority is environment variable.
 * second is knowdive.property file
 * 3rd is default values from this class.
 *
 * Note: if reset to default is needed, an option can be remove the knowdive.property file and rerun the karma app,
 * 			this will create a new property file with default value given in this class (if any).
 *
 * @author Danish
 * @date 28/01/2020
 */
public class KnowDiveServicesConfiguration {

	public enum KnowDiveEnvVariables{
		SCROLL_SERVICE_URL,
		EML_SERVICE_URL,
		KOS_SERVICE_URL,
		KG_IMPORTER_URL,
		CATALOG_BASE_URL,
		CATALOG_INSTANCE_API_KEY
	}

	private String scroll_service_url = "";
	private String eml_service_url = "";
	private String kos_service_url = "";
	private String kg_importer_url = "";
	private String catalog_base_url = "";
	private String catalog_instance_api_key = "";

	private static final Logger logger = LoggerFactory.getLogger(KnowDiveServicesConfiguration.class);

	private static final String newLine = System.getProperty("line.separator");

	private static final String prop_scroll_service_url = KnowDiveEnvVariables.SCROLL_SERVICE_URL.name()
			+ "";
	private static final String prop_eml_service_url = KnowDiveEnvVariables.EML_SERVICE_URL.name()
			+ "";
	private static final String prop_kos_service_url = KnowDiveEnvVariables.KOS_SERVICE_URL.name()
			+ "";
	private static final String prop_kg_importer_url = KnowDiveEnvVariables.KG_IMPORTER_URL.name()
			+ "=";
	private static final String prop_catalog_base_url = KnowDiveEnvVariables.CATALOG_BASE_URL.name()
			+ "=";
	private static final String prop_catalog_instance_api_key = KnowDiveEnvVariables.CATALOG_INSTANCE_API_KEY.name()
			+ "=";

	private static final String defaultKDProperties = prop_scroll_service_url + newLine
			+ prop_eml_service_url + newLine
			+ prop_kos_service_url + newLine
			+ prop_kg_importer_url + newLine
			+ prop_catalog_base_url + newLine
			+ prop_catalog_instance_api_key + newLine
			;

	private final String contextId;
	private ServletContextParameterMap contextParameters;
	private Properties kdProperties;

	public KnowDiveServicesConfiguration(String contextId) {
		this.contextId = contextId;
	}

	public void loadConfig() {

		try {
			// Step 0: get the context parameter from workspace contextID.
			contextParameters = ContextParametersRegistry.getInstance().getContextParameters(contextId);
			this.kdProperties = new Properties();
			// link to the directory with Configuration files
			String userConfigDir = contextParameters.getParameterValue(
					ServletContextParameterMap.ContextParameter.USER_CONFIG_DIRECTORY) ;
			logger.info("KnowDiveConfiguration:" + userConfigDir + "knowdive.properties");
			if(userConfigDir == null || userConfigDir.length() == 0) {
				try {

					//TODO this should never be necessary. why did this happen?
					KarmaMetadataManager mgr = new KarmaMetadataManager(contextParameters);
					mgr.register(new UserConfigMetadata(contextParameters), new UpdateContainer());
				} catch (KarmaException e) {
					logger.error("Could not register with KarmaMetadataManager", e);
				}

			}

			// Step 1: Load Files, if there is no file create new with default properties
			File file = resetToDefault(false);

			// Step 2: convert the file into property object
			FileInputStream fis = new FileInputStream(file);
			try {
				kdProperties.load(fis);
			} finally {
				fis.close();
			}

			// Step 3: initiate or over ride local objects with values found in the file.
			String envValue = System.getenv(KnowDiveEnvVariables.SCROLL_SERVICE_URL.name());
			String prpValue = kdProperties.getProperty(KnowDiveEnvVariables.SCROLL_SERVICE_URL.name());
			if(envValue != null) {
				logger.info("set SCROLL_SERVICE_URL="+ envValue +" from environment variable");
				scroll_service_url = envValue;
			}
			else if(prpValue != null) {
				logger.info("set SCROLL_SERVICE_URL="+prpValue + " from " + userConfigDir + "knowdive.properties");
				scroll_service_url = prpValue;
			}
			else {
				logger.info("set default value "+prop_scroll_service_url);
				addProperty(prop_scroll_service_url);
			}

			envValue = System.getenv(KnowDiveEnvVariables.EML_SERVICE_URL.name());
			prpValue = kdProperties.getProperty(KnowDiveEnvVariables.EML_SERVICE_URL.name());
			if(envValue != null) {
				logger.info("set EML_SERVICE_URL="+ envValue +" from environment variable");
				eml_service_url = envValue;
			}
			else if(prpValue != null) {
				logger.info("set EML_SERVICE_URL="+prpValue + " from " + userConfigDir + "knowdive.properties");
				eml_service_url = prpValue;
			}
			else {
				logger.info("set default value "+prop_eml_service_url);
				addProperty(prop_eml_service_url);
			}

			envValue = System.getenv(KnowDiveEnvVariables.KOS_SERVICE_URL.name());
			prpValue = kdProperties.getProperty(KnowDiveEnvVariables.KOS_SERVICE_URL.name());
			if(envValue != null) {
				logger.info("set KOS_SERVICE_URL="+ envValue +" from environment variable");
				kos_service_url = envValue;
			}
			else if(prpValue != null) {
				logger.info("set KOS_SERVICE_URL="+prpValue + " from " + userConfigDir + "knowdive.properties");
				kos_service_url = prpValue;
			}
			else {
				logger.info("set default value "+prop_kos_service_url);
				addProperty(prop_kos_service_url);
			}

			envValue = System.getenv(KnowDiveEnvVariables.KG_IMPORTER_URL.name());
			prpValue = kdProperties.getProperty(KnowDiveEnvVariables.KG_IMPORTER_URL.name());
			if(envValue != null) {
				logger.info("set KG_IMPORTER_URL="+ envValue +" from environment variable");
				kg_importer_url = envValue;
			}
			else if(prpValue != null) {
				logger.info("set KG_IMPORTER_URL="+prpValue + " from " + userConfigDir + "knowdive.properties");
				kg_importer_url = prpValue;
			}
			else {
				logger.info("set default value "+prop_kg_importer_url);
				addProperty(prop_kg_importer_url);
			}

			envValue = System.getenv(KnowDiveEnvVariables.CATALOG_BASE_URL.name());
			prpValue = kdProperties.getProperty(KnowDiveEnvVariables.CATALOG_BASE_URL.name());
			if(envValue != null) {
				logger.info("set CATALOG_BASE_URL="+ envValue +" from environment variable");
				catalog_base_url = envValue;
			}
			else if(prpValue != null) {
				logger.info("set CATALOG_BASE_URL="+prpValue + " from " + userConfigDir + "knowdive.properties");
				catalog_base_url = prpValue;
			}
			else {
				logger.info("set default value "+prop_catalog_base_url);
				addProperty(prop_catalog_base_url);
			}

			envValue = System.getenv(KnowDiveEnvVariables.CATALOG_INSTANCE_API_KEY.name());
			prpValue = kdProperties.getProperty(KnowDiveEnvVariables.CATALOG_INSTANCE_API_KEY.name());
			if(envValue != null) {
				logger.info("set CATALOG_INSTANCE_API_KEY="+ envValue +" from environment variable");
				catalog_instance_api_key = envValue;
			}
			else if(prpValue != null) {
				logger.info("set CATALOG_INSTANCE_API_KEY="+prpValue + " from " + userConfigDir + "knowdive.properties");
				catalog_instance_api_key = prpValue;
			}
			else {
				logger.info("set default value "+prop_catalog_instance_api_key);
				addProperty(prop_catalog_instance_api_key);
			}

		} catch (IOException e) {
			logger.error("Could not load knowdive.properties", e);
		}
	}

	public String getScroll_service_url() {
		return scroll_service_url;
	}

	public String getEml_service_url() {
		return eml_service_url;
	}

	public String getKos_service_url() {
		return kos_service_url;
	}

	public String getkg_importer_url() {
		return kg_importer_url;
	}

	public String getCatalog_base_url() {
		return catalog_base_url;
	}

	public String getCatalog_instance_api_key() {
		return catalog_instance_api_key;
	}

	public void setScroll_service_url(String scroll_service_url) {
		this.scroll_service_url = scroll_service_url;
		this.updateProperty(KnowDiveEnvVariables.SCROLL_SERVICE_URL.name(), scroll_service_url);
	}

	public void setEml_service_url(String eml_service_url) {
		this.eml_service_url = eml_service_url;
		this.updateProperty(KnowDiveEnvVariables.EML_SERVICE_URL.name(), eml_service_url);
	}

	public void setKos_service_url(String kos_service_url) {
		this.kos_service_url = kos_service_url;
		this.updateProperty(KnowDiveEnvVariables.KOS_SERVICE_URL.name(), kos_service_url);
	}

	public void setkg_importer_url(String kg_importer_url) {
		this.kg_importer_url = kg_importer_url;
		this.updateProperty(KnowDiveEnvVariables.KG_IMPORTER_URL.name(), kg_importer_url);
	}

	public void setCatalog_base_url(String catalog_base_url) {
		this.catalog_base_url = catalog_base_url;
		this.updateProperty(KnowDiveEnvVariables.CATALOG_BASE_URL.name(), catalog_base_url);
	}

	public void setCatalog_instance_api_key(String catalog_instance_api_key) {
		this.catalog_instance_api_key = catalog_instance_api_key;
		this.updateProperty(KnowDiveEnvVariables.CATALOG_INSTANCE_API_KEY.name(), catalog_instance_api_key);
	}

	private void addProperty(String propLine){
		try {
			File file = new File(contextParameters.getParameterValue(
					ServletContextParameterMap.ContextParameter.USER_CONFIG_DIRECTORY) + "/knowdive.properties");
			PrintWriter out = null;
			out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
			out.println(propLine);
			out.close();
			String[] keyValue = propLine.split("=");
			this.kdProperties.put(keyValue[0], keyValue[1]);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	private void updateProperty(String key, String value){
		try {
			this.kdProperties.put(key, value);
			File file = new File(contextParameters.getParameterValue(
					ServletContextParameterMap.ContextParameter.USER_CONFIG_DIRECTORY) + "/knowdive.properties");
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, false)));
			logger.info("Write Property: " + key +"="+ this.kdProperties.getProperty(key));
			this.kdProperties.store(out, null);
			out.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 *
	 * @param forceReset true: it will remove the existing file and return new file with default values
	 *                   false: if file exist, return the file, else create new file with default values
	 * @throws IOException
	 */
	public File resetToDefault(boolean forceReset){
		File file = new File(contextParameters.getParameterValue(
				ServletContextParameterMap.ContextParameter.USER_CONFIG_DIRECTORY) + "/knowdive.properties");
		if(forceReset || !file.exists()) {
			try {
				file.createNewFile();
				OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter outWriter = new PrintWriter(bw);
				outWriter.println(defaultKDProperties);
				outWriter.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}
		return file;
	}
}
