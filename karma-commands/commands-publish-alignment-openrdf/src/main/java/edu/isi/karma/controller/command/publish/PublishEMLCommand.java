package edu.isi.karma.controller.command.publish;

import edu.isi.karma.controller.command.CommandType;
import edu.isi.karma.controller.command.WorksheetSelectionCommand;
import edu.isi.karma.controller.update.AbstractUpdate;
import edu.isi.karma.controller.update.ErrorUpdate;
import edu.isi.karma.controller.update.UpdateContainer;
import edu.isi.karma.rep.Worksheet;
import edu.isi.karma.rep.Workspace;
import edu.isi.karma.util.JSONUtil;
import edu.isi.karma.util.Util;
import edu.isi.karma.view.VWorkspace;
import edu.isi.karma.webserver.ContextParametersRegistry;
import edu.isi.karma.webserver.ServletContextParameterMap;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * command class to handle publish eml request. command uses remote api to convert RDF to EML.
 *
 * @author Danish <danishasghar.cheema@studenti.unitn.it>
 * @date: 2019-10-01
 */
public class PublishEMLCommand extends WorksheetSelectionCommand {

	private String worksheetName;
	private String emlServiceURL;

	public enum PublishEMLCommandJsonKeys
	{
		updateType, fileUrl, worksheetId, errorReport
	}

	private static Logger logger = LoggerFactory
			.getLogger(PublishEMLCommand.class);


	protected PublishEMLCommand(String id, String model, String worksheetId, String selectionId, String emlServiceURL) {
		super(id, model, worksheetId, selectionId);
		this.emlServiceURL = emlServiceURL;
	}

	@Override
	public String getCommandName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String getTitle() {
		return "Publish EML";
	}

	@Override
	public String getDescription() {
		return this.worksheetName;
	}

	@Override
	public CommandType getCommandType() {
		return CommandType.notUndoable;
	}

	@Override
	public UpdateContainer doIt(Workspace workspace) {
		Worksheet worksheet = workspace.getWorksheet(worksheetId);
		this.worksheetName = worksheet.getTitle();

		final ServletContextParameterMap contextParameters
				= ContextParametersRegistry.getInstance().getContextParameters(workspace.getContextId());

		final String rdfFileName = workspace.getCommandPreferencesId() + worksheetId + ".ttl";
		final String rdfFileLocalPath
				= contextParameters.getParameterValue(ServletContextParameterMap.ContextParameter.RDF_PUBLISH_DIR)
						+ rdfFileName;

		final String emlFileName = workspace.getCommandPreferencesId() + worksheetId + ".eml";
		final String emlFileLocalPath
				= contextParameters.getParameterValue(ServletContextParameterMap.ContextParameter.EML_PUBLISH_DIR)
				+ emlFileName;

		try {
			String rdf = new String(Files.readAllBytes(Paths.get(rdfFileLocalPath)), StandardCharsets.UTF_8);
			StringBuffer eml = getEML(rdf);

			if(eml != null){
				BufferedWriter out = new BufferedWriter(new FileWriter(emlFileLocalPath));
				out.write(JSONUtil.prettyPrintJson(eml.toString()));
				out.flush();
				out.close();
			}
			else {
				throw new Exception("unable to convert to EML");
			}

			return new UpdateContainer(new AbstractUpdate() {
				public void generateJson(String prefix, PrintWriter pw,
										 VWorkspace vWorkspace) {
					JSONObject outputObject = new JSONObject();
					try {
						outputObject.put(PublishEMLCommandJsonKeys.updateType.name(), "PublishEMLUpdate");
						outputObject.put(PublishEMLCommandJsonKeys.fileUrl.name(),
								contextParameters.getParameterValue(
										ServletContextParameterMap.ContextParameter.EML_PUBLISH_RELATIVE_DIR)
										+ emlFileName);
						outputObject.put(PublishEMLCommandJsonKeys.worksheetId.name(), worksheetId);
						pw.println(outputObject.toString(3));
					} catch (JSONException e) {
						logger.error("Error occured while generating JSON!");
					}
				}
			});
		} catch (Exception e) {
			logger.error("Error in EML Conversion: " + e);
			Util.logException(logger, e);
			return new UpdateContainer(new ErrorUpdate(e.getMessage()));
		}
	}

	@Override
	public UpdateContainer undoIt(Workspace workspace) {
		return null;
	}

	private StringBuffer getEML(String rdf){

//		initial url link for concept extraction pipe line
		StringBuffer url = new StringBuffer();
		url.append(emlServiceURL);
		url.append("/converter/rdf");

		try {
			URL obj;
			obj = new URL(url.toString());

			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			// add request header
			con.setRequestMethod("POST");
			con.setRequestProperty("Accept", "application/json");
			con.setRequestProperty("Content-Type", "text/plain");

			// Send POST request
			con.setDoOutput(true);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(con.getOutputStream()
							, "UTF-8"));
			bw.write(rdf);
			bw.flush();
			bw.close();

			// get Response
			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String inputLine;
			StringBuffer extractionsBuffer = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				extractionsBuffer.append(inputLine);
			}
			in.close();

			return extractionsBuffer;


		}catch (IOException e){
			logger.error("Error in connection with remote server " + e);
			Util.logException(logger, e);
			return null;
		}
	}
}
