package edu.isi.karma.web.services.catalog.rep;

import eu.trentorise.opendata.jackan.model.CkanResource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

class CatalogResourceTest {

	@TempDir
	File tempDir;

	CatalogResource resource;
	CatalogResource nullResource;
	CatalogResource emptyResource;

	@BeforeEach
	void setUp() {
		CkanResource tempCkanResource1 = CatalogImportTestUtil.getTempCkanResource("remoteFile20", "id20", tempDir, "txt", 1000);
		this.resource = new CatalogResource(tempCkanResource1, new File(tempDir, "localFile1").getAbsolutePath(), "");

		this.nullResource = new CatalogResource(null, null, null);
		this.emptyResource = new CatalogResource(new CkanResource(), "", "");

		Assertions.assertNotNull(this.resource, "CatalogResource should not be null");
		Assertions.assertNotNull(this.nullResource, "CatalogResource should not be null");
		Assertions.assertNotNull(this.emptyResource, "CatalogResource should not be null");
	}

	@Test
	void createNewResourceTest() {
		Assertions.assertEquals(this.resource.getCurrentSize(), 0);
	}

	@Test
	void startDownloadTest() {
		Assertions.assertFalse(this.resource.isDownloadCompleted());
		this.resource.startDownload();
		boolean flag = true;
		while (flag) {
			if (!resource.isDownloadCompleted()
				&& !resource.isErrorWhileDownloading()
				&& !resource.isDownloadCanceled())
				continue;
			flag = false;
		}
	}

	@Test
	void getCurrentSizeTest() {
		Assertions.assertEquals(0, this.resource.getCurrentSize());
		this.resource.startDownload();
		Assertions.assertTrue(this.resource.getCurrentSize() > 0);

		Assertions.assertEquals(this.nullResource.getCurrentSize(), -1);
		this.nullResource.startDownload();
		Assertions.assertEquals(this.nullResource.getCurrentSize(), -1);

		Assertions.assertEquals(this.emptyResource.getCurrentSize(), -1);
		this.emptyResource.startDownload();
		Assertions.assertEquals(this.emptyResource.getCurrentSize(), -1);
	}

	@Test
	void isErrorWhileDownloadingTest() {
		Assertions.assertFalse(this.nullResource.isErrorWhileDownloading());
		this.resource.startDownload();
		Assertions.assertFalse(this.nullResource.isErrorWhileDownloading());

		Assertions.assertFalse(this.nullResource.isErrorWhileDownloading());
		this.nullResource.startDownload();
		Assertions.assertTrue(this.nullResource.isErrorWhileDownloading());

		Assertions.assertFalse(this.emptyResource.isErrorWhileDownloading());
		this.emptyResource.startDownload();
		Assertions.assertTrue(this.emptyResource.isErrorWhileDownloading());
	}

}
