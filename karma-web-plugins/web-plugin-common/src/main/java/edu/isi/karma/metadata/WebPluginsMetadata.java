package edu.isi.karma.metadata;

import edu.isi.karma.webserver.KarmaException;
import edu.isi.karma.webserver.ServletContextParameterMap;

import java.io.File;

/**
 * class extends {@link KarmaUserMetadata} class to define the directory where plugin data can be stored.
 *
 * @author Danish
 * @date 2020/12/20
 */
public class WebPluginsMetadata extends KarmaUserMetadata {

	public WebPluginsMetadata(ServletContextParameterMap contextParameters) throws KarmaException {
		super(contextParameters);
	}

	@Override
	protected ServletContextParameterMap.ContextParameter getDirectoryContextParameter() {
		return ServletContextParameterMap.ContextParameter.USER_WEB_PLUGIN_DIRECTORY;
	}

	@Override
	protected String getDirectoryPath() {
		return "web-plugin-files" + File.separator;
	}

	@Override
	public KarmaMetadataType getType() {
		return WebPluginsMetaDataTypes.USER_WEB_PLUGIN_DIRECTORY;
	}

	public enum WebPluginsMetaDataTypes implements KarmaMetadataType {
		USER_WEB_PLUGIN_DIRECTORY
	}

}

