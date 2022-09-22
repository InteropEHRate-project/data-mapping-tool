package edu.isi.karma.webserver;

import edu.isi.karma.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Servlet to handle Concept Extraction request
 *
 * @author Danish
 */
@Deprecated
public class ConceptExtractionHandler extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static Logger logger = LoggerFactory.getLogger(ConceptExtractionHandler.class);

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		logger.debug("Request URL: " + request.getRequestURI());
		logger.debug("Request Path Info: " + request.getPathInfo());
		logger.debug("Request Param: " + request.getQueryString());

		String apiURL = request.getQueryString() + "/nlp/pipelines";

		try {
			URL obj = new URL(apiURL);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");

			int responseCode = con.getResponseCode();

			if (responseCode == HttpURLConnection.HTTP_OK) { // success
				BufferedReader in = new BufferedReader(new InputStreamReader(
						con.getInputStream()));
				String inputLine;
				StringBuffer resp = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					resp.append(inputLine);
				}
				in.close();

				PrintWriter out = response.getWriter();
				response.setContentType("application/json");
				response.setCharacterEncoding("UTF-8");
				out.print(resp.toString());
				out.flush();

			} else {
				throw new IOException("ResponseCode other than 200" );
			}


		}catch (IOException e){
			logger.error("Error in retrieving list of NLP pipelines " + e);
			Util.logException(logger, e);
		}


	}
}
