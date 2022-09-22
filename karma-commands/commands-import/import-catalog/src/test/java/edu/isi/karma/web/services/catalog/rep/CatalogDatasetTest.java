package edu.isi.karma.web.services.catalog.rep;

import edu.isi.karma.webserver.KarmaException;
import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.*;
import java.util.Arrays;

//TODO: use Mockito to have better control on the tests.
class CatalogDatasetTest {
	CatalogDataset nullDataset;

	@TempDir
	File tempDir;

	@BeforeEach
	void setUp() {
		Assertions.assertTrue(this.tempDir.isDirectory(), "Should be a directory ");
		nullDataset = new CatalogDataset(null, null, null);
		Assertions.assertNotNull(nullDataset);
	}

	@AfterEach
	void tearDown() {
		CatalogsDownloadManager.resetInstance();
	}

	@Test
	void startDownloadTest() {

		//temp resources to be downloaded
		CkanResource tempCkanResource1 = CatalogImportTestUtil.getTempCkanResource("file1", "id1", tempDir, "txt", 1000000);
		CkanResource tempCkanResource2 = CatalogImportTestUtil.getTempCkanResource("file2", "id2", tempDir, "csv", 1000000);
		//temp ckan dataset instance to be downloaded
		CkanDataset tempCkanDataset = CatalogImportTestUtil.getTempCkanDataset("tempName1", "tempDataset1", "tempID1");
		//populate the resource list
		tempCkanDataset.setResources(Arrays.asList(tempCkanResource1, tempCkanResource2));
		CatalogDataset catalogDataset = new CatalogDataset(tempCkanDataset, tempCkanDataset.getResources(), "");
		CatalogsDownloadManager.getInstance().setDownloadDirectory(tempDir.getAbsolutePath());


		Assertions.assertFalse(nullDataset.startDownload(),
			"CatalogDataset instance created with null values should never start the download process successfully");
		Assertions.assertTrue(catalogDataset.startDownload(),
			"if download started successfully it should return true");
	}

	@Test
	void startDownloadTest2() {

		//temp resources to be downloaded
		CkanResource tempCkanResource1 = CatalogImportTestUtil.getTempCkanResource("file11", "id11", tempDir, "txt", 1000000);
		CkanResource tempCkanResource2 = CatalogImportTestUtil.getTempCkanResource("file12", "id12", tempDir, "csv", 1000000);
		CkanResource tempCkanResource3 = CatalogImportTestUtil.getTempCkanResource("file13", "id13", tempDir, "csv", 1000000);
		//temp ckan dataset instance to be downloaded
		CkanDataset tempCkanDataset = CatalogImportTestUtil.getTempCkanDataset("tempName6", "tempDataset6", "tempID6");
		//populate the resource list
		tempCkanDataset.setResources(Arrays.asList(tempCkanResource1, tempCkanResource2));
		CatalogDataset catalogDataset = new CatalogDataset(tempCkanDataset, Arrays.asList(tempCkanResource1, tempCkanResource2, tempCkanResource3), "");
		CatalogsDownloadManager.getInstance().setDownloadDirectory(tempDir.getAbsolutePath());


		Assertions.assertTrue(catalogDataset.startDownload(),
			"if download started successfully it should return true");
		Assertions.assertEquals(catalogDataset.getDownloadStartedResources().size(), 2,
			"should only start download for resources which belong to the dataset");
		catalogDataset.getDownloadStartedResources().forEach(r -> {
			Assertions.assertNotEquals(r.getName(), tempCkanResource3.getName());
		});

	}

	/**
	 * private ckan dataset download testing
	 */
	@Test
	void startDownloadTest3() {

		//temp resources to be downloaded
		CkanResource tempCkanResource1 = CatalogImportTestUtil.getTempCkanResource("file111", "id111", tempDir, "txt", 1000000);
		CkanResource tempCkanResource2 = CatalogImportTestUtil.getTempCkanResource("file112", "id112", tempDir, "csv", 1000000);
		CkanResource tempCkanResource3 = CatalogImportTestUtil.getTempCkanResource("file113", "id113", tempDir, "csv", 1000000);
		//temp ckan dataset instance to be downloaded
		CkanDataset tempCkanDataset = CatalogImportTestUtil.getTempCkanDatasetPrivate("tempName16", "tempDataset16", "tempID16", "userID16");
		//populate the resource list
		tempCkanDataset.setResources(Arrays.asList(tempCkanResource1, tempCkanResource2));
		CatalogDataset catalogDataset = new CatalogDataset(tempCkanDataset, Arrays.asList(tempCkanResource1, tempCkanResource2, tempCkanResource3), "");
		CatalogsDownloadManager.getInstance().setDownloadDirectory(tempDir.getAbsolutePath());


		Assertions.assertTrue(catalogDataset.startDownload(),
			"if download started successfully it should return true");
		Assertions.assertEquals(catalogDataset.getDownloadStartedResources().size(), 2,
			"should only start download for resources which belong to the dataset");
		catalogDataset.getDownloadStartedResources().forEach(r -> {
			Assertions.assertNotEquals(r.getName(), tempCkanResource3.getName());
		});

	}

	@Test
	void cancelDownloadTest() {

		long sizeResource1 = 100 * 1000000;
		long sizeResource2 = 100 * 1000000;

		//temp resources to be downloaded
		CkanResource tempCkanResource1 = CatalogImportTestUtil
			.getTempCkanResource("file3", "id3", tempDir, "txt", sizeResource1);
		CkanResource tempCkanResource2 = CatalogImportTestUtil
			.getTempCkanResource("file4", "id4", tempDir, "csv", sizeResource2);
		//temp ckan dataset instance to be downloaded
		CkanDataset tempCkanDataset = CatalogImportTestUtil.getTempCkanDataset("tempName2", "tempDataset2", "tempID2");
		//populate the resource list
		tempCkanDataset.setResources(Arrays.asList(tempCkanResource1, tempCkanResource2));
		CatalogDataset catalogDataset = new CatalogDataset(tempCkanDataset, tempCkanDataset.getResources(),"");
		CatalogsDownloadManager.getInstance().setDownloadDirectory(tempDir.getAbsolutePath());

		//start download for dataset
		Assertions.assertTrue(catalogDataset.startDownload(),
			"if download started successfully it should return true");

		//wait for download to start for resource 1
		boolean flag = true;
		while (flag) {
			if (catalogDataset.getDownloadStartedResources().get(0).getCurrentSize() < 5)
				continue;
			flag = false;
		}

		// testing the method
		Assertions.assertTrue(catalogDataset.cancelDownload(catalogDataset.getDownloadStartedResources().get(0).getName()));

		Assertions.assertEquals(0, catalogDataset.getDownloadStartedResources().get(0).getCurrentSize(),
			"after the cancellation of the resource download current size must go back to zero");

		// wait for all the download to complete
		for (CatalogResource r : catalogDataset.getDownloadStartedResources()) {
			flag = true;
			while (flag) {
				if (!r.isDownloadCompleted()
					&& !r.isErrorWhileDownloading()
					&& !r.isDownloadCanceled())
					continue;
				flag = false;
			}
		}

		// test the results
		Assertions.assertEquals(0, catalogDataset.getDownloadStartedResources().get(0).getCurrentSize(),
			"after the cancellation of the resource download current size must go back to zero");
		Assertions.assertEquals(catalogDataset.getDownloadStartedResources().get(1).getCurrentSize(), sizeResource2);
	}

	@Test
	void isDownloadActiveTest() {

		// test for null CatalogDataset object
		Assertions.assertFalse(nullDataset.startDownload(),
			"CatalogDataset instance created with null values should never start the download process successfully");
		Assertions.assertFalse(nullDataset.isDownloadActive(),
			"CatalogDataset instance created with null values should never start the download process successfully");

		// Mockito CatalogDataset object
		CatalogResource r1 = Mockito.spy(new CatalogResource());
		Mockito.when(r1.isDownloadCompleted()).thenReturn(false);
		Mockito.when(r1.isDownloadCanceled()).thenReturn(false);
		Mockito.when(r1.isErrorWhileDownloading()).thenReturn(false);
		CatalogResource r2 = Mockito.spy(new CatalogResource());
		Mockito.when(r2.isDownloadCompleted()).thenReturn(false);
		Mockito.when(r2.isDownloadCanceled()).thenReturn(false);
		Mockito.when(r2.isErrorWhileDownloading()).thenReturn(false);

		CatalogDataset catalogDataset = Mockito.spy(new CatalogDataset());
		Mockito.when(catalogDataset.getDownloadStartedResources()).thenReturn(Arrays.asList(r1, r2));

		//test
		Assertions.assertTrue(catalogDataset.isDownloadActive(),
			"if download started successfully it should return true");

		//wait for download to complete
		Mockito.when(r1.isDownloadCompleted()).thenReturn(true);
		Mockito.when(r2.isDownloadCompleted()).thenReturn(true);

		//test
		Assertions.assertFalse(catalogDataset.isDownloadActive(),
			"if download completed successfully it should return false");
	}

	@Test
	void getDownloadStartedResourcesTest() {
		//temp resources to be downloaded
		CkanResource tempCkanResource1 = CatalogImportTestUtil.getTempCkanResource("file7", "id7", tempDir, "txt", 1000000);
		CkanResource tempCkanResource2 = CatalogImportTestUtil.getTempCkanResource("file8", "id8", tempDir, "csv", 1000000);
		//temp ckan dataset instance to be downloaded
		CkanDataset tempCkanDataset = CatalogImportTestUtil.getTempCkanDataset("tempName4", "tempDataset4", "tempID4");
		//populate the resource list
		tempCkanDataset.setResources(Arrays.asList(tempCkanResource1, tempCkanResource2));
		CatalogDataset catalogDataset = new CatalogDataset(tempCkanDataset, tempCkanDataset.getResources(),"");
		CatalogsDownloadManager.getInstance().setDownloadDirectory(tempDir.getAbsolutePath());

		// test for null CatalogDataset object
		Assertions.assertFalse(nullDataset.startDownload(),
			"CatalogDataset instance created with null values should never start the download process successfully");
		Assertions.assertTrue(nullDataset.getDownloadStartedResources().isEmpty(),
			"CatalogDataset instance created with null values should never start the download process successfully");

		// test for normal CatalogDataset object
		Assertions.assertTrue(catalogDataset.startDownload(),
			"if download started successfully it should return true");
		Assertions.assertFalse(catalogDataset.getDownloadStartedResources().isEmpty(),
			"once the download is started the list should not be empty");
		Assertions.assertEquals(catalogDataset.getDownloadStartedResources().size(), 2,
			"size to the list must be 2 in this case where test is downloading two resources");
		//wait for download to complete
		for (CatalogResource r : catalogDataset.getDownloadStartedResources()) {
			boolean flag = true;
			while (flag) {
				if (!r.isDownloadCompleted() && !r.isErrorWhileDownloading())
					continue;
				flag = false;
			}
		}
		Assertions.assertFalse(catalogDataset.getDownloadStartedResources().isEmpty(),
			"even if download is completed the list should not be empty");
		Assertions.assertEquals(catalogDataset.getDownloadStartedResources().size(), 2,
			"size to the list must be 2 in this case where test is downloading two resources");

	}

	@Test
	void getDirectoryNameTest() throws KarmaException {
		//temp resources to be downloaded
		CkanResource tempCkanResource1 = CatalogImportTestUtil.getTempCkanResource("file9", "id9", tempDir, "txt", 1000000);
		CkanResource tempCkanResource2 = CatalogImportTestUtil.getTempCkanResource("file10", "id10", tempDir, "csv", 1000000);
		//temp ckan dataset instance to be downloaded
		CkanDataset tempCkanDataset = CatalogImportTestUtil.getTempCkanDataset("tempName5", "tempDataset5", "tempID5");
		//populate the resource list
		tempCkanDataset.setResources(Arrays.asList(tempCkanResource1, tempCkanResource2));
		CatalogDataset catalogDataset = new CatalogDataset(tempCkanDataset, tempCkanDataset.getResources(),"");
		CatalogsDownloadManager.getInstance().setDownloadDirectory(tempDir.getAbsolutePath());

		Assertions.assertThrows(KarmaException.class, () -> nullDataset.getDirectoryName(),
			"if download is not started, this method will throw exception"
		);

		Assertions.assertTrue(catalogDataset.startDownload(),
			"if download started successfully it should return true");
		Assertions.assertDoesNotThrow(catalogDataset::getDirectoryName,
			"if download is not started, this method will throw exception"
		);
		Assertions.assertNotNull(catalogDataset.getDirectoryName());
	}

}
