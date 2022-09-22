package edu.isi.karma.controller.command.importdata;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import edu.isi.karma.controller.command.Command;
import edu.isi.karma.controller.command.CommandFactory;
import edu.isi.karma.rep.Workspace;
import edu.isi.karma.util.FileUtil;
import edu.isi.karma.webserver.ContextParametersRegistry;
import edu.isi.karma.webserver.ServletContextParameterMap;
import edu.isi.karma.webserver.ServletContextParameterMap.ContextParameter;

public class ImportJSONLinesFileCommandFactory extends CommandFactory {

	public enum Arguments {
		revisedWorksheet, useDownloadedFile, fileName, datasetName
	}

	@Override
	public Command createCommand(HttpServletRequest request, Workspace workspace) {
		ServletContextParameterMap contextParameters = ContextParametersRegistry.getInstance().getContextParameters(workspace.getContextId());

		File uploadedFile;
		if(request.getParameter(Arguments.useDownloadedFile.name()) == null){
			uploadedFile = FileUtil.downloadFileFromHTTPRequest(request, contextParameters.getParameterValue(ContextParameter.USER_UPLOADED_DIR));
		}else{
			uploadedFile = FileUtil.getDownloadedFile(request.getParameter(Arguments.fileName.name()),
					request.getParameter(Arguments.datasetName.name()),
					contextParameters.getParameterValue(ContextParameter.USER_CATALOG_DOWNLOAD_DIRECTORY));
		}
		
		if(request.getParameter(Arguments.revisedWorksheet.name()) == null){
			return new ImportJSONLinesFileCommand(getNewId(workspace), Command.NEW_MODEL, uploadedFile);
		}
		return new ImportJSONLinesFileCommand(getNewId(workspace), request.getParameter(Arguments.revisedWorksheet.name()), uploadedFile);
	}

	@Override
	public Class<? extends Command> getCorrespondingCommand() {
		return ImportJSONLinesFileCommand.class;
	}

}
