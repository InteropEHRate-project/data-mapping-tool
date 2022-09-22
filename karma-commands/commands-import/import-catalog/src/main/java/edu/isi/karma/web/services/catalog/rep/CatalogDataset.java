package edu.isi.karma.web.services.catalog.rep;

import com.google.common.base.Strings;
import edu.isi.karma.webserver.KarmaException;
import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * dataset class for Ckan to handle the download request for resources.
 *
 * @author Danish
 * @date 2020/06/20
 */
public class CatalogDataset {
	private static final Logger logger = LoggerFactory.getLogger(CatalogDataset.class);
	/**
	 * list of resources which has successfully started the download.
	 */
	private final List<CatalogResource> downloadStartedResources;
	/**
	 * ckan dataset/package instance
	 */
	private CkanDataset selectedCkanDataset;
	/**
	 * list of dataset/package resource tobe downloaded
	 */
	private List<CkanResource> selectedCkanResources;
	/**
	 * directory where resource are downloaded
	 */
	private String directoryPath;
	/**
	 * api key used for auth while downloading private resources.
	 */
	private String apiKey;

	public CatalogDataset() {
		this.downloadStartedResources = new LinkedList<>();
	}

	/**
	 * constructor to initialize the method
	 *
	 * @param selectedCkanDataset   ckan dataset object
	 * @param selectedCkanResources list of ckan resources to be downloaded. each resource should belong to the ckan dataset.
	 */
	public CatalogDataset(CkanDataset selectedCkanDataset, List<CkanResource> selectedCkanResources, String apiKey) {
		this.selectedCkanDataset = selectedCkanDataset;
		this.selectedCkanResources = selectedCkanResources;
		this.downloadStartedResources = new LinkedList<>();
		this.apiKey = apiKey;
	}

	/**
	 * returns the name of the directory where resources are downloaded.
	 *
	 * @return
	 */
	public String getDirectoryName() throws KarmaException {
		if (Strings.isNullOrEmpty(this.directoryPath))
			throw new KarmaException("target download directory is not defined or missing");
		return (new File(this.directoryPath)).getName();
	}

	/**
	 * get a list of {@link CkanResource} objects where the download was started in a new thread.
	 * will also include all the resources where download is completed.
	 *
	 * @return empty of populated list, never null.
	 */
	public List<CatalogResource> getDownloadStartedResources() {
		return this.downloadStartedResources;
	}

	/**
	 * start the download of resources related to this catalogDataset object
	 *
	 * @return true if download started successfully, false otherwise
	 */
	protected boolean startDownload() {
		if (Strings.isNullOrEmpty(CatalogsDownloadManager.getInstance().getDownloadDirectory())) {
			logger.error("path to the download directory must be set properly using CkanService singleton");
			return false;
		}

		int MAX_T = 5;
		ExecutorService pool = Executors.newFixedThreadPool(MAX_T);
		boolean isStarted = false;

		if (this.selectedCkanResources != null
			&& this.selectedCkanDataset != null
			&& !this.selectedCkanResources.isEmpty()) {
			Thread.currentThread().setName("Thread-Download-DataSet-" + selectedCkanDataset.getName());
			this.selectedCkanDataset.getResources().forEach(resource -> {

				// check if resource belong to the same dataset
				boolean notContain = true;
				for (CkanResource selectedResource : this.selectedCkanResources)
					if (resource.getId().equals(selectedResource.getId()))
						notContain = false;
				if (notContain)
					return;

				if (this.directoryPath == null)
					this.directoryPath = getDirectoryAbsolutePath();
				String fileName = Strings.isNullOrEmpty(resource.getName())
					? ""
					: resource.getName().toLowerCase().replace(" ", "_");
				String format = Strings.isNullOrEmpty(resource.getName())
					? ""
					: "." + resource.getFormat().toLowerCase();

				// if format already appended with resource name
				int formatInd = fileName.lastIndexOf(".");
				if (formatInd > 0 && format.equals(fileName.substring(formatInd)))
					format = "";

				if (Strings.isNullOrEmpty(fileName)) {
					logger.error("no file name found for resource" + resource.toString());
					return;
				}

				String toAbsolutePath = this.directoryPath
					+ "/"
					+ fileName
					+ format;
				DownloadResourceProcess downloadResourceProcess = new DownloadResourceProcess(resource, toAbsolutePath, this.apiKey);
				this.downloadStartedResources.add(downloadResourceProcess.getResource());
				pool.execute(downloadResourceProcess);
			});
			isStarted = true;
		}
		waitForThreads(pool);

		return isStarted;
	}

	/**
	 * @param resourceName resource for which we want to cancel the download.
	 * @return try in the case of cancel successful, false otherwise.
	 */
	protected boolean cancelDownload(String resourceName) {
		boolean returnValue = false;

		for (CatalogResource r : getDownloadStartedResources()) {
			if (r.getName().equals(resourceName)) {
				returnValue = r.cancelDownload();
			}
		}
		return returnValue;
	}

	/**
	 * get a flag as true if the download for any of the resources belonging to this dataset is still active.
	 * get a flag as false if the download for all of
	 * the resources belonging to this dataset is either complete of ended with error.
	 *
	 * @return boolean
	 */
	protected boolean isDownloadActive() {
		if (getDownloadStartedResources().isEmpty()) return false;
		for (CatalogResource r : getDownloadStartedResources()) {
			if (!r.isDownloadCompleted()
				&& !r.isErrorWhileDownloading()
				&& !r.isDownloadCanceled())
				return true;
		}
		return false;
	}

	/**
	 * @return true if download never started successfully for any of the resources.
	 */
	protected boolean isErrorWhileDownloading() {
		if (getDownloadStartedResources().isEmpty()) return false;
		for (CatalogResource r : getDownloadStartedResources()) {
			if (!r.isErrorWhileDownloading()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * get the absolute path of the directory where resources will be downloaded.
	 * path ends with the name of this dataset.
	 * in the case of same directory exist it will create a new directory by increasing the integer at the end.
	 * <p>
	 * Example of returns:
	 * if directory doesn't exist: ~/karma/name-of-dataset
	 * if directory already exist: ~/karma/name-of-dataset_1
	 *
	 * @return
	 */
	private String getDirectoryAbsolutePath() {
		String absolutePath = CatalogsDownloadManager.getInstance().getDownloadDirectory()
			+ this.selectedCkanDataset.getName().toLowerCase().replace(" ", "_");
		int i = 1;
		while ((new File(absolutePath)).isDirectory()) {
			absolutePath = CatalogsDownloadManager.getInstance().getDownloadDirectory()
				+ this.selectedCkanDataset.getName().toLowerCase().replace(" ", "_").concat("_" + i++);
		}
		return absolutePath;
	}

	/**
	 * wait for all the pool threads to end.
	 *
	 * @param pool
	 */
	private void waitForThreads(ExecutorService pool) {
		try {
			pool.shutdown();
			pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			logger.error("tasks interrupted");
		} finally {
			pool.shutdownNow();
		}
	}
}
