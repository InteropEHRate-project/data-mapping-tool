package edu.isi.karma.web.services.catalog.rep;

import edu.isi.karma.webserver.KarmaException;
import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanResource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;

class CatalogDownloadManagerTest {
	CatalogDataset nullDataset;

	@TempDir
	File tempDir;

	@BeforeEach
	void setUp() throws KarmaException {
		CatalogsDownloadManager.getInstance().setDownloadDirectory(tempDir.getAbsolutePath());
		nullDataset = CatalogsDownloadManager.getInstance()
			.addNewDatasetForDownload(null, null, null);
	}

	@AfterEach
	void tearDown() {
		CatalogsDownloadManager.resetInstance();
	}

	@Test
	void getInstanceTest() {
		Assertions.assertNotNull(CatalogsDownloadManager.getInstance(),
			"this method should always return a singleton of CatalogDownloadManager");
	}

	@Test
	void addNewDatasetForDownloadTest() throws KarmaException {
		Assertions.assertNotNull(nullDataset);

		// Mockito CkanDataset object
		CkanResource r1 = Mockito.spy(new CkanResource());
		CkanResource r2 = Mockito.spy(new CkanResource());
		CkanDataset ckanDataset = Mockito.spy(new CkanDataset());

		CatalogDataset dataset = CatalogsDownloadManager.getInstance()
			.addNewDatasetForDownload(ckanDataset, Arrays.asList(r1, r2),"");
		Assertions.assertNotNull(dataset);
	}

	@Test
	void startDownloadTest() {
		Assertions.assertFalse(CatalogsDownloadManager.getInstance().startDatasetDownload(nullDataset));

		CatalogDataset catalogDataset = Mockito.spy(new CatalogDataset());
		Mockito.when(catalogDataset.isDownloadActive()).thenReturn(true);
		Assertions.assertTrue(CatalogsDownloadManager.getInstance().startDatasetDownload(catalogDataset));
	}

	@Test
	void getAllDownloadDatasetsTest() {
		Assertions.assertTrue(CatalogsDownloadManager.getInstance().getAllDownloadDatasets().isEmpty(),
			"no catalog dataset will be added to list until download starts.");
		Assertions.assertFalse(CatalogsDownloadManager.getInstance().startDatasetDownload(nullDataset));
		Assertions.assertTrue(CatalogsDownloadManager.getInstance().getAllDownloadDatasets().isEmpty(),
			"no catalog dataset will be added to list until download start successfully.");

		CatalogDataset catalogDataset = Mockito.spy(new CatalogDataset());
		Mockito.when(catalogDataset.isDownloadActive()).thenReturn(true);
		Assertions.assertTrue(CatalogsDownloadManager.getInstance().startDatasetDownload(catalogDataset));

		Assertions.assertFalse(CatalogsDownloadManager.getInstance().getAllDownloadDatasets().isEmpty(),
			"started datasets must be returned in the list download start successfully.");
		Assertions.assertEquals(CatalogsDownloadManager.getInstance().getAllDownloadDatasets().size(), 1,
			"started datasets must be returned in the list download start successfully.");
	}

	@Test
	void getActiveDownloadDatasetsTest() {

		Assertions.assertTrue(CatalogsDownloadManager.getInstance().getActiveDownloadDatasets().isEmpty(),
			"no catalog dataset will be added to list until download starts.");
		Assertions.assertFalse(CatalogsDownloadManager.getInstance().startDatasetDownload(nullDataset));

		CatalogDataset d1 = Mockito.spy(new CatalogDataset());
		Mockito.when(d1.isDownloadActive()).thenReturn(true);
		CatalogDataset d2 = Mockito.spy(new CatalogDataset());
		Mockito.when(d2.isDownloadActive()).thenReturn(true);
		CatalogsDownloadManager catalogsDownloadManager = Mockito.spy(CatalogsDownloadManager.getInstance());

		Assertions.assertTrue(CatalogsDownloadManager.getInstance().getActiveDownloadDatasets().isEmpty(),
			"no catalog dataset will be added to list until download start successfully.");

		LinkedList<CatalogDataset> l = new LinkedList<>();
		l.add(d1);
		l.add(d2);
		Mockito.when(catalogsDownloadManager.getAllDownloadDatasets()).thenReturn(l);

		Assertions.assertFalse(catalogsDownloadManager.getActiveDownloadDatasets().isEmpty(),
			"started datasets must be returned in the list download start successfully.");
		Assertions.assertEquals(catalogsDownloadManager.getActiveDownloadDatasets().size(), 2,
			"started datasets must be returned in the list download start successfully.");

		//wait for download to complete
		Mockito.when(d1.isDownloadActive()).thenReturn(false);
		Mockito.when(d2.isDownloadActive()).thenReturn(false);

		Assertions.assertTrue(catalogsDownloadManager.getActiveDownloadDatasets().isEmpty(),
			"started datasets must be returned in the list download start successfully.");
	}

	@Test
	@DisplayName("Test the catalog download directory variable")
	void getAndSetDownloadDirectoryTest() {
		CatalogsDownloadManager.resetInstance();
		Assertions.assertNull(CatalogsDownloadManager.getInstance().getDownloadDirectory(),
			"on fresh start must be null");
		CatalogsDownloadManager.getInstance().setDownloadDirectory("directory");
		Assertions.assertNotNull(CatalogsDownloadManager.getInstance().getDownloadDirectory(),
			"after setting value must not be null");
		Assertions.assertEquals(CatalogsDownloadManager.getInstance().getDownloadDirectory(), "directory",
			"value must match with the previously set value");
		CatalogsDownloadManager.getInstance().setDownloadDirectory(null);
		Assertions.assertNull(CatalogsDownloadManager.getInstance().getDownloadDirectory(),
			"must be null after setting to null");
	}

	@Test
	void resetInstanceTest() {
		CatalogsDownloadManager.resetInstance();
		Assertions.assertTrue(CatalogsDownloadManager.getInstance().getAllDownloadDatasets().isEmpty());
		Assertions.assertTrue(CatalogsDownloadManager.getInstance().getActiveDownloadDatasets().isEmpty());
		Assertions.assertNull(CatalogsDownloadManager.getInstance().getDownloadDirectory());
	}

}
