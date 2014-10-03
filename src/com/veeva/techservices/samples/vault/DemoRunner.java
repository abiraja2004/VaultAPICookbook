/**
 * Author: Murugesh Naidu, Technical Architect, Veeva Technical Services
 * Oct 3, 2014
 * vaultcookbookexamples
 * DemoRunner.java
 */
package com.veeva.techservices.samples.vault;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

/**
 * @author Murugesh Naidu
 * Simple running the DemoRunner application will execute sample queries against the Vault DNS defined in constants below. Please ensure to define the following
 * constants appropriately to make this run smoothly:
 * VAULT_DNS
 * VAULT_USER_ID
 * VAULT_PASSWD
 * VAULT_VERSION
 * 
 */
public class DemoRunner {
	public static final long serialVersionUID = 1L;
	public static String VAULT_DNS = "https://vv-consulting-medcommssandbox.veevavault.com";//REPLACE THIS WITH YOUR VAULT DNS
	public static String VAULT_USER_ID = "murugesh.naidu@vv-consulting.com";//REPLACE THIS WITH YOUR USER ID
	public static String VAULT_PASSWD = "REPLACETHISWITHPLAINTEXTPASSWORD";//REPLACE THIS WITH YOUR USER PASSWORD
	public static String VAULT_VERSION = "v9.0";
	public static String VAULT_SESSION_ID;
	public static JSONObject VAULT_AUTHENTICATED_JSON;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			orchestrateDemo();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private static void orchestrateDemo() throws ClientProtocolException, IOException, JSONException {
		DemoRunner runner = new DemoRunner();
		String sessionId = runner.getVaultSessionId(VAULT_DNS, VAULT_USER_ID, VAULT_PASSWD);
		int docId = runner.createSampleDocument(sessionId, null);
		System.out.println("#######Id of created Document = " + docId);
		JSONObject response = null;
		response = runner.getAllDocumentsUsingDocEnpoint(sessionId);
		System.out.println("#######Retrieved Documents in JSON (upto 10) \n" + response);
		response = runner.getSingleDocumentUsingDocEnpoint(docId, sessionId);
		System.out.println("#######Retrieved Single Document Data in JSON\n" + response);
		response = runner.getDocTypeMetadata(sessionId);
		System.out.println("#######Retrieved Document MetaData in JSON\n" + response);
		response = runner.queryDocumentsUsingVQL("cholecap", sessionId, "all");		
		System.out.println("#######Retrieved Document Properties via vQL in JSON\n\n" + response);
	}

	/**
	 * Method returns a <code>String</code> containing the Vault Session Id
	 * 
	 * @author Murugesh Naidu
	 * @param vaultDNS
	 *            - The DNS for the Vault conforming to format as defined by
	 *            <code>DemoRunner.VAULT_DNS</code>
	 * @param userName
	 *            - Vault user name as conforming to format as defined by
	 *            <code>DemoRunner.VAULT_USER_ID</code>
	 * @param password
	 *            - Clear Text password. Sample defined in
	 *            <code>DemoRunner.VAULT_PASSWD</code>
	 * @return
	 * @throws IOException 
	 */
	private String getVaultSessionId(String vaultDNS, String userName,
			String password) throws IOException {
		String sessionId = null;
		CloseableHttpClient httpclient = HttpClients.createDefault();
		CloseableHttpResponse response = null;
		try {
			JSONObject responseJson = null;
			HttpPost post = new HttpPost(vaultDNS + "/api/auth");
			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			urlParameters.add(new BasicNameValuePair("username", userName));
			urlParameters.add(new BasicNameValuePair("password", password));
			post.setEntity(new UrlEncodedFormEntity(urlParameters));
			response = httpclient.execute(post);
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));
			String line = "";

			while ((line = bufferedReader.readLine()) != null) {
				responseJson = new JSONObject(line);
				// System.out.println("responseJson = " + responseJson);
			}
			VAULT_AUTHENTICATED_JSON = responseJson;
			String responseStatus = (String) responseJson.get("responseStatus");
			System.out.println("Authentication response: "
					+ responseJson.toString());
			if (null != responseStatus && responseStatus.equals("SUCCESS")) {
				sessionId = (String) responseJson.get("sessionId");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			response.close();
			httpclient.close();
		}
		return sessionId;
	}

	/**
	 * Creates a Sample Document in Vault with default basic required attributes. Modify this method according to your needs to add 
	 * any additional required attributes that may be needed. Used the sample to retrieve document types (Metadata API) to figure out what's 
	 * required for a Document type
	 * @author Murugesh Naidu
	 * @param sessionId
	 * @param fileToCreate - optional, if none is created, this code tries to pick an expected file in Classpath
	 * @return - Returns <code>id</code> of the recently created document
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException 
	 */
	private int createSampleDocument(String sessionId, File fileToCreate) throws ClientProtocolException, IOException, JSONException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		CloseableHttpResponse response = null;
		try {
			HttpPost httppost = new HttpPost(VAULT_DNS
					+ "/api/" + VAULT_VERSION + "/objects/documents");

			FileBody bin = new FileBody(fileToCreate!=null? fileToCreate : new File("V-CVM v1.0.pptx"));
			StringBody type = new StringBody("Project Document",
					ContentType.TEXT_PLAIN);
			StringBody product = new StringBody("00P000000000102",
					ContentType.TEXT_PLAIN);
			StringBody country = new StringBody("australia",
					ContentType.TEXT_PLAIN);
			StringBody minorVersion = new StringBody("0",
					ContentType.TEXT_PLAIN);
			StringBody majorVersion = new StringBody("1",
					ContentType.TEXT_PLAIN);
			StringBody name = new StringBody("NaiduTest-1",
					ContentType.TEXT_PLAIN);
			StringBody lifeCycle = new StringBody("General Lifecycle",
					ContentType.TEXT_PLAIN);

			HttpEntity reqEntity = MultipartEntityBuilder.create()
					.addPart("file", bin).addPart("type__v", type)
					.addPart("product__v", product)
					.addPart("country__v", country)
					.addPart("minor_version_number__v", minorVersion)
					.addPart("major_version_number__v", majorVersion)
					.addPart("lifecycle__v", lifeCycle)
					.addPart("life", majorVersion).addPart("name__v", name)
					.build();
			httppost.setEntity(reqEntity);
			httppost.addHeader("Authorization", sessionId);
			System.out
					.println("executing request " + httppost.getRequestLine());
			response = httpclient.execute(httppost);
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));
			String line = "";
			if (bufferedReader != null) {
				JSONObject responseJson = null;
				int docId = -1;
				while ((line = bufferedReader.readLine()) != null) {
					responseJson = new JSONObject(line);
				}
				System.out.println(responseJson);
				String responseStatus = (String) responseJson.get("responseStatus");					
				if (null != responseStatus && responseStatus.equals("SUCCESS")) {
					docId = (Integer) responseJson.get("id");
				}
				return docId;
			}
		} finally {
			response.close();
			httpclient.close();
		}
		return -1;//should never reach here
	}


	/**
	 * Queries Vault Documents using vQL and returns <code>JSONObject</object> containing the response. You can use vQL to query any supported object (as listed in Vault Developer Site)
	 * @author Murugesh Naidu
	 * @param searchString - any supported searchString that can be fed to to FIND element of vQL
	 * @param sessionId - Vault Session Id
	 * @param scope - properties or all
	 * @return - Response as a JSONObject
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 */
	private JSONObject queryDocumentsUsingVQL(String searchString,
			String sessionId, String scope) throws ClientProtocolException,
			IOException, JSONException {
		String query = "Select id, name__v, document_creation_date__v from documents";
		if (null != searchString && !searchString.equals("getAllDocs")) {
			query = query + " Find '" + searchString + "' scope " + scope;
		}
		query = URLEncoder.encode(query, "UTF-8");
		String apiString = "/api/" + VAULT_VERSION + "/query?q=" + query;

		return doVaultGET(VAULT_DNS, apiString, sessionId, null);
	}

	/**
	 * Helper method to perform GET Requests to Vault Enpoints
	 * @author Murugesh Naidu
	 * @param vaultDNS
	 * @param apiString
	 * @param sessionId
	 * @param headerParams
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 */
	private JSONObject doVaultGET(String vaultDNS, String apiString,
			String sessionId, Header[] headerParams)
			throws ClientProtocolException, IOException, JSONException {
		JSONObject responseJson = null;
		System.out.println("apiString = " + apiString);
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpGet getRequest = new HttpGet(vaultDNS + apiString);
		getRequest.setHeaders(headerParams);
		if (sessionId != null) {
			getRequest.addHeader("Authorization", sessionId);
		}
		org.apache.http.HttpResponse response = httpClient.execute(getRequest);
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(response.getEntity().getContent()));
		String line = "";

		while ((line = bufferedReader.readLine()) != null) {
			responseJson = new JSONObject(line);
		}
		return responseJson;
	}

	/**
	 * Performs a sample Documents retrieval operation returning upto 10 documents. Uses the "documents" endpoint to retrieve this data
	 * @author Murugesh Naidu
	 * @param vaultSessionId
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 */
	private JSONObject getAllDocumentsUsingDocEnpoint(String vaultSessionId)
			throws ClientProtocolException, IOException, JSONException {
		String apiString = "/api/" + VAULT_VERSION + "/objects/documents?limit=10";
		return doVaultGET(VAULT_DNS, apiString, vaultSessionId, null);
	}

	/**
	 * Performs a sample Document retrieval for given docId . Uses the "documents" endpoint to retrieve this data
	 * @author Murugesh Naidu
	 * @param docId
	 * @param sessionId
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 */
	private JSONObject getSingleDocumentUsingDocEnpoint(int docId,
			String sessionId) throws ClientProtocolException, IOException,
			JSONException {
		String apiString = "/api/" + VAULT_VERSION + "/objects/documents/" + docId;
		return doVaultGET(VAULT_DNS, apiString, sessionId, null);
	}

	/**
	 * Performs a sample Document Types retrieval using the Vault Documents Metadata API
	 * @author Murugesh Naidu
	 * @param sessionId
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 */
	private JSONObject getDocTypeMetadata(String sessionId)
			throws ClientProtocolException, IOException, JSONException {
		String apiString = "/api/" + VAULT_VERSION + "/metadata/objects/documents/types/";
		return doVaultGET(VAULT_DNS, apiString, sessionId, null);
	}

}
