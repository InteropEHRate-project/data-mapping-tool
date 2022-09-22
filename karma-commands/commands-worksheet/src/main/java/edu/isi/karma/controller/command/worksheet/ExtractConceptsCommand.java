package edu.isi.karma.controller.command.worksheet;

import java.lang.reflect.Type;
import java.util.*;

import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import edu.isi.karma.kr2rml.exception.ValueNotFoundKarmaException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.isi.karma.controller.command.CommandType;
import edu.isi.karma.controller.command.selection.SuperSelection;
import edu.isi.karma.er.helper.CloneTableUtils;
import edu.isi.karma.util.Util;
import edu.isi.karma.gson.adopters.immutable.ImmutableListDeserializer;
import edu.isi.karma.gson.adopters.immutable.ImmutableMapDeserializer;
import edu.isi.karma.rep.*;
import edu.isi.karma.rep.Table;
import edu.isi.karma.util.JSONUtil;

import eu.trentorise.opendata.commons.Dict;
import eu.trentorise.opendata.semtext.*;
import it.unitn.disi.languages.utils.LanguageUtils;


/**
 * a command used to extract concepts using the external API via Http request.
 * if successful it creates new column next to the column where the command was executed from.
 * each node of new column is either empty or type of {@link ConceptCellValue}.
 *
 * @author danish.cheema@unitn.it
 */


public class ExtractConceptsCommand extends SWEBNLPExtractionCommand {

	public enum Preference {
		mustHave, niceToHave, canIgnore
	}

	private enum AllowedPipelines {
		ConceptExtractionPipeline, SingleWordConceptExtractionPipeline
	}
	/**
	 * root concept local Id used for nlp pipeline
	 */
	private final Long rootConcept;
	/**
	 * preference value of {@link Preference}
	 */
	private final String preferenceLevel;
	/**
	 * locale to be used as a parameter for nlp pipeline.
	 */
	private final String rootLocale;
	/**
	 * text to Concept Array. used to map given attrebute value to a selected concept
	 */
	private final Map<String, Meaning> textToConceptArray;
	/**
	 * save all the retrieved concept with the rowId.
	 * Key is the rowId and Value is list of possible concepts.
	 */
	private Map<String, List<Concept>> retrievedConceptList = new HashMap<>();
	/**
	 * list of unique concepts found in the data. to be used for {@link Preference#mustHave} option.
	 */
	private ConceptCellValue uniqueConceptList = new ConceptCellValue();

	private static final Logger logger = LoggerFactory.getLogger(ExtractConceptsCommand.class);

	protected ExtractConceptsCommand(String id, String model, String worksheetId, String hNodeId,
									 String extractionURL,
									 String highLevelConcept, String selectionId,
									 String rootLocale, String preferenceLevel,
									 String textToConceptArray, String pipeline
	) {
		super(id,
			model,
			worksheetId,
			selectionId,
			hNodeId,
			pipeline,
			5000,
			1,
			120);
		this.rootConcept = highLevelConcept.compareTo("") == 0 ? null : Long.valueOf(highLevelConcept);
		this.rootLocale = rootLocale.toLowerCase().replace(" ", "");
		this.preferenceLevel = preferenceLevel.replace(" ", "");
		this.textToConceptArray = deSerializeText2Concept(textToConceptArray);

		addTag(CommandTag.Transformation);
	}


	@Override
	public String getCommandName() {
		return ExtractConceptsCommand.class.getSimpleName();
	}

	@Override
	public String getTitle() {
		return "Concept Extraction: " + hNodeName;
	}

	@Override
	public String getDescription() {
		return "";
	}

	@Override
	public CommandType getCommandType() {
		return this.commandType;
	}

	@Override
	public String getInputHNodeId() throws Exception {
		return hNodeId;
	}

	@Override
	public String getURL() {
		//initial url link for SemText extraction pipe line
		StringBuffer url = new StringBuffer()
			.append(extractionURL)
			.append("/nlp/pipelines/")
			.append(pipeline)
			.append("/run?")
			.append("knowledgeBase=1")
			.append("&format=SEMTEXT")
			.append("&retainAllMeanings=false")
			.append(rootLocale.compareTo("autodetect") == 0 ? "" : "&locale=" + rootLocale);
		return url.toString();
	}

	@Override
	public JSONObject getHTTPBody() {
		//prepare body for POST request
		JSONObject httpBody = new JSONObject();

		// in case if the root concept is defined we send it in the body of the http
		// request using nlp parameter.
		JSONObject nlpParameters = new JSONObject();
		nlpParameters.put("nlp.conceptConstraint",
			rootConcept == null ? JSONObject.NULL : new JSONObject().put("type", ".ListLongParameterValue")
				.put("value", new JSONArray().put(rootConcept)));
		httpBody.put("nlpParameters", nlpParameters);
		logger.info("httpBody: " + JSONUtil.prettyPrintJson(httpBody.toString()));

		return httpBody;
	}

	/**
	 * parse the json and populate the {@link #retrievedConceptList} and {@link #uniqueConceptList}.
	 * we assume that the http response data will be in the same order as request data, so we use
	 * the same row order to save back the response.
	 *
	 * @param concepts     string used to deserialize in list of {@link SemText}.
	 * @param matrixOfRowIds
	 */
	@Override
	public void filterAndSave(String concepts, List<List<String>> matrixOfRowIds) {
		if (!Strings.isNullOrEmpty(concepts)) {
			try {
				// Deserialize the semText
				Gson gson = new GsonBuilder()
					.registerTypeAdapter(ImmutableList.class, new ImmutableListDeserializer())
					.registerTypeAdapter(ImmutableMap.class, new ImmutableMapDeserializer())
					.registerTypeAdapter(Meaning.class, meaningDeserializer)
					.create();
				Type type = new TypeToken<List<SemText>>() {
				}.getType();
				List<SemText> semArray = gson.fromJson(concepts, type);

				if (semArray != null) {
					Iterator<List<String>> matrixofKeysItr = matrixOfRowIds.iterator();
					for (SemText semantic : semArray) {
						// for each concept there can be multiple rowIds associated to it.
						// it will loop over all of them and attach this concept to it.
						if (matrixofKeysItr.hasNext()) {
							List<String> listofkeys = matrixofKeysItr.next();
							for (String key : listofkeys) {
								boolean atleastOneConceptFound = false;
								for (Sentence sentence : semantic.getSentences()) {
									if (sentence.getTerms() != null) {
										for (Term t : sentence.getTerms()) {
											atleastOneConceptFound = true;
											Meaning selectedMeaning = t.getSelectedMeaning();
											addIntoUniqueConcept(selectedMeaning);
											addInRetrievedConceptList(key, selectedMeaning);
											// add all the possible meaning attached to a token. and select as
											// default the selected meaning by WSD algorithm of nlp pipeline.
											for (Meaning m : t.getMeanings()) {
												if (m.getId().compareTo(selectedMeaning.getId()) != 0) {
													addIntoUniqueConcept(m);
													addInRetrievedConceptList(key, m);
												}
											}
										}
									}
								}
								if (!atleastOneConceptFound) { // handel the noConceptFound case
									addInRetrievedConceptList(key, null);
								}
							}
						}
					}
				}
			} catch (Exception e) {
				logger.error("Error in parsing Concepts from TextToConcept Array " + e);
				Util.logException(logger, e);
			}
		} else {
			matrixOfRowIds.forEach(keys -> keys.forEach( key -> this.addInRetrievedConceptList(key, null)));
		}
	}

	@Override
	public Map<String, HashMap<String, CellValue>> getRetrievedInformation(Workspace workspace) {

		// if no unique concept is found.
		if(uniqueConceptList.getConcepts().isEmpty()){
			return null;
		}


		Worksheet worksheet = workspace.getWorksheet(worksheetId);
		SuperSelection selection = getSuperSelection(worksheet);
		RepFactory repFactory = workspace.getFactory();

		// prepare data tobe added in worksheet at this point we already know that at least one concept was
		// extracted successfully. so we will create a new column of concepts and leave blank the nodes which
		// doesnt have any concept extracted at least in the case of "can ignore" option of user preferences.

		HNode hnode = repFactory.getHNode(this.hNodeId);
		String newColumnName = hnode.getColumnName() + " Concepts";

		//head table of the current head node
		HTable ht = repFactory.getHTable(repFactory.getHNode(this.hNodeId).getHTableId());
		//list of tables
		List<Table> tables = new ArrayList<>();
		CloneTableUtils.getDatatable(worksheet.getDataTable(), ht, tables, selection);

		HashMap<String, HashMap<String, CellValue>> addValues = new HashMap<>();
		addValues.put(newColumnName, new HashMap<>());
		for (Table table : tables) {
			ArrayList<Row> rows = table.getRows(0, table.getNumRows(), selection);
			for (Row row : rows) {
				ConceptCellValue value = getConcepts(row.getId());
				addValues.get(newColumnName).put(row.getId(), value != null ? value : new StringCellValue(null));
			}
		}
		return addValues;
	}

	@Override
	public String getInfoMessage() {
		return "Concepts Extracted";
	}

	@Override
	public Boolean isAllowedPipeline(String pipeline){
		return Arrays.stream(AllowedPipelines.values()).anyMatch(t -> t.name().equals(pipeline));
	}

	/**
	 * add unique concept in the list tobe used in case of
	 * {@link Preference#mustHave} or {@link Preference#niceToHave}.
	 *
	 * @param meaning
	 */
	private synchronized void addIntoUniqueConcept(Meaning meaning) {
		int flag = 0;
		if (meaning != null && uniqueConceptList != null) {
			for (Concept concept : uniqueConceptList.getConcepts()) {
				if (concept.getMeaning().getId().equals(meaning.getId())) {
					// concept already exist in the list
					flag = 1;
					break;
				}
			}
			if (flag == 0) {
				//add a new concept in the list
				// TODO for now it select the first concept as preselected.
				//  algorithm can be improved
				if (uniqueConceptList.getConcepts().isEmpty()) {
					uniqueConceptList.getConcepts().add(new Concept(meaning, true));
				} else {
					uniqueConceptList.getConcepts().add(new Concept(meaning, false));
				}
			}
		}
	}


	/**
	 * populates the list of concepts given the rowid and sets isSelected to true for first concept.
	 * isSelected is used to identify the default meaning for a given row.
	 *
	 * @param rowId
	 * @param meaning
	 */
	private synchronized void addInRetrievedConceptList(String rowId, Meaning meaning) {
		if (meaning != null) {
			if (retrievedConceptList.get(rowId) != null) { // if Concept List for given rowId already exist
				retrievedConceptList.get(rowId).add(new Concept(meaning, false));
			} else {
				List<Concept> list = new LinkedList<>();
				list.add(new Concept(meaning, true));
				retrievedConceptList.put(rowId, list);
			}
		} else {
			retrievedConceptList.put(rowId, null);
		}
	}

	/**
	 * converting jsonArray to a Map, where each json object consist of a string and
	 * a {@link Meaning} associate to it.
	 *
	 * @param conceptForTextArray
	 * @return
	 */
	private Map<String, Meaning> deSerializeText2Concept(String conceptForTextArray) {
		try {
			Gson gson = new GsonBuilder().create();
			JsonElement map = gson.fromJson(conceptForTextArray, JsonElement.class);

			Map<String, Meaning> array = new HashMap<>();
			for (JsonElement elm : map.getAsJsonArray()) {
				JsonObject obj = elm.getAsJsonObject();
				array.put(obj.get("text").getAsString(), getMeaningObj(obj.get("concept")));
			}
			return array;
		} catch (Exception e) {
			logger.error("Error in parsing Concepts from TextToConcept Array " + e);
			Util.logException(logger, e);
		}
		return null;
	}

	/**
	 * Custom deserializer for {@link Gson} to Convert SCROLL version of Meaning to
	 * trentorise version of {@link Meaning}.
	 */
	JsonDeserializer<Meaning> meaningDeserializer = (json, typeOfT, context) -> getMeaningObj(json);

	/**
	 * Custom Deserializer method to get a {@link Meaning} object.
	 * if there is no concept id/name found it returns the empty {@link Meaning} Object.
	 *
	 * @param json
	 * @return
	 */
	private Meaning getMeaningObj(JsonElement json) {
		JsonObject jsonObject = json.getAsJsonObject();

		String id;
		MeaningKind kind;
		Double probability;
		Dict name, description;
		ImmutableMap.Builder<String, String> metadata = ImmutableMap.builder();

		try {
			id = jsonObject.get("id").getAsString();
			Map.Entry<String, JsonElement> conceptName0 = jsonObject.getAsJsonObject("name")
				.getAsJsonObject().entrySet().iterator().next();
			Locale nameLocale = new LanguageUtils().getLocale(conceptName0.getKey());
			name = Dict.of(nameLocale, conceptName0.getValue().getAsString());
		} catch (NoSuchElementException | NullPointerException e) {
			return Meaning.of();
		}
		try {
			kind = MeaningKind.valueOf(jsonObject.get("kind").getAsString());
			probability = Double.valueOf(jsonObject.get("probability").getAsString());
		} catch (NoSuchElementException | NullPointerException e) {
			kind = MeaningKind.valueOf("CONCEPT");
			probability = 1.0;
		}
		try {
			Map.Entry<String, JsonElement> conceptDescription0 = jsonObject.getAsJsonObject("description")
				.getAsJsonObject().entrySet().iterator().next();
			Locale descLocale = new LanguageUtils().getLocale(conceptDescription0.getKey());
			description = Dict.of(descLocale, conceptDescription0.getValue().getAsString());
		} catch (NoSuchElementException | NullPointerException e) {
			description = Dict.of();
		}

		try {
			Iterator<Map.Entry<String, JsonElement>> conceptMetadata = jsonObject.getAsJsonObject("metadata")
				.getAsJsonObject("nltext").getAsJsonObject().entrySet().iterator();
			while (conceptMetadata.hasNext()) {
				Map.Entry<String, JsonElement> next = conceptMetadata.next();
				metadata.put(next.getKey(), next.getValue().getAsString());
			}
		} catch (NoSuchElementException | NullPointerException e) {
			logger.warn("No metaData found while deserializing will use null value ");
		}

		return Meaning.of(
			id,
			kind,
			probability,
			name,
			description,
			metadata.build()
		);
	}

	/**
	 * get all the concepts associated to a given rowId.
	 * it returns {@link ConceptCellValue} object either with list of {@link Concept} or
	 * {@link #uniqueConceptList} according to the user {@link Preference}.
	 *
	 * @param key
	 * @return list of concept
	 */
	private ConceptCellValue getConcepts(String key) {

		List<Concept> meaningList = retrievedConceptList.get(key);

		if (Preference.mustHave.name().equalsIgnoreCase(this.preferenceLevel)) {
			return mustGetConcept(meaningList);
		} else if (Preference.niceToHave.name().equalsIgnoreCase(this.preferenceLevel)) {
			return mustGetConcept(meaningList);
		} else if (Preference.canIgnore.name().equalsIgnoreCase(this.preferenceLevel)) {
			return meaningList == null ? null : new ConceptCellValue(meaningList);
		}
		return null;
	}

	/**
	 * in case if no concepts found it will return {@link #uniqueConceptList}
	 * else the same concepts
	 *
	 * @param concepts
	 * @return list of concepts or null in case of no concept found.
	 */
	private ConceptCellValue mustGetConcept(List<Concept> concepts) {

		if (concepts == null) { //if system didn't find any concepts for a given rowId
			if (uniqueConceptList != null)
				return this.uniqueConceptList;
			return null;
		}
		return new ConceptCellValue(concepts);
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
			else if (textToConceptArray != null && textToConceptArray.size() > 0 ) {
				for (Map.Entry<String, Meaning> e : textToConceptArray.entrySet()) {
					if (text.toLowerCase().contains(e.getKey().toLowerCase())) {
						addIntoUniqueConcept(e.getValue());
						rowIds.forEach(rowId -> addInRetrievedConceptList(rowId, e.getValue()));
						iterator.remove();
					}
				}
			}
		}
		return cellValueRowIdsMap;
	}
}
