package edu.isi.karma.web.services.catalog.rep;

import eu.trentorise.opendata.jackan.model.CkanResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A runnable class used to handel multiple thread to download multiple catalog resources.
 *
 * @author Danish
 * @date 2020/06/20
 */
public class DownloadResourceProcess implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(DownloadResourceProcess.class);
	private final CatalogResource resource;

	public DownloadResourceProcess(CkanResource resource, String absoluteResourcePath, String apiKey) {
		this.resource = new CatalogResource(resource, absoluteResourcePath, apiKey);
	}

	/**
	 * Runnable override method to be executed in a new thread.
	 * It starts the download of the resource.
	 */
	@Override
	public void run() {
		logger.info("thread started for download: " + this.resource.getName());
		Thread.currentThread().setName("Thread-Download-Resource-" + this.resource.getName());
		this.resource.startDownload();
	}

	/**
	 * @return resource associated to this process
	 */
	public CatalogResource getResource() {
		return resource;
	}

}
