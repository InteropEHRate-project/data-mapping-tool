package edu.isi.karma.util;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import edu.isi.karma.gson.adopters.RuntimeTypeAdapterFactory;
import edu.isi.karma.gson.adopters.immutable.ImmutableListDeserializer;
import edu.isi.karma.gson.adopters.immutable.ImmutableListMultimapAdapter;
import edu.isi.karma.gson.adopters.immutable.ImmutableMapDeserializer;
import edu.isi.karma.rep.CellValue;
import edu.isi.karma.rep.ConceptCellValue;
import edu.isi.karma.rep.StringCellValue;
import eu.trentorise.opendata.commons.Dict;
import eu.trentorise.opendata.semtext.Meaning;
import eu.trentorise.opendata.semtext.MeaningKind;
import eu.trentorise.opendata.semtext.MeaningStatus;
import eu.trentorise.opendata.semtext.Term;
import it.unitn.disi.languages.utils.LanguageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * enum which uses Gson library and {@link edu.isi.karma.gson.adopters} package to Serialize and deserialize the data
 *
 * @author Danish danish.cheema@unitn.it
 */
public enum GsonUtil {
	INSTANCE;

	private static final Logger logger = LoggerFactory.getLogger(GsonUtil.class);

	private Gson cellValueGson;
	private Gson immutableGson;

	GsonUtil(){

		cellValueGson = new GsonBuilder()
				.registerTypeAdapterFactory(RuntimeTypeAdapterFactory.of(CellValue.class)
						.registerSubtype(ConceptCellValue.class)
						.registerSubtype(StringCellValue.class))
				.registerTypeAdapter(ImmutableList.class, new ImmutableListDeserializer())
				.registerTypeAdapter(ImmutableMap.class, new ImmutableMapDeserializer())
				.registerTypeAdapter(ImmutableListMultimap.class, new ImmutableListMultimapAdapter())
				.create();

		immutableGson = new GsonBuilder()
				.registerTypeAdapter(ImmutableList.class, new ImmutableListDeserializer())
				.registerTypeAdapter(ImmutableMap.class, new ImmutableMapDeserializer())
				.registerTypeAdapter(ImmutableListMultimap.class, new ImmutableListMultimapAdapter())
				.create();
	}


	/**
	 * in order to serialize or Deserialize the objects which uses Java Polymorphism it uses
	 * {@link RuntimeTypeAdapterFactory} with Gson and attaches {@link ConceptCellValue} and {@link StringCellValue}
	 * where both of these classes extends {@link CellValue}. Moreover Guava Immutable List, Map and ListMultimap
	 * type adopter are also registered to it
	 *
	 * @return Gson object
	 */
	public Gson getCellValueGsonObj() {
		return cellValueGson;
	}

	/**
	 * get gson object to be used for serializing and deserializing Guava Immutable List, Map and Multimap
	 *
	 * @return
	 */
	public Gson getImmutableGsonObj() {
		return immutableGson;
	}

	/**
	 * Custom Deserializer method to get a {@link Meaning} object.
	 * if there is no concept id/name found it returns the empty {@link Meaning} Object.
	 *
	 * @param json
	 * @return
	 */
	public Meaning getMeaningObj(JsonElement json) {
		JsonObject jsonObject = json.getAsJsonObject();

		String id;
		MeaningKind kind;
		Double probability;
		Dict name , description;
		ImmutableMap.Builder<String, String> metadata = ImmutableMap.builder();

		try{
			id = jsonObject.get("id").getAsString();
			Map.Entry<String,JsonElement> conceptName0 = jsonObject.getAsJsonObject("name")
				.getAsJsonObject().entrySet().iterator().next();
			Locale nameLocale = new LanguageUtils().getLocale(conceptName0.getKey());
			name = Dict.of(nameLocale,conceptName0.getValue().getAsString());
		}catch (NoSuchElementException | NullPointerException e){
//			logger.warn("Either name/id element does not exist or its empty, " +
//					"so null Meaning object is returned ");
			return null;
		}
		try{
			kind = MeaningKind.valueOf(jsonObject.get("kind").getAsString());
			probability = Double.valueOf(jsonObject.get("probability").getAsString());
		}catch (NoSuchElementException | NullPointerException e){
//			logger.warn("No Kind and/or probability found while deserializing the Concept Object, " +
//					"CONCEPT and 1.0 will be used respectively ");
			kind = MeaningKind.valueOf("CONCEPT");
			probability = 1.0;
		}
		try{
			Map.Entry<String,JsonElement> conceptDescription0 = jsonObject.getAsJsonObject("description")
				.getAsJsonObject().entrySet().iterator().next();
			Locale descLocale = new LanguageUtils().getLocale(conceptDescription0.getKey());
			description = Dict.of(descLocale, conceptDescription0.getValue().getAsString());
		}catch (NoSuchElementException | NullPointerException e){
//			logger.warn("Either description element does not exist or its empty ");
			description = Dict.of();
		}

		try{
			Iterator<Map.Entry<String,JsonElement>> conceptMetadata = jsonObject.getAsJsonObject("metadata")
				.getAsJsonObject("nltext").getAsJsonObject().entrySet().iterator();
			while (conceptMetadata.hasNext()){
				Map.Entry<String,JsonElement> next = conceptMetadata.next();
				metadata.put(next.getKey(),next.getValue().getAsString());
			}
		}
		catch (NoSuchElementException | NullPointerException e){
//			logger.warn("No metaData found while deserializing will use null value ");
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
	 * Custom Deserializer method to get a {@link Term} object.
	 * if there is no concept id/name found it returns the empty {@link Term} Object.
	 *
	 * @param json
	 * @return
	 */
	public Term getTermObj(JsonElement json) {
		JsonObject jsonObject = json.getAsJsonObject();

		Integer start = null;
		Integer end = null;
		ImmutableList.Builder<Meaning> meanings = ImmutableList.builder();
		MeaningStatus meaningStatus = null;
		Meaning selectedMeaning = null;
		ImmutableMap.Builder<String, ArrayList> metadata = ImmutableMap.builder();

		try{
			if (jsonObject.has("start"))
				start = jsonObject.get("start").getAsInt();
			if (jsonObject.has("end"))
				end = jsonObject.get("end").getAsInt();
			if (jsonObject.has("meanings")) {
				Iterator<JsonElement> meaning = jsonObject.getAsJsonArray("meanings").iterator();
				while (meaning.hasNext()) {
					JsonElement next = meaning.next();
					meanings.add(getMeaningObj(next));
				}
			}
			if (jsonObject.has("meaningStatus"))
				meaningStatus = MeaningStatus.valueOf(jsonObject.get("meaningStatus").getAsString());
			if (jsonObject.has("selectedMeaning"))
				selectedMeaning = getMeaningObj(jsonObject.get("selectedMeaning").getAsJsonObject());
			if (jsonObject.has("metadata")) {
				Iterator<Map.Entry<String, JsonElement>> teamMetadata = jsonObject.getAsJsonObject("metadata")
					.getAsJsonObject("nltext").getAsJsonObject().entrySet().iterator();
				while (teamMetadata.hasNext()) {
					Map.Entry<String, JsonElement> next = teamMetadata.next();
					if (next.getKey().equals("stems") || next.getKey().equals("annotations")) {
						ArrayList values = new ArrayList();
						if (next.getValue() != null && next.getValue() instanceof JsonArray) {
							Iterator<JsonElement> listValue = next.getValue().getAsJsonArray().iterator();
							while (listValue.hasNext()) {
								JsonElement value = listValue.next();
								values.add(value.getAsString());
							}
						}
						metadata.put(next.getKey(), values);
					}
				}
			}
		}
		catch (NoSuchElementException | NullPointerException | IllegalStateException e){
			logger.error("SWEB response doesn't match expected format");
			logger.error("SWEB response: " + json);
			logger.error("SWEB expected response: \"start\": 0,\n" +
				"                        \"end\": 11,\n" +
				"                        \"meaningStatus\": \"TO_DISAMBIGUATE\",\n" +
				"                        \"meanings\": [],\n" +
				"                        \"metadata\": {\n" +
				"                            \"nltext\": {\n" +
				"                                \"stems\": [\n" +
				"                                    \"Bisoprololo\"\n" +
				"                                ],\n" +
				"                                \"annotations\":[\"Prescr:DrugIngredient_1\"],\n" +
				"                                \"relation\":\"[Prescr:DrugIngredient_1] [Prescr:DrugProduct_1]\",\n" +
				"                                \"derivedLemmas\":[]\n" +
				"                            }\n" +
				"                        }" );
			return Term.of(0,0,MeaningStatus.NOT_SURE,null);
		}

		if ( (start < 0 || end < 0))
			return Term.of(0,0,MeaningStatus.NOT_SURE,null);

		return Term.of(
			start,
			end,
			meaningStatus,
			selectedMeaning,
			meanings.build(),
			metadata.build()
		);
	}
}
