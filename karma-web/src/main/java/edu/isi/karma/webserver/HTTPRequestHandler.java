package edu.isi.karma.webserver;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Servlet to handle remote http request and responses
 *
 * @author Danish
 */
public class HTTPRequestHandler extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(HTTPRequestHandler.class);

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

		logger.debug("Request URL: " + request.getRequestURI());
		logger.debug("Request Path Info: " + request.getPathInfo());
		logger.debug("Request Param: " + request.getQueryString());
		logger.debug("Request Header: " + Collections.list(request.getHeaderNames()));

		String repString = null;

		//encode all the URL's which pass through this servlet for HTTP request.
		String apiURL = null;
		try {
			apiURL = URLDecoder.decode(request.getQueryString(), StandardCharsets.UTF_8.name());
			apiURL = StringUtils.normalizeSpace(apiURL);
			apiURL = apiURL.replaceAll(" ", "%20");
		} catch (UnsupportedEncodingException e) {
			logger.error("Unsupported Encoding Exception for URL: " + request.getQueryString());
		}

		if (apiURL != null) {
			URL obj = null;
			try {
				obj = new URL(apiURL);
			} catch (MalformedURLException e) {
				logger.error("Malformed Exception URL: " + apiURL);
			}
			if (obj != null) {
				HttpURLConnection con = null;
				try {
					con = (HttpURLConnection) obj.openConnection();
				} catch (IOException e) {
					logger.error("Unable to open connection to URL: " + apiURL);
				}
				if (con != null) {
					con.setRequestMethod("GET");
					if (!Strings.isNullOrEmpty(request.getHeader("User-Agent")))
						con.setRequestProperty("User-Agent", request.getHeader("User-Agent"));
					if (!Strings.isNullOrEmpty(request.getHeader("Accept")))
						con.setRequestProperty("Accept", request.getHeader("Accept"));
					if (!Strings.isNullOrEmpty(request.getHeader("Connection")))
						con.setRequestProperty("Connection", request.getHeader("Connection"));
					if (!Strings.isNullOrEmpty(request.getHeader("Authorization")))
						con.setRequestProperty("Authorization", request.getHeader("Authorization"));
					if (!Strings.isNullOrEmpty(request.getHeader("Accept-Language")))
						con.setRequestProperty("Accept-Language", request.getHeader("Accept-Language"));

					BufferedReader in = null;
					try {
						in = new BufferedReader(new InputStreamReader(con.getInputStream()));
					} catch (IOException e) {
						logger.error("URL: " + apiURL + " " + e.getMessage());
					}
					if (in != null) {
						String inputLine;
						StringBuffer resp = new StringBuffer();

						while ((inputLine = in.readLine()) != null) {
							resp.append(inputLine);
						}
						in.close();
						repString = resp.toString();
					}
				}
			}
		}

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		if (Strings.isNullOrEmpty(repString) || Strings.isNullOrEmpty(apiURL)) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			repString = "{\"errorMessage\":\"HTTP Response is empty or null\"}";
		}

		PrintWriter out = response.getWriter();
		out.print(repString);
		out.flush();

	}
}
