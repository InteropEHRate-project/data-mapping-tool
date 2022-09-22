/*******************************************************************************
 * Copyright 2012 University of Southern California
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This code was developed by the Information Integration Group as part
 * of the Karma project at the Information Sciences Institute of the
 * University of Southern California.  For more information, publications,
 * and related projects, please see: http://www.isi.edu/integration
 ******************************************************************************/

package edu.isi.karma.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HTTPUtil {
	private static final Logger logger = LoggerFactory.getLogger(HTTPUtil.class);

	private HTTPUtil() {
	}

	public enum HTTP_METHOD {
		GET, POST, PUT, DELETE, HEAD
	}

	public enum HTTP_HEADERS {
		Accept,
	}

	public static String executeHTTPPostRequest(String serviceURL, String contentType,
			String acceptContentType, HttpEntity entity)
					throws ClientProtocolException, IOException {
		HttpPost httpPost = new HttpPost(serviceURL);
		httpPost.setEntity(entity);
		return invokeHTTPRequest(httpPost, contentType, acceptContentType, StandardCharsets.UTF_8.name());
	}

	public static String executeHTTPPostRequest(String serviceURL, String contentType,
												String acceptContentType, String rawPostBodyData, String encoding)
		throws ClientProtocolException, IOException {
		// Prepare the headers
		HttpPost httpPost = new HttpPost(serviceURL);
		httpPost.setEntity(new StringEntity(rawPostBodyData));
		return invokeHTTPRequest(httpPost, contentType, acceptContentType, encoding);
	}

	public static String executeHTTPPostRequest(String serviceURL, String contentType,
			String acceptContentType, Map<String, String> formParameters)
					throws ClientProtocolException, IOException {

		// Prepare the message body parameters
		List<NameValuePair> formParams = new ArrayList<>();
		for (Map.Entry<String, String> stringStringEntry : formParameters.entrySet()) {
			formParams.add(new BasicNameValuePair(stringStringEntry.getKey(), stringStringEntry.getValue()));
		}

		return executeHTTPPostRequest(serviceURL, contentType, acceptContentType, new UrlEncodedFormEntity(formParams, "UTF-8"));
	}

	public static String executeHTTPPostRequest(String serviceURL, String contentType,
			String acceptContentType, String rawPostBodyData)
					throws ClientProtocolException, IOException {

		// Prepare the headers
		HttpPost httpPost = new HttpPost(serviceURL);
		httpPost.setEntity(new StringEntity(rawPostBodyData));
		return invokeHTTPRequest(httpPost, contentType, acceptContentType, StandardCharsets.UTF_8.name());
	}

	private static String invokeHTTPRequest(HttpPost httpPost, String contentType,
			String acceptContentType, String encoding) throws ClientProtocolException, IOException {
		HttpClient httpClient = new DefaultHttpClient();

		if (acceptContentType != null && !acceptContentType.isEmpty()) {
			httpPost.setHeader(HTTP_HEADERS.Accept.name(), acceptContentType);
		}
		if (contentType != null && !contentType.isEmpty()) {
			httpPost.setHeader("Content-Type", contentType);
		}

		// Execute the request
		HttpResponse response = httpClient.execute(httpPost);

		// Parse the response and store it in a String
		HttpEntity entity = response.getEntity();
		StringBuilder responseString = new StringBuilder();
		if (entity != null) {
			BufferedReader buf = new BufferedReader(new InputStreamReader(entity.getContent(), Charset.forName(encoding)));

			String line = buf.readLine();
			while(line != null) {
				responseString.append(line);
				responseString.append('\n');
				line = buf.readLine();
			}
		}
		return responseString.toString();
	}

	public static String executeHTTPGetRequest(String uri, String acceptContentType)
			throws ClientProtocolException, IOException {
		HttpClient httpClient = new DefaultHttpClient();

		HttpGet request = new HttpGet(uri);
		if(acceptContentType != null && !acceptContentType.isEmpty()) {
			request.setHeader(HTTP_HEADERS.Accept.name(), acceptContentType);
		}
		HttpResponse response = httpClient.execute(request);

		// Parse the response and store it in a String
		HttpEntity entity = response.getEntity();
		StringBuilder responseString = new StringBuilder();
		if (entity != null) {
			BufferedReader buf = new BufferedReader(new InputStreamReader(entity.getContent()));

			String line = buf.readLine();
			while(line != null) {
				responseString.append(line);
				responseString.append('\n');
				line = buf.readLine();
			}
		}
		return responseString.toString();
	}

	public static Object executeAndParseHTTPGetService(String uri, boolean includeInputAttributes) throws ClientProtocolException, IOException {
		HttpClient httpClient = new DefaultHttpClient();

		HttpGet request = new HttpGet(uri);
		request.setHeader(HTTP_HEADERS.Accept.name(), "application/json; text/xml");

		HttpResponse response = httpClient.execute(request);

		// Parse the response and store it in a String
		HttpEntity entity = response.getEntity();
		StringBuilder responseString = new StringBuilder();
		if (entity != null) {
			BufferedReader buf = new BufferedReader(new InputStreamReader(entity.getContent()));

			String line = buf.readLine();
			while(line != null) {
				responseString.append(line);
				responseString.append('\n');
				line = buf.readLine();
			}
		}

		Object output = null;
		if(entity.getContentType().toString().indexOf("xml") != -1) {
			output = XML.toJSONObject(responseString.toString());
		} else if(entity.getContentType().toString().indexOf("json") != -1){
			output = JSONUtil.createJson(responseString.toString());
		}

		if(output != null && includeInputAttributes) {
			String[] queryParams = request.getURI().getQuery().split("&");

			JSONObject nestedArr;
			if(output instanceof JSONObject)
				nestedArr = (JSONObject)output;
			else {
				nestedArr = new JSONObject();
				nestedArr.put("output", output);
			}
			for(String queryParam: queryParams) {
				String[] param = queryParam.split("=");
				String name = param[0];
				String value = param[1];
				nestedArr.put(name, value);
			}

			JSONObject result = new JSONObject();
			result.put("nestedArray", nestedArr);
			return result;
		} else {
			return output;
		}
	}

	public static String executeHTTPDeleteRequest(String uri)
			throws ClientProtocolException, IOException {
		HttpClient httpClient = new DefaultHttpClient();

		HttpDelete request = new HttpDelete(uri);
		HttpResponse response = httpClient.execute(request);

		// Parse the response and store it in a String
		HttpEntity entity = response.getEntity();
		StringBuilder responseString = new StringBuilder();
		if (entity != null) {
			BufferedReader buf = new BufferedReader(new InputStreamReader(entity.getContent()));

			String line = buf.readLine();
			while(line != null) {
				responseString.append(line);
				responseString.append('\n');
				line = buf.readLine();
			}
		}
		return responseString.toString();
	}

	/**
	 * method used to check if url for post exists.
	 * @param urlString
	 * @return
	 */
	public static int returnHTTPPOSTRequestCode(String urlString) {
		try {
			URL url = new URL(urlString);
			// We want to check the current URL
			HttpURLConnection.setFollowRedirects(false);

			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

			// We don't need to get data
			httpURLConnection.setRequestMethod("POST");

			// Some websites don't like programmatic access so pretend to be a browser
			httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.1.2) Gecko/20090729 Firefox/3.5.2 (.NET CLR 3.5.30729)");

			return httpURLConnection.getResponseCode();
		} catch (IOException e) {
			logger.error(e.getMessage());
			return 404;
		}
	}
}
