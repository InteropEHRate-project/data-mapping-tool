package edu.isi.karma.web.services.catalog.rep;

import com.google.common.base.Strings;
import eu.trentorise.opendata.jackan.model.CkanResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * A class to handle the downloading of the resource for catalog/package/dataset.
 *
 * @author Danish
 * @date 2020/06/20
 */
public class CatalogResource {
	private static final Logger logger = LoggerFactory.getLogger(CatalogResource.class);
	/**
	 * resource value
	 */
	private CkanResource resource;
	/**
	 * resource URL
	 */
	private URL fromURL;
	/**
	 * new file where info will be downloaded
	 */
	private File toFile;
	/**
	 * absolute path where the file will be downloaded
	 */
	private String toPath;
	/**
	 * flag to check if download is complete
	 */
	private boolean isDownloadCompleted;
	/**
	 * flag to keep track in the case of error while downloading resource
	 */
	private boolean isError;
	/**
	 * keep the total size of the file;
	 */
	private long totalFileSize;
	/**
	 * is download canceled bny external user/method.
	 */
	private boolean isDownloadCanceled;
	/**
	 * use to track the download
	 */
	private ReadableByteChannel rbcObj = null;
	/**
	 * used for download and cancellation of the resource.
	 */
	private FileOutputStream fOutStream = null;
	/**
	 * api key used for auth while downloading private resources.
	 */
	private String apiKey;

	public CatalogResource() {
		this.isDownloadCompleted = false;
		this.isError = false;
		this.totalFileSize = -1;
	}


	/**
	 * constructor used to create a new object of this class
	 *
	 * @param resource resource instance for this object
	 * @param toPath   path where the file will be downloaded
	 */
	public CatalogResource(CkanResource resource, String toPath, String apiKey) {
		this.resource = resource;
		this.toFile = createNewFile(toPath);
		this.toPath = toPath;
		this.isDownloadCompleted = false;
		this.isError = false;
		this.totalFileSize = -1;
		this.apiKey = apiKey;
	}

	/**
	 * get the current size of the resources.
	 * can be used to keep track of the download.
	 *
	 * @return return -1 if file is null.
	 */
	public long getCurrentSize() {
		if (this.toFile != null) {
			return this.toFile.length();
		}
		return -1;
	}

	/**
	 * get the size of the resource.
	 * -1 if no size found.
	 *
	 * @return long
	 **/
	public long getTotalSize() {
		return this.totalFileSize;
	}

	/**
	 * @return get the absolute path to the resource in the backend where resource is being downloaded
	 */
	public String getAbsolutePath() {
		return this.toFile.getAbsolutePath();
	}

	/**
	 * @return get the name of the file
	 */
	public String getName() {
		return this.toFile.getName();
	}

	/**
	 * a boolean flag set to true in case of download completed successful.
	 *
	 * @return boolean
	 */
	public boolean isDownloadCompleted() {
		return this.isDownloadCompleted;
	}

	/**
	 * a boolean flag set to true in case of exceptions during download.
	 *
	 * @return boolean
	 */
	public boolean isErrorWhileDownloading() {
		return isError;
	}


	/**
	 * @return true if download was canceled, false otherwise
	 */
	public boolean isDownloadCanceled() {
		return this.isDownloadCanceled;
	}

	/**
	 * start the download for this resource
	 */
	protected void startDownload() {
		try {
			if (this.resource == null)
				throw new NullPointerException("No Resource found");
			this.totalFileSize = getTotalResourceSize();
			this.fromURL = createNewUrl(this.resource.getUrl());
			URLConnection connection = this.fromURL.openConnection();
			if (!Strings.isNullOrEmpty(this.apiKey)) {
				connection.setRequestProperty("Authorization", this.apiKey);
			}
			connection.connect();

			rbcObj = Channels.newChannel(connection.getInputStream());
			fOutStream = new FileOutputStream(this.toPath);

			fOutStream.getChannel().transferFrom(rbcObj, 0, Long.MAX_VALUE);

			if (!this.isDownloadCanceled)
				this.isDownloadCompleted = true;

		} catch (MalformedURLException e) {
			this.isError = true;
			logger.error("Malformed URL for catalog resource download. Provided URL was equal to = " + fromURL
				+ ". Fix this before restarting the download");
		} catch (IOException e) {
			this.isError = true;
			logger.error("Error while downloading the resource from URL= " + this.fromURL.toString());
			e.printStackTrace();
		} catch (NullPointerException e) {
			this.isError = true;
			logger.error(e.getMessage());
		} finally {
			try {
				if (fOutStream != null) {
					fOutStream.close();
				}
				if (rbcObj != null) {
					rbcObj.close();
				}
			} catch (IOException ioExObj) {
				logger.error("Problem Occurred While Closing The Object= " + ioExObj.getMessage());
			}
		}
	}

	/**
	 * method to cancel the download and reset the downloaded file to zero.
	 *
	 * @return true is done successfully
	 */
	protected boolean cancelDownload() {
		try {
			if (fOutStream != null) {
				fOutStream.close();
				logger.info("download stopped successfully");
			}
			if (rbcObj != null) {
				rbcObj.close();
				logger.info("download object closed successfully");
			}
			this.isDownloadCanceled = true;
			//reset the file size
			this.toFile = createNewFile(this.toPath);
			return true;
		} catch (IOException ioExObj) {
			logger.error("Problem Occurred While Closing The Object= " + ioExObj.getMessage());
			return false;
		}
	}

	/**
	 * try to get the total size of the resource tobe downloaded
	 * 1st try with ckan resource getSize method.
	 * 2nd try wif attached with http header.
	 *
	 * @return -1 if fails or positive long if successful
	 */
	private long getTotalResourceSize() {
		String s = resource.getSize();
		if (!Strings.isNullOrEmpty(s))
			return Long.parseLong(s);
		if (this.fromURL != null) {
			HttpURLConnection conn;
			try {
				conn = (HttpURLConnection) this.fromURL.openConnection();
				conn.setRequestMethod("HEAD");
				conn.getInputStream();
				long size = conn.getContentLength();
				conn.getInputStream().close();
				return size;
			} catch (IOException e) {
				logger.info("No total file size attached to Content Length of the file URL");
				return -1;
			}
		} else {
			return -1;
		}
	}

	/**
	 * create new file or reset the existing where resource will be downloaded.
	 *
	 * @return file object
	 */
	private File createNewFile(String path) {
		File newFile = null;
		if (!Strings.isNullOrEmpty(path)) {
			newFile = new File(path);
			if (newFile.exists()) {
				if (!newFile.delete()) {
					logger.error("Error deleting file " + newFile.getAbsolutePath());
				}
			}
			try {
				if (!newFile.getParentFile().mkdirs()) {
					logger.info("unable to creat new directory, it may already exist");
				}
				if (!newFile.createNewFile())
					logger.info("unable to creat new file, it may already exist");
			} catch (IOException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}
		}
		return newFile;
	}

	/**
	 * create a {@link URL} from a string
	 *
	 * @param url in string format
	 * @return {@link URL}
	 * @throws MalformedURLException
	 */
	private URL createNewUrl(String url) throws MalformedURLException {
		return new URL(url);
	}
}
