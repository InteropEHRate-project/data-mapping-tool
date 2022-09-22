package edu.isi.karma.webserver.rest;

import edu.isi.karma.web.services.catalog.rep.CatalogDataset;
import edu.isi.karma.web.services.catalog.rep.CatalogResource;
import edu.isi.karma.web.services.catalog.rep.CatalogsDownloadManager;
import edu.isi.karma.webserver.KarmaException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Danish
 */
@Path("/download")
public class CatalogDownloadHandler {

	private static final Logger logger = LoggerFactory.getLogger(CatalogDownloadHandler.class);
	CatalogsDownloadManager catalogsDownloadManager = CatalogsDownloadManager.getInstance();

	/**
	 * @param resourceName
	 * @param directory
	 * @return
	 */
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/cancel")
	public Response cancelDownload(@FormParam("resourceName") String resourceName,
								   @FormParam("datasetDirectoryName") String directory) {
		try {
			return Response.status(200).entity(
				catalogsDownloadManager.cancelResourceDownload(resourceName, directory)
			).build();
		} catch (Exception e) {
			logger.error("Error canceling resource download", e);
			return Response.serverError().build();
		}
	}

	/**
	 * @param fetchAll
	 * @return
	 */
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/fetch")
	public Response fetchDownload(@FormParam("fetchAll") String fetchAll) {
		try {
			Boolean isFetchAll = Boolean.valueOf(fetchAll);

			JSONArray responseDatasets = new JSONArray();
			boolean isAllDownloaded = true;

			try {
				List<CatalogDataset> datasets = isFetchAll ?
					catalogsDownloadManager.getAllDownloadDatasets() :
					catalogsDownloadManager.getActiveDownloadDatasets();

				for (CatalogDataset dataset : datasets) {
					boolean isDatasetDownloaded = true;
					JSONObject responseDataset = new JSONObject();
					JSONArray catalogRes = new JSONArray();
					if (dataset.getDownloadStartedResources().size() == 0)
						continue;
					for (CatalogResource resource : dataset.getDownloadStartedResources()) {
						JSONObject res = new JSONObject();
						res.put("name", resource.getName());
						res.put("isError", resource.isErrorWhileDownloading());
						res.put("isCanceled", resource.isDownloadCanceled());
						res.put("isResourceDownloaded", resource.isDownloadCompleted());
						if (!resource.isDownloadCompleted()
							&& !resource.isDownloadCanceled()
							&& !resource.isErrorWhileDownloading())
							isDatasetDownloaded = false;
						res.put("currentSize", resource.getCurrentSize());
						res.put("finalSize", resource.getTotalSize());
						catalogRes.put(res);
					}
					responseDataset.put("isDatasetDownloaded", isDatasetDownloaded);
					if (!isDatasetDownloaded) isAllDownloaded = false;
					responseDataset.put("uniqueName", dataset.getDirectoryName());
					responseDataset.put("resources", catalogRes);
					responseDatasets.put(responseDataset);
				}
			} catch (KarmaException e) {
				logger.error("Error while getting info for download progress", e.getMessage());
				e.printStackTrace();
			}
			JSONObject result = new JSONObject();
			result.put("isAllDownloaded", isAllDownloaded);
			result.put("datasets", responseDatasets);

			return Response.status(200).entity(result.toString()).build();
		} catch (Exception e) {
			logger.error("Error canceling resource download", e);
			return Response.serverError().build();
		}
	}

}
