package edu.isi.karma.web.plugins.batch.model;

import com.google.common.base.Strings;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import edu.isi.karma.config.KnowDiveServicesConfigurationRegistry;
import edu.isi.karma.config.ModelingConfiguration;
import edu.isi.karma.config.ModelingConfigurationRegistry;
import edu.isi.karma.kr2rml.URIFormatter;
import edu.isi.karma.kr2rml.mapping.R2RMLMappingIdentifier;
import edu.isi.karma.kr2rml.writer.KR2RMLRDFWriter;
import edu.isi.karma.kr2rml.writer.N3KR2RMLRDFWriter;
import edu.isi.karma.rdf.GenericRDFGenerator;
import edu.isi.karma.rdf.GenericRDFGenerator.InputType;
import edu.isi.karma.rdf.InputProperties;
import edu.isi.karma.rdf.RDFGeneratorRequest;
import edu.isi.karma.rdf.WorksheetGenerator;
import edu.isi.karma.rep.Worksheet;
import edu.isi.karma.rep.Workspace;
import edu.isi.karma.rep.WorkspaceManager;
import edu.isi.karma.util.EMLUtil;
import edu.isi.karma.web.plugins.batch.util.ProcessManager;
import edu.isi.karma.webserver.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * a class used to host batch mode process.
 * USE:
 * 1) create and run a one time process to generate RDF or EML
 * 2) create and schedule a process tobe used multiple times.
 *
 * @author danish
 */
public class Process implements Comparable<Process>, Runnable, Serializable {

	public enum OutputDataFormat {
		EML,
		RDF
	}

	public enum OutputSourceType {
		FILE,
		STREAM
	}

	private static final Logger logger = LoggerFactory.getLogger(Process.class);

	/**
	 * data input stream in the case of onetime run process
	 */
	@XStreamOmitField
	private InputStream dataInputStream;

	/*********************************************************************
	 /* to be persisted in backend
	 *********************************************************************/
	/**
	 * id of the process
	 */
	private final String processName;
	/**
	 * url to the input file
	 */
	private String fileUrl;
	/**
	 * raw data which can be provided instead of input file
	 */
	private String rawData;
	/**
	 * url to the r2rml model
	 */
	private String r2rmlURL;
	/**
	 * type of content in the input file
	 */
	private InputType contentType;
	/**
	 * max number of lines to be processed for batch-mode
	 * -1 is default for all lines
	 */
	private int maxNumLines;
	/**
	 * for CSV files a columnDeliminator such as COMMA or TAB
	 */
	private String columnDelimiter;
	/**
	 * in case of first row of csv file is attribute names
	 */
	private int headerStartIndex;
	/**
	 * line number where to start processing the data
	 */
	private int dataStartIndex;
	/**
	 * input file encoding
	 */
	private String encoding;
	/**
	 * if scheduled persist to filesystem in karma backend
	 */
	private boolean isScheduled;
	/**
	 * list of eml file urls generated for a scheduled of one-time process
	 */
	private String emlURL;
	/**
	 * list of rdf file urls generated for a scheduled of one-time process
	 */
	private String rdfURL;
	/**
	 * type of an output source, generate a string or a file
	 */
	private OutputSourceType outputSourceType;
	/**
	 * type of data format, generate rdf or eml
	 */
	private OutputDataFormat outputDataFormat;
	/**
	 * store string if output source is a stream
	 */
	private String emlOutputStream;
	/**
	 * store string if output source is a stream
	 */
	private String rdfOutputStream;
	/**
	 * if process is done executing
	 */
	private boolean completed;
	/**
	 * if there was an error during last process execution
	 */
	private boolean error;
	/**
	 * error message
	 */
	private String message;
	/**
	 * if process is still running
	 */
	private boolean alive;
	@XStreamOmitField
	private Workspace workspace;
	@XStreamOmitField
	private Worksheet worksheet;

	/**
	 * constructor used for one-time run process
	 *
	 * @param processName      name of the process. Use {@link edu.isi.karma.web.plugins.batch.controller.HelperController}
	 *                         to get the eligible name
	 * @param dataInputStream  {@link InputStream} to the input file
	 * @param dataUrl          url to the input file.
	 * @param rawData          a string of raw data to be processed.
	 * @param r2rmlFinaleUrl   {@link URL} in string format for the r2rml model
	 * @param contentType      type of content found in the input file
	 * @param encoding         encoding of the file
	 * @param maxNumLines      max number rof lines to be processed
	 * @param columnDelimiter
	 * @param headerStartIndex
	 * @param dataStartIndex
	 * @param outputSourceType {@link OutputSourceType}
	 * @param outputDataFormat {@link OutputDataFormat}
	 */
	public Process(String processName, InputStream dataInputStream, String dataUrl, String rawData, String r2rmlFinaleUrl,
				   InputType contentType, String encoding, int maxNumLines, String columnDelimiter, int headerStartIndex,
				   int dataStartIndex, String outputSourceType, String outputDataFormat) {
		this.processName = processName;
		this.dataInputStream = dataInputStream;
		this.rawData = rawData;
		this.r2rmlURL = r2rmlFinaleUrl;
		this.contentType = contentType;
		this.encoding = encoding;
		this.fileUrl = dataUrl;
		this.maxNumLines = maxNumLines;
		this.columnDelimiter = columnDelimiter;
		this.headerStartIndex = headerStartIndex;
		this.dataStartIndex = dataStartIndex;
		this.outputSourceType = OutputSourceType.valueOf(outputSourceType.toUpperCase());
		this.outputDataFormat = OutputDataFormat.valueOf(outputDataFormat.toUpperCase());

		this.isScheduled = false;
		this.emlOutputStream = "";
		this.rdfOutputStream = "";
		this.message = "process created";
		this.emlURL = "";
		this.rdfURL = "";
		this.completed = false;
		this.error = false;
		this.workspace = null;
		this.worksheet = null;
	}

	/******************************************************
	 * Override Methods
	 ******************************************************/

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
			.append(processName).toHashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (this.getClass() != o.getClass()) return false;
		Process process = (Process) o;
		return processName.equals(process.processName);
	}

	@Override
	public int compareTo(Process process) {
		return this.processName.compareTo(process.getProcessName());
	}

	@Override
	public void run() {
		setCompleted(false);
		setError(false);
		setAlive(true);
//		logger.info("new thread wait for 30 second");
//		ProcessManager.waitThread(1000 * 30);
//		logger.info("new thread restart the process after 30 second");
		String rdfFileLocalPath = generateRDF();
		String emlFileLocalPath = null;
		if (this.outputDataFormat.equals(OutputDataFormat.EML) && !Strings.isNullOrEmpty(rdfFileLocalPath)) {
			logger.info("generating eml");
			emlFileLocalPath = generateEML(rdfFileLocalPath);
		}
		cleanUp(rdfFileLocalPath, emlFileLocalPath);
		setCompleted(true);
		setAlive(false);
		this.setMessage("process completed");
		ProcessManager.getInstance().removeFromActive(this);
		ProcessManager.getInstance().addToHistory(this);
	}

	/******************************************************
	 * Public Methods
	 ******************************************************/

	/**
	 * If URL is provided, data will be fetched from the URL, else raw Data in
	 * JSON, CSV or XML should be provided
	 */
	public void generateWorkSpace() {
		setCompleted(false);
		setError(false);
		setAlive(true);
		initializeWorkspace();
		initializeWorksheet();
	}

	public boolean isDataInputStream() {
		return this.dataInputStream != null;
	}

	/******************************************************
	 * Private Methods
	 ******************************************************/

	private void initializeWorkspace() {

		ServletContextParameterMap contextParameters = ProcessManager.getInstance().getContextMap();

		Workspace workspace = WorkspaceManager.getInstance().createWorkspace(contextParameters.getId());
		WorkspaceRegistry.getInstance().register(new ExecutionController(workspace));
		WorkspaceKarmaHomeRegistry.getInstance().register(workspace.getId(), contextParameters.getKarmaHome());
		ModelingConfiguration modelingConfiguration = ModelingConfigurationRegistry.getInstance().register(contextParameters.getId());
		modelingConfiguration.setManualAlignment();
		this.workspace = workspace;
	}

	private void initializeWorksheet() {
		InputProperties inputProperties = new InputProperties();
		inputProperties.set(InputProperties.InputProperty.MAX_NUM_LINES, this.maxNumLines);
		inputProperties.set(InputProperties.InputProperty.DELIMITER, this.columnDelimiter);
		inputProperties.set(InputProperties.InputProperty.DATA_START_INDEX, this.dataStartIndex);
		inputProperties.set(InputProperties.InputProperty.HEADER_START_INDEX, this.headerStartIndex);
		String qualifier = "\"";
		inputProperties.set(InputProperties.InputProperty.TEXT_QUALIFIER, qualifier);
		inputProperties.set(InputProperties.InputProperty.ENCODING, this.encoding);
		String workSheetIndex = "1";
		inputProperties.set(InputProperties.InputProperty.WORKSHEET_INDEX, workSheetIndex);

		try {
			this.worksheet = WorksheetGenerator.generateWorksheet(this.r2rmlURL,
				new BufferedInputStream(this.dataInputStream), this.contentType, inputProperties, workspace);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (KarmaException e) {
			e.printStackTrace();
		}
	}

	private boolean getFinalDataInputStream() {
		InputStream finalIS = null;
		try {
			if (!Strings.isNullOrEmpty(this.fileUrl)) {
				finalIS = new URL(this.fileUrl).openStream();
			} else if (!Strings.isNullOrEmpty(this.rawData))
				finalIS = IOUtils.toInputStream(this.rawData, Charset.forName(encoding));
		} catch (IOException e) {
			logger.info("No Input Stream Found: " + e.getMessage());
			e.printStackTrace();
		}
		if (finalIS != null) {
			this.dataInputStream = finalIS;
			return true;
		}
		return false;
	}

	/**
	 * If URL is provided, data will be fetched from the URL, else raw Data in
	 * JSON, CSV or XML should be provided
	 */
	private String generateRDF() {
		try {
			// Prepare the file path and names
			this.setMessage("generating RDF");
			final String rdfFileName = this.processName + ".ttl";
			final String rdfFileLocalPath = ContextParametersRegistry.getInstance().getContextParameters(
				ProcessManager.getInstance().getContextMap().getId()).getParameterValue(
				ServletContextParameterMap.ContextParameter.RDF_PUBLISH_DIR) + rdfFileName;
			File f = new File(rdfFileLocalPath);
			File parentDir = f.getParentFile();
			parentDir.mkdirs();
			BufferedWriter bw = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(f), encoding));

			GenericRDFGenerator gRDFGen = new GenericRDFGenerator(null);

			R2RMLMappingIdentifier rmlID = new R2RMLMappingIdentifier(this.r2rmlURL, new URL(this.r2rmlURL));
			gRDFGen.addModel(rmlID);

			URIFormatter uriFormatter = new URIFormatter();
			N3KR2RMLRDFWriter outWriter = new N3KR2RMLRDFWriter(uriFormatter, new PrintWriter(bw));

			if (this.worksheet == null) {
				getFinalDataInputStream();
			}else{
				this.dataInputStream = null;
			}

			RDFGeneratorRequest request = generateRDFRequest(rmlID.getName(), this.r2rmlURL, this.dataInputStream, outWriter);
			gRDFGen.generateRDF(request);

			bw.close();
			this.setRdfURL(
				ContextParametersRegistry.getInstance().getContextParameters(
					ProcessManager.getInstance().getContextMap().getId())
					.getParameterValue(ServletContextParameterMap.ContextParameter.PUBLIC_RDF_ADDRESS)
					+ Paths.get(rdfFileLocalPath).getFileName());
			return rdfFileLocalPath;
		} catch (KarmaException e) {
			setError(true);
			setAlive(false);
			setMessage("generating RDF error");
			logger.info("Karma Exception");
			e.printStackTrace();
		} catch (IOException e) {
			setError(true);
			setAlive(false);
			setMessage("generating RDF error");
			logger.info("IO Exception");
			e.printStackTrace();
		}
		return null;
	}

	private String generateEML(String rdfFileLocalPath) {
		this.setMessage("generating EML");
		// Prepare the file path and names
		final String emlFileName = this.processName + ".eml";
		final String emlFileLocalPath = ContextParametersRegistry.getInstance().getContextParameters(
			ProcessManager.getInstance().getContextMap().getId())
			.getParameterValue(ServletContextParameterMap.ContextParameter.EML_PUBLISH_DIR) +
			emlFileName;
		logger.info("eml file local path" + emlFileLocalPath);
		String emlUrl = KnowDiveServicesConfigurationRegistry.getInstance().getKnowDiveConfiguration(
			ProcessManager.getInstance().getContextMap().getId()).getEml_service_url();
		return EMLUtil.generateEMLFromRDF(rdfFileLocalPath, encoding, emlFileLocalPath, emlUrl) ? emlFileLocalPath : null;
	}

	private void cleanUp(String rdfFileLocalPath, String emlFileLocalPath) {
		try {
			if (this.outputDataFormat.equals(OutputDataFormat.EML)
				&& !Strings.isNullOrEmpty(rdfFileLocalPath)
				&& !Strings.isNullOrEmpty(emlFileLocalPath)) {
				Files.delete(Paths.get(rdfFileLocalPath));
				this.setRdfURL("");
				if (this.outputSourceType.equals(OutputSourceType.STREAM)) {
					this.setEmlOutputStream(Files.readString(Paths.get(emlFileLocalPath), Charset.forName(encoding)));
					Files.delete(Paths.get(emlFileLocalPath));
				} else {
					this.setEmlURL(
						ContextParametersRegistry.getInstance().getContextParameters(
							ProcessManager.getInstance().getContextMap().getId())
							.getParameterValue(ServletContextParameterMap.ContextParameter.PUBLIC_EML_ADDRESS)
							+ Paths.get(emlFileLocalPath).getFileName());
				}
			} else if (this.outputDataFormat.equals(OutputDataFormat.RDF)
				&& this.outputSourceType.equals(OutputSourceType.STREAM)) {
				this.setRdfOutputStream(Files.readString(Paths.get(rdfFileLocalPath), Charset.forName(encoding)));
				Files.delete(Paths.get(rdfFileLocalPath));
				this.setRdfURL("");
			}

		} catch (IOException e) {
			setError(true);
			setMessage("cleaning up fail");
			e.printStackTrace();
		}
	}

	private RDFGeneratorRequest generateRDFRequest(String modelName, String sourceName, InputStream is, KR2RMLRDFWriter writer) {
		RDFGeneratorRequest request = new RDFGeneratorRequest(modelName, sourceName);
		request.addWriter(writer);
		if(is != null) request.setInputStream(is);

		request.setDataType(this.contentType);

		request.setAddProvenance(false);
		request.setMaxNumLines(this.maxNumLines);
		request.setDataStartIndex(this.dataStartIndex);
		request.setHeaderStartIndex(this.headerStartIndex);
		request.setDelimiter(this.columnDelimiter);
		request.setContextParameters(ProcessManager.getInstance().getContextMap());

		if (!Strings.isNullOrEmpty(this.encoding))
			request.setEncoding(this.encoding);

		request.setWorkspace(this.workspace);
		request.setWorksheet(this.worksheet);

		return request;
	}

	/***************************************************
	 * Getter Setter
	 ***************************************************/

	public String getProcessName() {
		return this.processName;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
//		ProcessManager.getInstance().updateProcess(this);
	}

	public boolean isAlive() {
		return alive;
	}

	public void setAlive(boolean alive) {
		this.alive = alive;
//		ProcessManager.getInstance().updateProcess(this);
	}

	public boolean isCompleted() {
		return this.completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
//		ProcessManager.getInstance().updateProcess(this);
	}

	public boolean isError() {
		return this.error;
	}

	public void setError(boolean error) {
		this.error = error;
//		ProcessManager.getInstance().updateProcess(this);
	}

	public String getEmlOutputStream() {
		return emlOutputStream;
	}

	public void setEmlOutputStream(String emlOutputStream) {
		this.emlOutputStream = emlOutputStream;
	}

	public String getRdfOutputStream() {
		return rdfOutputStream;
	}

	public void setRdfOutputStream(String rdfOutputStream) {
		this.rdfOutputStream = rdfOutputStream;
	}

	public String getEncoding() {
		return encoding;
	}

	public String getR2rmlURL() {
		return r2rmlURL;
	}

	public void setR2rmlURL(String r2rmlURL) {
		this.r2rmlURL = r2rmlURL;
//		ProcessManager.getInstance().updateProcess(this);
	}

	public InputType getContentType() {
		return contentType;
	}

	public void setContentType(InputType contentType) {
		this.contentType = contentType;
//		ProcessManager.getInstance().updateProcess(this);
	}

	public String getFileUrl() {
		return fileUrl;
	}

	public String getRawData() {
		return rawData;
	}

	public int getMaxNumLines() {
		return maxNumLines;
	}

	public String getColumnDelimiter() {
		return columnDelimiter;
	}

	public int getHeaderStartIndex() {
		return headerStartIndex;
	}

	public int getDataStartIndex() {
		return dataStartIndex;
	}

	public boolean isScheduled() {
		return isScheduled;
	}

	public void setScheduled(boolean scheduled) {
		isScheduled = scheduled;
//		ProcessManager.getInstance().updateProcess(this);
	}

	public String getEmlURL() {
		return emlURL;
	}

	public void setEmlURL(String url) {
		this.emlURL = url;
	}

	public String getRdfURL() {
		return rdfURL;
	}

	public void setRdfURL(String url) {
		this.rdfURL = url;
	}

	public OutputSourceType getOutputSourceType() {
		return outputSourceType;
	}

	public OutputDataFormat getOutputDataFormat() {
		return outputDataFormat;
	}

}
