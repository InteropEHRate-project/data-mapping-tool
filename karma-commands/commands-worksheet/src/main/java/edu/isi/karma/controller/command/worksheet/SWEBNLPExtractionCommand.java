package edu.isi.karma.controller.command.worksheet;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import edu.isi.karma.config.KnowDiveServicesConfigurationRegistry;
import edu.isi.karma.controller.command.CommandException;
import edu.isi.karma.controller.command.CommandType;
import edu.isi.karma.controller.command.ISWEBNLPExtractionCommand;
import edu.isi.karma.controller.command.WorksheetSelectionCommand;
import edu.isi.karma.controller.command.selection.SuperSelection;
import edu.isi.karma.controller.update.ErrorUpdate;
import edu.isi.karma.controller.update.InfoUpdate;
import edu.isi.karma.controller.update.UpdateContainer;
import edu.isi.karma.controller.update.WorksheetUpdateFactory;
import edu.isi.karma.er.helper.CloneTableUtils;
import edu.isi.karma.kr2rml.exception.ValueNotFoundKarmaException;
import edu.isi.karma.rep.*;

import edu.isi.karma.util.Util;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * handle the request of extracting the Information using SWEB API.
 * java thread pool are used in case of bigger files which divide the data into {@link #MAX_BODY}
 * number of lines for each http request.
 * if successful it creates a new command of {@link AddValuesCommand} to update the worksheet.
 *
 * @author danish.cheema@unitn.it
 */
public abstract class SWEBNLPExtractionCommand extends WorksheetSelectionCommand implements ISWEBNLPExtractionCommand {

	private static final Logger logger = LoggerFactory.getLogger(SWEBNLPExtractionCommand.class);
	/**
	 * max number of concurrent Thread
	 * TODO: SWEB-runtime server need to load whole DB for first request. in case of more then one
	 * 	http request at once, the SWEB-runtime become unresponsive. for that reason only one parallel
	 * 	thread is used at a time. Note that error still exist if 1 http request is sent at first before
	 * 	creating a pool of bigger size.
	 */
	private int MAX_THREADS = 1;
	/**
	 * max time out for each http request thread in seconds
	 */
	private int TIME_OUT = 120;
	/**
	 * maximum number of rows sent in one Http Request Body
	 */
	private int MAX_BODY = 5000;
	/**
	 * input column node id
	 */
	protected final String hNodeId;
	/**
	 * list of new columns nodes ids created
	 */
	protected List<String> newColumnsNodeIds;
	/**
	 * name of the NLP pipeline
	 */
	final String pipeline;
	/**
	 * URL for SWEB Service
	 */
	String extractionURL;
	/**
	 * Absolute Column Name
	 */
	String hNodeName;
	CommandType commandType = CommandType.undoable;


	public SWEBNLPExtractionCommand(String id,
									String model,
									String worksheetId,
									String selectionId,
									String hNodeId,
									String pipeline,
									int MAX_BODY,
									int MAX_THREADS,
									int TIME_OUT) {
		super(id, model, worksheetId, selectionId);
		this.hNodeId = hNodeId;
		this.pipeline = pipeline;
		this.MAX_BODY = MAX_BODY;
		this.MAX_THREADS = MAX_THREADS;
		this.TIME_OUT = TIME_OUT;
	}

	@Override
	public UpdateContainer doIt(Workspace workspace) throws CommandException {

		if (!isAllowedPipeline(this.pipeline)) {
			this.commandType = CommandType.notInHistory;
			return new UpdateContainer(new ErrorUpdate(this.pipeline + " is not supported for this functionality"));
		}

		this.extractionURL = KnowDiveServicesConfigurationRegistry.getInstance()
			.getKnowDiveConfiguration(workspace.getContextId())
			.getScroll_service_url();
		Worksheet worksheet = workspace.getWorksheet(this.worksheetId);
		SuperSelection selection = getSuperSelection(worksheet);
		RepFactory repFactory = workspace.getFactory();
		HTable ht = workspace.getFactory().getHTable(repFactory.getHNode(this.hNodeId).getHTableId());

		try {
			String newHNodeId = getInputHNodeId();
			// map to save row id as key and text as values.
			Map<String, List<String>> cellValueRowIdsMap = getCellValueRowIdsMap(workspace, worksheet, selection, newHNodeId);
			sendRequestToSWEB(cellValueRowIdsMap);

			Map<String, HashMap<String, CellValue>> valuesToBeAdded = getRetrievedInformation(workspace);

			// exit if no SemText found
			if (valuesToBeAdded == null) {
				this.commandType = CommandType.notInHistory;
				throw new Exception("No Information Found");
			}

			// Add Value Command
			// prepare data tobe added in worksheet
			this.newColumnsNodeIds = addNewCellValueCommand(workspace, worksheet, model, selection,
				valuesToBeAdded, ht.getId(), newHNodeId);
			this.hNodeName = workspace.getFactory().getHNode(newHNodeId).getAbsoluteColumnName(workspace.getFactory());

			UpdateContainer c = new UpdateContainer(new InfoUpdate(getInfoMessage()));
			c.append(WorksheetUpdateFactory
				.createRegenerateWorksheetUpdates(
					this.worksheetId,
					getSuperSelection(workspace.getWorksheet(this.worksheetId)),
					workspace.getContextId()
				)
			);
			c.append(computeAlignmentAndSemanticTypesAndCreateUpdates(workspace));
			logger.info("Command executed successfully");

			return c;
		} catch (Exception e) {
			logger.error("Error in Command: " + e);
			Util.logException(logger, e);
			return new UpdateContainer(new ErrorUpdate(e.getMessage()));
		}

	}

	@Override
	public UpdateContainer undoIt(Workspace workspace) {
		Worksheet worksheet = workspace.getWorksheet(worksheetId);
		RepFactory repFactory = workspace.getFactory();
		HTable ht = repFactory.getHTable(repFactory.getHNode(this.hNodeId).getHTableId());
		//remove the new column
		for (String nodeId : this.newColumnsNodeIds) {
			ht.removeHNode(nodeId, worksheet);
		}

		return WorksheetUpdateFactory.createRegenerateWorksheetUpdates(
			worksheetId,
			getSuperSelection(worksheet),
			workspace.getContextId()
		);

	}

	/**
	 * method return the cell value row ids map,
	 * NOTE that the cell values will be unique, by adding rowid to list
	 * NOTE that empty or null value will be filtered and filerandsave function
	 * will be called with null value for given rowid
	 *
	 * @param workspace parent workspace
	 * @param worksheet parent worksheet
	 * @param selection
	 * @param hNodeId   column id
	 * @return map of rowID and cell value
	 * @throws CommandException
	 */
	public static Map<String, List<String>> getCellValueRowIds(Workspace workspace,
															   Worksheet worksheet,
															   SuperSelection selection,
															   String hNodeId) throws ValueNotFoundKarmaException {
		Map<String, List<String>> cellValueRowIdsMap = new LinkedHashMap<>();
		//list of tables
		List<Table> tables = new ArrayList<>();
		HTable hTable = workspace.getFactory().getHTable(workspace.getFactory().getHNode(hNodeId).getHTableId());
		CloneTableUtils.getDatatable(worksheet.getDataTable(), hTable, tables, selection);
		for (Table table : tables) {
			for (Row row : table.getRows(0, table.getNumRows(), selection)) {
				String text = row.getNode(hNodeId).getValue().asString();
				String rowId = row.getId();
				// eliminating the duplicates
				if (cellValueRowIdsMap.containsKey(text)) {
					cellValueRowIdsMap.get(text).add(rowId);
				} else {
					List<String> keys = new LinkedList<>();
					keys.add(rowId);
					cellValueRowIdsMap.put(text, keys);

				}
			}
		}
		if (cellValueRowIdsMap.size() < 1) {
			throw new ValueNotFoundKarmaException("unable to get cell values for column name: "
				+ workspace.getFactory().getHNode(hNodeId).getColumnName(),
				workspace.getFactory().getHNode(hNodeId).getColumnName());
		}
		return cellValueRowIdsMap;
	}

	/**
	 * divide the data into small blocks for http request and keep the related row ids in the karma backend.
	 * we assume that the http response data will be in the same order as request data, so we use
	 * the same row order to save back the response.
	 *
	 * @param cellValueRowIdsMap
	 * @throws Exception
	 */
	private void sendRequestToSWEB(Map<String, List<String>> cellValueRowIdsMap) throws Exception {
		List<List<String>> inputRowIds = new ArrayList<>(cellValueRowIdsMap.values());
		List<String> inputTexts = new ArrayList<>(cellValueRowIdsMap.keySet());

		logger.info("Total number of http request: " + (1 + (int) Math.ceil(cellValueRowIdsMap.size() / MAX_BODY)));
		// Java Thread Pool
		ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);
		// To note the times it takes for http requests and response to end
		long startTime = System.nanoTime();

		for (int fromIndex = 0; fromIndex < cellValueRowIdsMap.size(); fromIndex += MAX_BODY) {
			int toIndex = fromIndex + MAX_BODY;
			if (cellValueRowIdsMap.size() < toIndex)
				toIndex = cellValueRowIdsMap.size();
			pool.execute(executeRequest(inputRowIds.subList(fromIndex, toIndex)
				, inputTexts.subList(fromIndex, toIndex)));
		}

		if (waitForThreads(pool)) {
			throw new Exception("TIMEOUT: Cancel non-finished tasks for Structure Extractions");
		}
		long endTime = System.nanoTime();
		logger.info("Time to end all http Requests and deSerialize: " +
			(float) TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS) / 1000 + "sec");
	}

	/**
	 * method used to send http request to server to extract Structure.
	 *
	 * @param matrixOfRowIds
	 * @param text           list of texts
	 * @return a runnable to be used by pool of threads.
	 */
	private Runnable executeRequest(final List<List<String>> matrixOfRowIds, final List<String> text) {

		return new Runnable() {
			@Override
			public void run() {

				StringBuffer extractionsBuffer = new StringBuffer();

				//prepare body for POST request
				JSONObject httpBody = getHTTPBody();
				httpBody.put("text", text);

				try {
					URL obj;
					obj = new URL(getURL());

					HttpURLConnection con = (HttpURLConnection) obj.openConnection();

					// add request header
					con.setRequestMethod("POST");
					con.setRequestProperty("Accept", "application/json");
					con.setRequestProperty("Content-Type", "application/json");

					// Send POST request
					con.setDoOutput(true);
					BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(), StandardCharsets.UTF_8));
					bw.write(String.valueOf(httpBody));
					bw.flush();
					bw.close();

					// get Response
					BufferedReader in = new BufferedReader(new InputStreamReader(
						con.getInputStream()));
					String inputLine;

					while ((inputLine = in.readLine()) != null) {
						extractionsBuffer.append(inputLine);
					}
					in.close();

				} catch (IOException e) {
					logger.error("Error in http request to remote server " + e.getMessage());
					logger.error("Text: " + text);
				}
				if (String.valueOf(extractionsBuffer).isEmpty() || String.valueOf(extractionsBuffer).equals("[]")) {
					logger.info("No information found for text " + text);
				}
				// save Result
				filterAndSave(String.valueOf(extractionsBuffer), matrixOfRowIds);
			}
		};

	}


	/**
	 * wait for all the pool threads to end there jobs if not done in {@link #TIME_OUT} seconds it
	 * kill the rest of the processes. in case of time out http Exception is turn to true.
	 *
	 * @param pool
	 */
	private boolean waitForThreads(ExecutorService pool) {
		boolean isTimeout = false;
		try {
			pool.shutdown();
			pool.awaitTermination(this.TIME_OUT, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.error("tasks interrupted");
		} finally {
			if (!pool.isTerminated()) {
				isTimeout = true;
			}
			pool.shutdownNow();
			return isTimeout;
		}
	}

	/**
	 * add new column next to hNodeId column with info retrieved from SWEB response
	 *
	 * @param workspace
	 * @param worksheet
	 * @param model
	 * @param selection
	 * @param newColumnNameValueMap
	 * @param hTableId
	 * @param hNodeId
	 * @return
	 * @throws CommandException
	 */
	 private List<String> addNewCellValueCommand(Workspace workspace,
											   Worksheet worksheet,
											   String model,
											   SuperSelection selection,
											   Map<String, HashMap<String, CellValue>> newColumnNameValueMap,
											   String hTableId,
											   String hNodeId) throws CommandException {

		List<String> newHNodeIds = new LinkedList<>();

		List<String> reverseOrderedKeys = new ArrayList<>(newColumnNameValueMap.keySet());
		Collections.reverse(reverseOrderedKeys);

		AddCellValueCommand valueCmd;
		AddCellValueCommandFactory valueFactory = new AddCellValueCommandFactory();
		for (String key : reverseOrderedKeys) {
			valueCmd = (AddCellValueCommand) valueFactory.createCommand(newColumnNameValueMap.get(key), model, workspace,
				hNodeId, worksheet.getId(), hTableId, key, HNode.HNodeType.Transformation, selection.getName());
			valueCmd.doIt(workspace);
			newHNodeIds.add(valueCmd.getNewHNodeId());
		}
		return newHNodeIds;
	}

}
