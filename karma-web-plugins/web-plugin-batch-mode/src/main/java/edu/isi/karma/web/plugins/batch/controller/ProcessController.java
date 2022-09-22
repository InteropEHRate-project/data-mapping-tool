package edu.isi.karma.web.plugins.batch.controller;

import com.google.common.base.Strings;
import edu.isi.karma.config.KnowDiveServicesConfigurationRegistry;
import edu.isi.karma.rdf.GenericRDFGenerator.InputType;
import edu.isi.karma.util.FileUtil;
import edu.isi.karma.util.HTTPUtil;
import edu.isi.karma.web.plugins.batch.util.FormParameters;
import edu.isi.karma.web.plugins.batch.util.ProcessManager;
import edu.isi.karma.web.plugins.batch.model.Process;
import edu.isi.karma.webserver.KarmaException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.FileAlreadyExistsException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * rest api to manage batch-mode process.
 *
 * @author danish
 */
@Path("/process")
public class ProcessController {

	private static final Logger logger = LoggerFactory.getLogger(ProcessController.class);
	@Context
	ServletContext context;
	@Context
	UriInfo uriInfo;

	/**
	 * run a one time batch mode process.
	 *
	 * @return 200 if process completes, else 403 and message of error
	 */
	@POST
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/run")
	public Response createProcess(
		@FormDataParam(FormParameters.DATA_INPUT_STREAM) InputStream dataInputStream,
		@FormDataParam(FormParameters.DATA_INPUT_STREAM) FormDataContentDisposition dataFileDetail,
		@FormDataParam(FormParameters.R2RML_INPUT_STREAM) InputStream r2rmlInputStream,
		@FormDataParam(FormParameters.R2RML_INPUT_STREAM) FormDataContentDisposition r2rmlFileDetail,
		@DefaultValue("") @FormDataParam(FormParameters.DATA_URL) String dataUrl,
		@DefaultValue("") @FormDataParam(FormParameters.RAW_DATA) String rawData,
		@DefaultValue("") @FormDataParam(FormParameters.R2RML_URL) String r2rmlURL,
		@DefaultValue("") @FormDataParam(FormParameters.CONTENT_TYPE) InputType contentType,
		@DefaultValue("UTF-8") @FormDataParam(FormParameters.ENCODING) String encoding,
		@DefaultValue("-1") @FormDataParam(FormParameters.MAX_NUM_LINES) int maxNumLines,
		@DefaultValue(",") @FormDataParam(FormParameters.COLUMN_DELIMITER) String columnDelimiter,
		@DefaultValue("1") @FormDataParam(FormParameters.HEADER_START_INDEX) int headerStartIndex,
		@DefaultValue("2") @FormDataParam(FormParameters.DATA_START_INDEX) int dataStartIndex,
		@DefaultValue("FILE") @FormDataParam(FormParameters.OUTPUT_SOURCE_TYPE) String outputSourceType,
		@DefaultValue("RDF") @FormDataParam(FormParameters.OUTPUT_DATA_FORMAT) String outputDataFormat
	) {
		try {
			logger.info("Path - /process/run . Create a BatchModeProcess object and run the process");
			ProcessManager.initProcessManager(context,uriInfo);

			Map<String, Object> obj = new HashMap<>();
			int responseStatus = 200;

			String r2rmlFinaleUrl = getR2rmlFinaleUrl(r2rmlInputStream, r2rmlFileDetail, r2rmlURL);
			StringBuilder processName = new StringBuilder();

			// building process name
			if (!Strings.isNullOrEmpty(dataFileDetail.getFileName()))
				processName.append(dataFileDetail.getFileName().replace(".", "-")).append("-");
			else if (!Strings.isNullOrEmpty(dataUrl))
				processName.append(FilenameUtils.getBaseName(dataUrl)).append("-");
			processName.append((new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")).format(new Date(System.currentTimeMillis())));

			if (processName.toString().equals("")
				|| (dataInputStream == null && Strings.isNullOrEmpty(rawData) && Strings.isNullOrEmpty(dataUrl))
				|| Strings.isNullOrEmpty(r2rmlFinaleUrl)
			) {
				responseStatus = 403;
				obj.put("message", "required fields must be provided");
			} else if (outputDataFormat.equals(Process.OutputDataFormat.EML.name())
				&& HTTPUtil.returnHTTPPOSTRequestCode(
				KnowDiveServicesConfigurationRegistry.getInstance().getKnowDiveConfiguration(
					ProcessManager.getInstance().getContextMap().getId()).getEml_service_url() + "/converter/rdf") == 404
			) {
				responseStatus = 403;
				obj.put("message", "EML url doesn't exist");
			} else {
				Process process = new Process(processName.toString(), dataInputStream, dataUrl, rawData, r2rmlFinaleUrl,
					contentType, encoding, maxNumLines, columnDelimiter, headerStartIndex, dataStartIndex,
					outputSourceType, outputDataFormat);
				ProcessManager.getInstance().runProcess(process);
				obj.put("message", "ProcessID: " + processName + "\nUpload completed! Running the Data Integration process in background");
			}

			return Response.status(responseStatus).entity(obj).build();
		} catch (Exception e) {
			logger.error("Error running the process: " + e.getMessage());
			return Response.serverError().build();
		}
	}

	@GET
	@Produces("text/event-stream")
	@Path("/active")
	public void getActiveProcesses(@Context SseEventSink sseEventSink,
								   @Context Sse sse) {
		ProcessManager.initProcessManager(context,uriInfo);
		new Thread(() -> {
			logger.info("Path - /process/active . SEE to active processes");
			while (true) {
				ProcessManager.waitThread(3000);
				List<Process> listProcess = ProcessManager.getInstance().getAllActive();
				if (!CollectionUtils.isEmpty(listProcess)) {
					OutboundSseEvent sseEvent = sse.newEventBuilder()
//					.name("active-processes")
						.mediaType(MediaType.APPLICATION_JSON_TYPE)
						.data(listProcess)
						.reconnectDelay(3000)
						.comment("list of active processes")
						.build();
					sseEventSink.send(sseEvent);
				} else {
					break;
				}
			}
			sseEventSink.close();
		}).start();
	}

	/**
	 * return the list of process history
	 *
	 * @return
	 */
	@GET
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/history")
	public Response getHistoryProcesses() {
		try {
			ProcessManager.initProcessManager(context,uriInfo);
			logger.info("Path - /process/history . getting the list of history ");
			Map<String, Object> obj = new HashMap<>();
			int responseStatus = 200;

			List<Process> list = ProcessManager.getInstance().getAllHistory();

			if (list == null) {
				logger.error("Error: Persistence strategy not initialized ");
				responseStatus = 403;
				obj.put("message", "unsuccessful: persistence strategy not initialized");
			} else {
				obj.put("message", "successful");
				obj.put("processes", list);
			}

			return Response.status(responseStatus).entity(obj).build();

		} catch (Exception e) {
			logger.error("Error getting the list of processes: " + e.getMessage());
			return Response.serverError().build();
		}
	}

//	/**
//	 * schedule a process.
//	 *
//	 * @param processName
//	 * @return
//	 */
//	@POST
//	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//	@Produces(MediaType.APPLICATION_JSON)
//	@Path("/schedule")
//	public Response scheduleBatch(
//		@FormDataParam(FormParameters.R2RML_INPUT_STREAM) InputStream r2rmlInputStream,
//		@FormDataParam(FormParameters.R2RML_INPUT_STREAM) FormDataContentDisposition r2rmlFileDetail,
//		@DefaultValue("") @FormDataParam(FormParameters.PROCESS_NAME) String processName,
//		@DefaultValue("") @FormDataParam(FormParameters.DATA_URL) String dataUrl,
//		@DefaultValue("") @FormDataParam(FormParameters.R2RML_URL) String r2rmlURL,
//		@DefaultValue("") @FormDataParam(FormParameters.CONTENT_TYPE) InputType contextType,
//		@DefaultValue("UTF-8") @FormDataParam(FormParameters.ENCODING) String encoding,
//		@DefaultValue("-1") @FormDataParam(FormParameters.MAX_NUM_LINES) int maxNumLines,
//		@DefaultValue(",") @FormDataParam(FormParameters.COLUMN_DELIMITER) String columnDelimiter,
//		@DefaultValue("1") @FormDataParam(FormParameters.HEADER_START_INDEX) int headerStartIndex,
//		@DefaultValue("2") @FormDataParam(FormParameters.DATA_START_INDEX) int dataStartIndex,
//		@DefaultValue("FILE") @FormDataParam(FormParameters.OUTPUT_SOURCE_TYPE) String outputSourceType,
//		@DefaultValue("RDF") @FormDataParam(FormParameters.OUTPUT_DATA_FORMAT) String outputDataFormat
//	) {
//		try {
//			logger.info("Path - /process/schedule . schedule a BatchModeProcess object which can be run in the future");
//			Map<String, Object> obj = new HashMap<>();
//			int responseStatus = 200;
//
//			//upload r2rml input stream to karma backend
//			String r2rmlFinaleUrl = getR2rmlFinaleUrl(r2rmlInputStream, r2rmlFileDetail, r2rmlURL);
//
//			if (!processName.equals("") && !dataUrl.equals("") && !Strings.isNullOrEmpty(r2rmlFinaleUrl)) {
//
//				Process process = new Process(processName, r2rmlFinaleUrl, contextType, encoding, dataUrl, maxNumLines,
//					columnDelimiter, headerStartIndex, dataStartIndex, outputSourceType, outputDataFormat);
//
//				if (ProcessManager.getInstance().add(process)) {
//					logger.info("process created successfully");
//					obj.put("message", "successful");
//				} else {
//					responseStatus = 403;
//					obj.put("message", "unsuccessful: either process: "
//						+ processName + " already exist OR persistence strategy not setup");
//				}
//			} else {
//				responseStatus = 403;
//				obj.put("message", "required fields must be provided");
//			}
//
//			return Response.status(responseStatus).entity(obj).build();
//
//		} catch (Exception e) {
//			logger.error("Error scheduling the process with ID: " + processName, e);
//			return Response.serverError().build();
//		}
//	}
//
//	@POST
//	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//	@Produces(MediaType.APPLICATION_JSON)
//	@Path("/remove")
//	public Response removeProcess(
//		@DefaultValue("") @QueryParam(FormParameters.PROCESS_NAME) String processName
//	) {
//		try {
//			logger.info("Path - /process/remove . remove process processName: " + processName);
//			Map<String, Object> obj = new HashMap<>();
//			int responseStatus = 200;
//
//			Process process = ProcessManager.getInstance().getProcess(processName);
//			if (process != null) {
//				if (ProcessManager.getInstance().removeProcess(processName)) {
//					obj.put("message", "successful");
//				} else {
//					responseStatus = 500;
//					obj.put("message", "ERROR: unable to remove" + processName);
//				}
//			} else {
//				responseStatus = 403;
//				obj.put("message", "ERROR: either process: "
//					+ processName + " doesn't exist OR persistence strategy not initialized");
//			}
//
//			return Response.status(responseStatus).entity(obj).build();
//
//		} catch (Exception e) {
//			logger.error("Error running the batch. EML generation error", e);
//			return Response.serverError().build();
//		}
//	}
//
//	/**
//	 * return the list of process defined
//	 *
//	 * @return
//	 */
//	@GET
//	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//	@Produces(MediaType.APPLICATION_JSON)
//	@Path("/getall")
//	public Response getAllProcesses() {
//		try {
//			logger.info("Path - /process/getall . getting the list of processes ");
//			Map<String, Object> obj = new HashMap<>();
//			int responseStatus = 200;
//
//			if (ProcessManager.getInstance().getAll() == null) {
//				logger.error("Error: Persistence strategy not initialized ");
//				responseStatus = 403;
//				obj.put("message", "unsuccessful: persistence strategy not initialized");
//			} else {
//				obj.put("message", "successful");
//				obj.put("processes", ProcessManager.getInstance().getAll());
//			}
//
//			return Response.status(responseStatus).entity(obj).build();
//
//		} catch (Exception e) {
//			logger.error("Error getting the list of processes", e);
//			return Response.serverError().build();
//		}
//	}

//	/**
//	 * run a scheduled or unscheduled process
//	 *
//	 * @param processName
//	 * @return
//	 */
//	@POST
//	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//	@Produces(MediaType.APPLICATION_JSON)
//	@Path("/run")
//	public Response runProcess(
//		@DefaultValue("") @QueryParam(FormParameters.PROCESS_NAME) String processName
//	) {
//		try {
//			logger.info("Path - /process/run . run a scheduled or unscheduled processName: " + processName);
//			Map<String, Object> obj = new HashMap<>();
//			int responseStatus = 200;
//
//			Process process = ProcessManager.getInstance().getProcess(processName);
//			if (process != null) {
//				Thread t = new Thread(process);
//				t.start();
//				if (t.isAlive() && !t.isInterrupted()) {
//					obj.put("message", "successful");
//				} else {
//					responseStatus = 500;
//					obj.put("message", "unsuccessful: Unable to start the process");
//				}
//			} else {
//				responseStatus = 403;
//				obj.put("message", "unsuccessful: either process: "
//					+ processName + " doesn't exist OR persistence strategy not initialized");
//			}
//
//			return Response.status(responseStatus).entity(obj).build();
//
//		} catch (Exception e) {
//			logger.error("Error running the batch. EML generation error", e);
//			return Response.serverError().build();
//		}
//	}
//
//	/**
//	 * get the status of a given process.
//	 *
//	 * @param processName
//	 * @return
//	 */
//	@GET
//	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//	@Produces(MediaType.APPLICATION_JSON)
//	@Path("/getstatus")
//	public Response getBatch(
//		@DefaultValue("") @QueryParam(FormParameters.PROCESS_NAME) String processName
//	) {
//		try {
//			Map<String, Object> obj = new HashMap<>();
//			int responseStatus = 200;
//
//			Process process = ProcessManager.getInstance().getProcess(processName);
//			if (process != null) {
//				if (process.isError()) {
//					responseStatus = 500;
//					obj.put("message", "unsuccessful: Unable to complete the process");
//				} else if (process.isAlive()) {
//					obj.put("message", "active");
//				} else if (process.isCompleted()) {
//					obj.put("process", process);
//				}
//			} else {
//				responseStatus = 403;
//				obj.put("message", "unsuccessful: either process: "
//					+ processName + " doesn't exist OR persistence strategy not initialized");
//			}
//
//			return Response.status(responseStatus).entity(obj).build();
//
//		} catch (Exception e) {
//			logger.error("Error while fetching the status of the process: " + processName, e);
//			return Response.serverError().build();
//		}
//	}

	/******************************************************
	 * Private Methods
	 ******************************************************/

	private String getR2rmlFinaleUrl(InputStream inputStream, FormDataContentDisposition fileDetail, String url) {
		if (inputStream != null) {
			String downloadedFilePath = ProcessManager.getInstance().getBatchModeUserDirectory()
				+ "/Ontologies/" + fileDetail.getFileName();
			;
			logger.info("r2rml file path: " + downloadedFilePath);
			try {
				FileUtil.downloadFromInputStream(inputStream, downloadedFilePath);
				return new File(downloadedFilePath).toURI().toURL().toString();
			} catch (FileAlreadyExistsException e) {
				logger.info("r2rml file already exist, using existing");
				try {
					return new File(downloadedFilePath).toURI().toURL().toString();
				} catch (MalformedURLException malformedURLException) {
					malformedURLException.printStackTrace();
				}
			} catch (IOException e) {
				logger.error("unable to download r2rml");
				return null;
			}
		} else if (!Strings.isNullOrEmpty(url)) {
			return url;
		}
		return null;

	}

}
