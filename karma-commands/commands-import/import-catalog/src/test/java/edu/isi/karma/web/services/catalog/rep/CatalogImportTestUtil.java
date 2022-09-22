package edu.isi.karma.web.services.catalog.rep;

import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanResource;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class CatalogImportTestUtil {

	/**
	 * 1*1000000 = 1MB
	 *
	 * @param filename
	 * @param sizeInBytes
	 * @return
	 */
	public static String getUrlStringToTempFile(final File tempDir, final String filename, final long sizeInBytes) {
		try {
			File file = new File(tempDir, filename);
			file.createNewFile();

			RandomAccessFile raf;
			raf = new RandomAccessFile(file, "rw");
			raf.setLength(sizeInBytes);
			raf.close();

			return file.toURI().toURL().toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static CkanResource getTempCkanResource(String name, String id, File tempDir, String format, long fileSize) {
		CkanResource r = new CkanResource();
		r.setName(name);
		r.setId(id);
		r.setUrl(getUrlStringToTempFile(tempDir, name + "." + format, fileSize));
		r.setSize(String.valueOf(fileSize));
		r.setFormat(format);
		return r;
	}

	public static CkanDataset getTempCkanDataset(String name, String title, String id) {
		CkanDataset tempCD;
		tempCD = new CkanDataset();
		tempCD.setTitle(title);
		tempCD.setId(id);
		tempCD.setName(name);
		return tempCD;
	}

	public static CkanDataset getTempCkanDatasetPrivate(String name, String title, String id, String userId) {
		CkanDataset tempCD;
		tempCD = new CkanDataset();
		tempCD.setTitle(title);
		tempCD.setId(id);
		tempCD.setName(name);
		tempCD.setPriv(true);
		tempCD.setCreatorUserId(userId);
		return tempCD;
	}
}
