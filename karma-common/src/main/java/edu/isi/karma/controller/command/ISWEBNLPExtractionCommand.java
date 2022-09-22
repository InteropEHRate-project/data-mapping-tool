package edu.isi.karma.controller.command;


import edu.isi.karma.controller.command.selection.SuperSelection;
import edu.isi.karma.kr2rml.exception.ValueNotFoundKarmaException;
import edu.isi.karma.rep.CellValue;
import edu.isi.karma.rep.Worksheet;
import edu.isi.karma.rep.Workspace;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ISWEBNLPExtractionCommand {

	/**
	 * method to format the input for SWEB NLP Pipeline
	 * @return
	 */
	String getInputHNodeId() throws Exception;

	/**
	 * method to format the SWEB endpoint url with all the query parameters
	 * @return
	 */
	String getURL();

	/**
	 * method to add any additional information into body other than text
	 * @return
	 */
	JSONObject getHTTPBody();

	/**
	 * method on how to process the SWEB response
	 * @param swebResponse
	 * @param matrixOfRowIds
	 */
	void filterAndSave(String swebResponse, List<List<String>> matrixOfRowIds);

	/**
	 * method on what information will be added to new columns
	 * a map of <column-name <rowID, CellValue>>
	 */
	Map<String, HashMap<String, CellValue>> getRetrievedInformation(Workspace workspace);

	/**
	 * what messages to display in case of successful command execution
	 * @return
	 */
	String getInfoMessage();

	/**
	 * check is the given pipeline is supported for a given command
	 * @param pipeline
	 * @return
	 */
	Boolean isAllowedPipeline(String pipeline);

	/**
	 * return the final map to create new columns
	 * @param workspace
	 * @param worksheet
	 * @param selection
	 * @param hNodeId
	 * @return
	 * @throws ValueNotFoundKarmaException
	 */
	Map<String, List<String>> getCellValueRowIdsMap(Workspace workspace,
													Worksheet worksheet,
													SuperSelection selection,
													String hNodeId) throws ValueNotFoundKarmaException;
}
