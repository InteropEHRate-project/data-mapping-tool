package edu.isi.karma.controller.command.worksheet;

import java.lang.reflect.Type;
import java.util.*;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import edu.isi.karma.controller.command.*;
import edu.isi.karma.controller.command.selection.SuperSelection;
import edu.isi.karma.gson.adopters.immutable.ImmutableListDeserializer;
import edu.isi.karma.gson.adopters.immutable.ImmutableMapDeserializer;
import edu.isi.karma.kr2rml.exception.ValueNotFoundKarmaException;
import edu.isi.karma.rep.*;
import edu.isi.karma.util.GsonUtil;
import eu.trentorise.opendata.semtext.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a command used to extract structure from text in sub-columns using the SWEB API.
 * if successful it creates new columns next to the column where the command was executed from.
 * each node of new column is either empty or type of {@link StringCellValue}.
 *
 * @author danish.cheema@unitn.it
 */
public class ExtractStructureCommand extends SWEBNLPExtractionCommand {

	private enum AllowedPipelines {
		IEPrescriptionPipeline
	}

	/**
	 * locale to be used as a parameter for nlp pipeline.
	 */
	private final String rootLocale;
	private Map<String, HashMap<String, CellValue>> retrievedInformation;

	/**
	 * flag to keep track if SWEB IE pipeline was successful for atlest one cell value
	 */
	private Boolean hasInformation = false;

	private static final Logger logger = LoggerFactory.getLogger(ExtractStructureCommand.class);

	protected ExtractStructureCommand(String id, String model, String worksheetId, String hNodeId,
									  String selectionId, String rootLocale, String pipeline
	) {
		super(id,
			model,
			worksheetId,
			selectionId,
			hNodeId,
			pipeline,
			5000,
			1,
			1200);
		this.rootLocale = rootLocale != null ? rootLocale.toLowerCase().replace(" ", "") : "autodetect";

		addTag(CommandTag.Transformation);
	}

	@Override
	public String getCommandName() {
		return ExtractStructureCommand.class.getSimpleName();
	}

	@Override
	public String getTitle() {
		return "Structure Extraction: " + hNodeName;
	}

	@Override
	public String getDescription() {
		return "";
	}

	@Override
	public CommandType getCommandType() {
		return commandType;
	}

	@Override
	public String getInputHNodeId() throws Exception {
		return this.hNodeId;
	}

	@Override
	public String getURL() {
		//initial url link for SemText extraction pipe line
		StringBuilder url = new StringBuilder()
			.append(extractionURL)
			.append("/nlp/pipelines/")
			.append(pipeline)
			.append("/run?")
			.append("knowledgeBase=1")
			.append("&")
			.append("format=SEMTEXT")
			.append("&")
			.append("retainAllMeanings=false")
			.append("&")
			.append("processStringsIndependently=false")
			.append(rootLocale.compareTo("autodetect") == 0 ? "" : "&locale=" + rootLocale);
		return url.toString();
	}

	@Override
	public JSONObject getHTTPBody() {
		//prepare body for POST request
		JSONObject httpBody = new JSONObject();

		// FIXME: correct way to send the processStringsIndependently is in the nlpParameters map as below
		// request using nlp parameter.
//		JSONObject nlpParameters = new JSONObject();
//		nlpParameters.put("nlp.processStringsIndependently",false);
//		httpBody.put("nlpParameters", nlpParameters);
//		logger.debug("httpBody: " + JSONUtil.prettyPrintJson(httpBody.toString()));

		return httpBody;
	}

	/**
	 * parse the json and populate the {@link #retrievedInformation}.
	 * we assume that the http response data will be in the same order as request data, so we use
	 * the same row order to save back the response.
	 *
	 * @param semTextArray string used to deserialize in list of SemText.
	 * @param listOfKeys
	 */
	@Override
	public void filterAndSave(String semTextArray, List<List<String>> listOfKeys) {

		if (semTextArray == null) {
			listOfKeys.forEach(keys -> keys.forEach(key -> this.addInRetrievedInformationList(key, null)));
		} else if (!semTextArray.isEmpty() && !semTextArray.equals("[]")) {
			// Deserialize the semTextArray
			Gson gson = new GsonBuilder()
				.registerTypeAdapter(ImmutableList.class, new ImmutableListDeserializer())
				.registerTypeAdapter(ImmutableMap.class, new ImmutableMapDeserializer())
				.registerTypeAdapter(Term.class, termDeserializer)
				.create();
			Type type = new TypeToken<List<SemText>>() {
			}.getType();
			List<SemText> semArray = gson.fromJson(semTextArray, type);

			if (semArray != null) {
				Iterator<List<String>> listOfKeysItr = listOfKeys.iterator();
				for (SemText semantic : semArray) {
					if (listOfKeysItr.hasNext()) {
						List<String> keys = listOfKeysItr.next();
						keys.forEach(key -> this.addInRetrievedInformationList(key, semantic));
					}
				}
			}
		}
	}

	/**
	 * Custom deserializer for {@link Gson} to Convert SCROLL version of Term to
	 * trentorise version of {@link Term}.
	 */
	JsonDeserializer<Term> termDeserializer = new JsonDeserializer<Term>() {
		@Override
		public Term deserialize(JsonElement json,
								Type typeOfT,
								JsonDeserializationContext context) throws JsonParseException {
			return GsonUtil.INSTANCE.getTermObj(json);
		}
	};

	@Override
	public Map<String, HashMap<String, CellValue>> getRetrievedInformation(Workspace workspace) {
		if (!this.hasInformation)
			return null;
		return retrievedInformation;
	}

	@Override
	public String getInfoMessage() {
		return "Structure Extracted";
	}

	@Override
	public Boolean isAllowedPipeline(String pipeline) {
		return Arrays.stream(AllowedPipelines.values()).anyMatch(t -> t.name().equals(pipeline));
	}

	@Override
	public Map<String, List<String>> getCellValueRowIdsMap(Workspace workspace,
														   Worksheet worksheet,
														   SuperSelection selection,
														   String hNodeId) throws ValueNotFoundKarmaException {
		Map<String, List<String>> cellValueRowIdsMap = getCellValueRowIds(workspace, worksheet, selection, hNodeId);
		Iterator<Map.Entry<String, List<String>>> iterator = cellValueRowIdsMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, List<String>> entry = iterator.next();
			String text = entry.getKey();
			List<String> rowIds = entry.getValue();
			if (Strings.isNullOrEmpty(text.trim())) {
				filterAndSave(
					null,
					new LinkedList<List<String>>() {{
						add(rowIds);
					}}
				);
				iterator.remove();
			}
		}
		return cellValueRowIdsMap;
	}

	/**
	 * @param rowId
	 * @param semText
	 * @return
	 */
	private synchronized void addInRetrievedInformationList(String rowId, SemText semText) {
		if (retrievedInformation == null) {
			retrievedInformation = new LinkedHashMap<>();
		}
		if (semText != null) {
			// create a data structure with key as new column name and value Map of rowId and cellValue
			Map<String, CellValue> annSteams = new HashMap<>();
			for (Sentence sentence : semText.getSentences()) {
				for (Term term : sentence.getTerms()) {
					if (term.getMetadata().containsKey("stems") && term.getMetadata().containsKey("annotations")) {
						ArrayList value = ((ArrayList) term.getMetadata().get("stems"));
						ArrayList columns = (ArrayList) term.getMetadata().get("annotations");
						if (columns.size() > 0) {
							for (Object columnName : columns) {
								columnName = columnName.toString().replace("/", "-");
								this.hasInformation = Boolean.TRUE;
								if (!retrievedInformation.containsKey(columnName.toString())) { // if column doesn't exist
									retrievedInformation.put(
										columnName.toString(),
										new HashMap<String, CellValue>() {{
											put(rowId, new StringCellValue(value.get(0).toString()));
										}}
									);
								} else { // if column already exist
									retrievedInformation.get(columnName).put(rowId, new StringCellValue(value.get(0).toString()));
								}
							}
						}
					}
				}
			}
		}
		// if no semtext found for this row add empty value for the row
		else {
			retrievedInformation.forEach((k, v) -> {
				retrievedInformation.get(k).put(rowId, new StringCellValue(""));
			});
		}
	}
}
