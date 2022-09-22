package edu.isi.karma.util;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Utility used to communicate with eml service to convert rdf data to eml.
 *
 * @author Danish
 */
public class EMLUtil {

	private static final Logger logger = LoggerFactory.getLogger(EMLUtil.class);
	/**
	 * max number of rdf object to be sent to eml converter in one http POST request
	 */
	private static final int MAX_EML_OBJECT_PER_HTTP = 1000;

	private EMLUtil() {
	}

	/**
	 *
	 * @param rdfFileLocalPath absolute path to the rdf file located in karma backend
	 * @param encoding encoding to the file content
	 * @param emlFileLocalPath absolute path to the eml file to be created
	 * @param emlUrl url to the eml service, only base path is required
	 * @return
	 */
	public static boolean generateEMLFromRDF(String rdfFileLocalPath, String encoding, String emlFileLocalPath,
											 String emlUrl){
		try {
			File f = new File(emlFileLocalPath);
			File parentDir = f.getParentFile();
			parentDir.mkdirs();
			BufferedWriter bw = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(f), encoding));

			// read the RDF file
			StringBuilder rdfObject = new StringBuilder();
			BufferedReader br = Files.newBufferedReader(Paths.get(rdfFileLocalPath), Charset.forName(encoding));
			boolean isFirstObject = true;
			int i = 0;
			bw.write("[");
			bw.newLine();
			for (String line; (line = br.readLine()) != null; ) {
				if (line.trim().isEmpty()) {
					i++;
					rdfObject.append("\n");
				} else {
					rdfObject.append(line);
				}
				if (i >= MAX_EML_OBJECT_PER_HTTP) {
					String emlObject = getEmlObjectFromRDF(rdfObject.toString(), encoding, emlUrl);
					if (!Strings.isNullOrEmpty(emlObject)) {
						if (!isFirstObject) {
							bw.write(",");
							bw.newLine();
						}
						// write EML to file
						bw.write(emlObject);
					}
					isFirstObject = false;
					rdfObject = new StringBuilder();
					i = 0;
				}
			}
			if (!Strings.isNullOrEmpty(rdfObject.toString())) {
				String emlObject = getEmlObjectFromRDF(rdfObject.toString(), encoding, emlUrl);
				if (!Strings.isNullOrEmpty(emlObject)) {
					if (!isFirstObject) {
						bw.write(",");
						bw.newLine();
					}
					// write EML to file
					bw.write(emlObject);
				}
			}
			bw.newLine();
			bw.write("]");
			bw.close();
			return true;
		}catch (IOException e){
			logger.error("error "+ e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * get a list of eml object providing rdf objects
	 * @param rdfObject
	 * @param encoding
	 * @param emlURL url to the service
	 * @return
	 */
	public static String getEmlObjectFromRDF(String rdfObject,String encoding,String emlURL) {
		StringBuilder url = new StringBuilder();
		url.append(emlURL);
		url.append("/converter/rdf");
		try {
			String jsonString = JSONUtil.prettyPrintJson(
				HTTPUtil.executeHTTPPostRequest(
					url.toString()
					,MediaType.TEXT_PLAIN
					,MediaType.APPLICATION_JSON
					,rdfObject
					,encoding
				)
			);
			jsonString = jsonString.trim();
			if(jsonString.length() > 0
				&& jsonString.charAt(0) == '['
				&& jsonString.charAt(jsonString.length() - 1) == ']'
			){
				jsonString = jsonString.substring(1, jsonString.length() - 1);
			}
			else{
				throw new IOException("response format is not as requested");
			}
			return jsonString;
		} catch (IOException e) {
			logger.error("Error in connection with remote server: " + e.getMessage());
			return null;
		}
	}
}
