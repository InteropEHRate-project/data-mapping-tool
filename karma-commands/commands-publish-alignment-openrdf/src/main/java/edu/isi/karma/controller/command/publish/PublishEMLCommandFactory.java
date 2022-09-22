package edu.isi.karma.controller.command.publish;

import edu.isi.karma.controller.command.Command;
import edu.isi.karma.controller.command.CommandFactory;
import edu.isi.karma.rep.Workspace;

import javax.servlet.http.HttpServletRequest;

/**
 * command factory to handle generating EML from Turtle RDF format.
 *
 * @author Danish
 * @date: 2019-10-01
 */
public class PublishEMLCommandFactory extends CommandFactory {

	private enum Arguments {
		worksheetId, selectionName, emlServiceURL
	}

	@Override
	public Command createCommand(HttpServletRequest request,
								 Workspace workspace) {
		String worksheetId = request.getParameter(PublishEMLCommandFactory.Arguments.worksheetId.name());
		String selectionName = request.getParameter(PublishEMLCommandFactory.Arguments.selectionName.name());
		String emlServiceURL = request.getParameter(PublishEMLCommandFactory.Arguments.emlServiceURL.name());
		PublishEMLCommand comm = new PublishEMLCommand(getNewId(workspace),
				Command.NEW_MODEL,
				worksheetId,
				selectionName, emlServiceURL
		);

		return comm;
	}

	@Override
	public Class<? extends Command> getCorrespondingCommand()
	{
		return PublishEMLCommand.class;
	}
}
