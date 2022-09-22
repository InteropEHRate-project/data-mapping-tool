package edu.isi.karma.metadata;

import edu.isi.karma.webserver.KarmaException;
import edu.isi.karma.webserver.ServletContextParameterMap;

import java.io.File;

/**
 * class extends {@link KarmaUserMetadata} class to define the directory where downloaded catalogs can be stored.
 *
 * @author Danish
 * @date 2020/06/20
 */
public class CatalogDownloadMetadata extends KarmaUserMetadata {

	public CatalogDownloadMetadata(ServletContextParameterMap contextParameters) throws KarmaException {
		super(contextParameters);
	}

	@Override
	protected ServletContextParameterMap.ContextParameter getDirectoryContextParameter() {
		return ServletContextParameterMap.ContextParameter.USER_CATALOG_DOWNLOAD_DIRECTORY;
	}

	@Override
	protected String getDirectoryPath() {
		return "catalog-download" + File.separator;
	}

	@Override
	public KarmaMetadataType getType() {
		return CatalogDownloadMetadataTypes.USER_CATALOG_DOWNLOAD;
	}

	public enum CatalogDownloadMetadataTypes implements KarmaMetadataType {
		USER_CATALOG_DOWNLOAD

	}

}
