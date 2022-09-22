package edu.isi.karma.controller.command.worksheet;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import edu.isi.karma.rep.CellValue;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class ExtractStructureCommandTest {

	ExtractStructureCommand nullCommand;

	@BeforeEach
	void setUp() {
		nullCommand = new ExtractStructureCommand(
			null, null, null, null, null, null, null
		);
	}

	@AfterEach
	void tearDown() {
	}

	@Test
	void doIt() {

	}

	@Test
	void undoIt() {
	}

	@Test
	void retrieveConcepts() {

	}

	@Test
	void filterAndSave() {
		String semTextArrayInput = null;
		String semTextArrayOutput = null;

		// test with normal input
		try {
			File file1 = new File(getClass().getClassLoader().getResource("extractStructure_normal_input.json").toURI());
			File file2 = new File(getClass().getClassLoader().getResource("extractStructure_normal_output.json").toURI());
			semTextArrayInput = FileUtils.readFileToString(file1, StandardCharsets.UTF_8);
			semTextArrayOutput = FileUtils.readFileToString(file2, StandardCharsets.UTF_8);
		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
		}
		Assertions.assertNotNull(semTextArrayInput, "semTextArrayInput should not be empty");
		Assertions.assertNotNull(semTextArrayOutput, "semTextArrayOutput should not be empty");
		nullCommand.filterAndSave(semTextArrayInput, Arrays.asList(Arrays.asList("1")));
		Gson gson = new GsonBuilder().create();
		Type type = new TypeToken<Map<String, HashMap<String, CellValue>>>() {}.getType();
		String retreatedInformation = gson.toJson(nullCommand.getRetrievedInformation(null), type);
		Assertions.assertEquals(JsonParser.parseString(retreatedInformation), JsonParser.parseString(semTextArrayOutput));

		// test with no semText found input
//		try {
//			File file = new File(getClass().getClassLoader().getResource("extractSemText_emptyField.json").toURI());
//			semTextArrayInput = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
//		} catch (URISyntaxException | IOException e) {
//			e.printStackTrace();
//		}
//		Assertions.assertNotNull(semTextArrayInput, "semTextArray should not be empty");
//		nullCommand.filterAndSave(semTextArrayInput, Arrays.asList("1", "2", "3"));
//		semTextArrayOutput = new StringBuilder()
//			.append("[")
//			.append(nullCommand.getSemText("1"))
//			.append(",")
//			.append(nullCommand.getSemText("2"))
//			.append(",")
//			.append(nullCommand.getSemText("3"))
//			.append("]")
//			.toString();
//		Object emptyObj = JSONUtil.createJson(nullCommand.getSemText("3"));
//		Assertions.assertTrue(emptyObj instanceof JSONObject);
//		Object emptySen = JSONUtil.createJson(((JSONObject) emptyObj).get("sentences").toString());
//		Assertions.assertTrue(emptySen instanceof JSONArray);
//		Assertions.assertEquals(((JSONArray) emptySen).length(), 0);
//		Assertions.assertEquals(JsonParser.parseString(semTextArrayInput), JsonParser.parseString(semTextArrayOutput));

	}

	@Test
	void addInRetrievedSemTextList() {
	}

	@Test
	void waitForThreads() {
	}

	@Test
	void getMeaningObj() {
	}

}
