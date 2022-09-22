package edu.isi.karma.web.plugins.batch.controller;

import edu.isi.karma.rdf.GenericRDFGenerator;
import edu.isi.karma.web.plugins.batch.util.ProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;

/**
 * @author Danish
 */
@Path("/helper")
public class HelperController {

	private static final Logger logger = LoggerFactory.getLogger(HelperController.class);
	@Context
	ServletContext context;
	@Context
	UriInfo uriInfo;

	@GET
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getAvailableProcessName")
	public Response getAvailableProcessName() {
		try {
			ProcessManager.initProcessManager(context,uriInfo);
			return Response.status(200).entity(
				ProcessManager.getInstance().getAvailableScheduledProcessName()
			).build();
		} catch (Exception e) {
			logger.error("Error getting available process key", e);
			return Response.serverError().build();
		}
	}


	@GET
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/content-types")
	public Response getContentType() {
		try {
			ProcessManager.initProcessManager(context,uriInfo);
			return Response.status(200).entity(
				Arrays.asList(GenericRDFGenerator.InputType.values())
			).build();
		} catch (Exception e) {
			logger.error("Error running the batch. EML generation error", e);
			return Response.serverError().build();
		}
	}

}
