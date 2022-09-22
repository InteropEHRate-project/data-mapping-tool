package edu.isi.karma.web.services.catalog.rep;

import com.google.common.base.Strings;
import edu.isi.karma.webserver.KarmaException;
import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Download manager to keep track of the progress of dataset and resources download.
 *
 * @author Danish
 * @date 2020/06/20
 */
public class CatalogsDownloadManager {

	private static final Logger logger = LoggerFactory.getLogger(CatalogsDownloadManager.class);
	private static CatalogsDownloadManager INSTANCE;
	/**
	 * list of datasets/packages where the download is started successfully
	 */
	private final LinkedList<CatalogDataset> downloadDatasets;
	/**
	 * absolute path to the download directory for catalogs
	 */
	private String downloadDirectory;

	/**
	 * constructor used to crate singleton
	 */
	private CatalogsDownloadManager() {
		this.downloadDatasets = new LinkedList<>();
		this.downloadDirectory = null;
	}

	/**
	 * get singleton instance of the this class
	 *
	 * @return an instance of {@link CatalogsDownloadManager}
	 */
	public static CatalogsDownloadManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new CatalogsDownloadManager();
		}
		return INSTANCE;
	}

	/**
	 * reinitialized the instance.
	 */
	public static void resetInstance() {
		INSTANCE = new CatalogsDownloadManager();
	}

	/**
	 * should always be used to create new Ckan dataset/package download request.
	 * used to keep track of all downloads at one place.
	 *
	 * @param selectedCkanDataset   (mandatory) ckan package/dataset to be downloaded
	 * @param selectedCkanResources (mandatory) ckan resources related to this dataset, selected for download.
	 * @return always return an instance of {@link CatalogDataset}, never null.
	 */
	public CatalogDataset addNewDatasetForDownload(CkanDataset selectedCkanDataset,
												   List<CkanResource> selectedCkanResources,
												   String apiKey) throws KarmaException {
		if (Strings.isNullOrEmpty(this.downloadDirectory))
			throw new KarmaException("download directory is not set properly");
		return new CatalogDataset(selectedCkanDataset, selectedCkanResources, apiKey);
	}

	/**
	 * return all the list of {@link CatalogDataset} where download is started.
	 * it includes both active and already downloaded datasets.
	 *
	 * @return empty or populated list, never null
	 */
	public LinkedList<CatalogDataset> getAllDownloadDatasets() {
		return this.downloadDatasets;
	}

	/**
	 * return list of datasets/packages who's downloads are still active
	 *
	 * @return empty or populated list, never null
	 */
	public LinkedList<CatalogDataset> getActiveDownloadDatasets() {
		LinkedList<CatalogDataset> active = new LinkedList<>();
		getAllDownloadDatasets().forEach(catalog -> {
			if (catalog.isDownloadActive()) active.addFirst(catalog);
		});
		return active;
	}

	/**
	 * start the download for given package/dataset.
	 *
	 * @param dataset dataset for which to start the download
	 * @return true if started successfully
	 */
	public boolean startDatasetDownload(CatalogDataset dataset) {
		new Thread(dataset::startDownload).start();
		while (!dataset.isDownloadActive()) {
			if (dataset.isErrorWhileDownloading())
				return false;
		}
		getAllDownloadDatasets().addFirst(dataset);
		logger.info("download started successfully");
		return true;
	}

	/**
	 * @return null or path to directory set by user class.
	 */
	public String getDownloadDirectory() {
		return this.downloadDirectory;
	}

	/**
	 * set the catalog download home directory
	 *
	 * @param ckanDownloadDirectory absolute directory path where all the datasets and resources will be downloaded.
	 */
	public void setDownloadDirectory(String ckanDownloadDirectory) {
		this.downloadDirectory = ckanDownloadDirectory;
	}

	/**
	 * use to cancel the download for specific resource
	 *
	 * @param resourceName name of the resource for which download will be canceled
	 * @param directory    name of the directory in which resource is being downloaded
	 * @return true is the download stop successfully.
	 */
	public boolean cancelResourceDownload(String resourceName, String directory) {
		for (CatalogDataset dataset : getAllDownloadDatasets()) {
			try {
				if (directory.equals(dataset.getDirectoryName())) {
					return dataset.cancelDownload(resourceName);
				}
			} catch (KarmaException e) {
				e.printStackTrace();
			}
		}
		logger.error("ckan resource stop: UNSUCCESSFUL");
		return false;
	}

}
