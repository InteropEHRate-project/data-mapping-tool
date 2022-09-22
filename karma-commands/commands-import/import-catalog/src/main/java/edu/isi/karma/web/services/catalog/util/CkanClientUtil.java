package edu.isi.karma.web.services.catalog.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import eu.trentorise.opendata.jackan.CheckedCkanClient;
import eu.trentorise.opendata.jackan.CkanClient;
import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Danish
 * @date 2020/07/01
 */
public class CkanClientUtil {

	private static final Logger logger = LoggerFactory.getLogger(CkanClientUtil.class);
	private static CkanClientUtil INSTANCE;

	private CkanClientUtil() {
	}

	/**
	 * get singleton instance of the this class
	 *
	 * @return an instance of {@link CkanClientUtil}
	 */
	public static CkanClientUtil getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new CkanClientUtil();
		}
		return INSTANCE;
	}

	/**
	 * convert string to list of strings.
	 *
	 * @param selectedResourceIds serialized list of strings
	 * @return deserialized list of list of strings
	 */
	private static List<String> getStringToList(String selectedResourceIds) {
		if (Strings.isNullOrEmpty(selectedResourceIds))
			return null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(selectedResourceIds, new TypeReference<List<String>>() {
			});
		} catch (IOException e) {
			logger.info("Unable to convert string to list: " + selectedResourceIds + " Assuming it contains only one resource");
			return Collections.singletonList(selectedResourceIds);
		}
	}

	/**
	 * get the list of resources associated to given CKAN dataset for particular client.
	 *
	 * @param apiUrl       (mandatory) url to the ckan catalog instance
	 * @param apiKey       (if not available, set to null or empty) user key to access private datasets
	 * @param datasetId    (mandatory) dataset/package id to get its resources
	 * @param resourceList (mandatory) a serialized list of ids or single string to get selected resources
	 * @return list of resources or null if non found.
	 */
	public List<CkanResource> getCkanResources(String apiUrl, String apiKey, String datasetId, String resourceList) {
		List<CkanResource> selectedRes = null;

		if (!Strings.isNullOrEmpty(datasetId) && !Strings.isNullOrEmpty(resourceList)) {
			CkanDataset dataset = getCkanDataset(apiUrl, apiKey, datasetId);
			List<String> resourceIds = getStringToList(resourceList);
			if (dataset != null && resourceIds != null) {
				for (CkanResource ckanResource : dataset.getResources()) {
					for (String id : resourceIds) {
						if (ckanResource.getId().equals(id)) {
							if (selectedRes == null)
								selectedRes = new LinkedList<>();
							selectedRes.add(ckanResource);
						}
					}
				}
			}
		}
		return selectedRes;
	}

	/**
	 * get ckan dataset/package for a given user.
	 *
	 * @param apiUrl    (mandatory) catalog url
	 * @param apiKey    (if not available, set to null or empty) user key for catalog
	 * @param datasetId (mandatory) id for the dataset to retrieve
	 * @return null if fails or {@link CkanDataset}
	 */
	public CkanDataset getCkanDataset(String apiUrl, String apiKey, String datasetId) {

		CkanClient ckanClient = Strings.isNullOrEmpty(apiUrl) ? null : Strings.isNullOrEmpty(apiKey) ?
			new CkanClient(apiUrl) : new CheckedCkanClient(apiUrl, apiKey);
		if (ckanClient == null) {
			logger.error("Unable to connect to the external Ckan Instance: " + apiUrl);
			return null;
		}

		return ckanClient.getDataset(datasetId);
	}
}
