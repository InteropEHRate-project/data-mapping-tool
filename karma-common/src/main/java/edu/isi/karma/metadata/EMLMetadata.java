package edu.isi.karma.metadata;

import edu.isi.karma.webserver.KarmaException;
import edu.isi.karma.webserver.ServletContextParameterMap;

public class EMLMetadata extends KarmaPublishedMetadata{

	public EMLMetadata(ServletContextParameterMap contextParameters) throws KarmaException
	{
		super(contextParameters);
	}

	@Override
	protected ServletContextParameterMap.ContextParameter getDirectoryContextParameter() {
		return ServletContextParameterMap.ContextParameter.EML_PUBLISH_DIR;
	}

	@Override
	protected ServletContextParameterMap.ContextParameter getRelativeDirectoryContextParameter() {
		return ServletContextParameterMap.ContextParameter.EML_PUBLISH_RELATIVE_DIR;
	}

	@Override
	protected String getDirectoryPath() {
		return "EML/";
	}

	@Override
	public KarmaMetadataType getType() {
		return StandardPublishMetadataTypes.EML;
	}
}
