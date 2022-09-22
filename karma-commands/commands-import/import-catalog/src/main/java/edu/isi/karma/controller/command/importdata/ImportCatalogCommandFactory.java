package edu.isi.karma.controller.command.importdata;

import edu.isi.karma.controller.command.Command;
import edu.isi.karma.controller.command.CommandFactory;
import edu.isi.karma.rep.Workspace;

import javax.servlet.http.HttpServletRequest;

/**
 * Command factory class to handel the create command request from the frontend of the karma.
 *
 * @author Danish
 * @date 2020/06/20
 */
public class ImportCatalogCommandFactory extends CommandFactory {
	@Override
	public Command createCommand(HttpServletRequest request, Workspace workspace) {
		return new ImportCatalogCommand(getNewId(workspace),
			Command.NEW_MODEL,
			request.getParameter(Arguments.datasetId.name()),
			request.getParameter(Arguments.selectedResources.name())
		);
	}

	@Override
	public Class<? extends Command> getCorrespondingCommand() {
		return ImportCatalogCommand.class;
	}

	private enum Arguments {
		datasetId, selectedResources
	}
}
