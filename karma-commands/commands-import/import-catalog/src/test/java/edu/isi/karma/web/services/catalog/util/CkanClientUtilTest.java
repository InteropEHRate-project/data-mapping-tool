package edu.isi.karma.web.services.catalog.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.isi.karma.web.services.catalog.rep.CatalogImportTestUtil;
import edu.isi.karma.web.services.catalog.rep.CatalogsDownloadManager;
import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanResource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class CkanClientUtilTest {

	@TempDir
	File tempDir;

	@AfterEach
	void tearDown() {
		CatalogsDownloadManager.resetInstance();
	}

	@Test
	void getInstance() {
		Assertions.assertNotNull(CkanClientUtil.getInstance());
	}

	@Test
	@DisplayName("Test fetching ckan resources from a ckan instance for a given package/dataset")
	void getCkanResourcesTest() throws IOException {
		// return null for empty input
		List<CkanResource> emptyResources = CkanClientUtil.getInstance().getCkanResources("", "", "", "");
		Assertions.assertNull(emptyResources);

		// return null for null input
		List<CkanResource> nullResources = CkanClientUtil.getInstance().getCkanResources(null, null, null, null);
		Assertions.assertNull(nullResources);

		//temp ckanDataset
		CkanDataset ckanDataset = CatalogImportTestUtil.getTempCkanDataset("tempName", "title", "tempID");
		CkanResource r1 = CatalogImportTestUtil.getTempCkanResource("file21", "id21", tempDir, "txt", 1000000);
		CkanResource r2 = CatalogImportTestUtil.getTempCkanResource("file22", "id22", tempDir, "csv", 1000000);
		ckanDataset.setResources(Arrays.asList(r1, r2));

		//get a spy object to change method behaviour
		CkanClientUtil m = Mockito.spy(CkanClientUtil.getInstance());
		Mockito.when(m.getCkanDataset("", null, "fakeId")).thenReturn(ckanDataset);

		//stringify list of resource ids
		final StringWriter sw = new StringWriter();
		final ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(sw, Collections.singletonList(r1.getId()));

		// return not null for if successful retrieval of data.
		List<CkanResource> resources = m.getCkanResources("", null, "fakeId", sw.toString());
		Assertions.assertNotNull(resources);

	}

	@Test
	@DisplayName("get Ckan dataset object providing api URL, Key and ID")
	void getCkanDatasetTest() {
		CkanDataset emptyDataset = CkanClientUtil.getInstance().getCkanDataset("", "", "");
		Assertions.assertNull(emptyDataset);
		CkanDataset nullDataset = CkanClientUtil.getInstance().getCkanDataset(null, null, null);
		Assertions.assertNull(nullDataset);
	}

}
