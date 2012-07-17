package com.solitude.slots.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.solitude.slots.opensocial.Person;
import com.solitude.slots.opensocial.RestfulCollection;
import com.solitude.slots.opensocial.Score;

/**
 * Makes requests to the MocoSpace opensocial API
 * @author kwright
 */
public class OpenSocialService {
	/** logging level */
	private static final Level LOG_LEVEL = Level.INFO;
	
	/** singleton instance */
	private static final OpenSocialService instance = new OpenSocialService();
	/** @return singleton */
	public static OpenSocialService getInstance() { return instance; }	
	/** logger */
    private static final Logger log = Logger.getLogger(instance.getClass().getName());
	/** private constructor to ensure singleton */
	private OpenSocialService() { }
	
	/**
	 * Fetch access token's owner information
	 * @param accessToken from moco
	 * @param additionalFields to include in person response
	 * @return person for requesting user
	 * @throws ApiException on API error
	 * @throws IOException on connection error
	 * @throws ParseException for invalid JSON
	 */
	public Person fetchSelf(String accessToken, String... additionalFields) throws ApiException, IOException, ParseException {
		return this.fetchPerson(accessToken, -1, additionalFields);
	}
	
	/**
	 * Fetch a user's info
	 * @param accessToken from moco
	 * @param userId of user to return (-1 indicates self)
	 * @param additionalFields to include in person response
	 * @return person for requesting user
	 * @throws ApiException on API error
	 * @throws IOException on connection error
	 * @throws ParseException for invalid JSON
	 */
	public Person fetchPerson(String accessToken, int userId, String... additionalFields) throws ApiException, IOException, ParseException {
		RestfulCollection persons = this.fetchPeople(accessToken, new int[]{userId}, additionalFields);
		JSONObject entry = (JSONObject)persons.getFieldValue(RestfulCollection.Field.ENTRY.toString());
		return entry == null ? null : new Person(entry);
	}
	
	/**
	 * Fetch a users' info
	 * @param accessToken from moco
	 * @param userId of user(s) to return 
	 * @param additionalFields to include in person response
	 * @return person for requesting user
	 * @throws ApiException on API error
	 * @throws IOException on connection error
	 * @throws ParseException for invalid JSON
	 */
	public RestfulCollection fetchPeople(String accessToken, int[] userIds, String... additionalFields) throws ApiException, IOException, ParseException {
		if (userIds == null || userIds.length == 0) return new RestfulCollection();
		String url = GameUtils.getMocoSpaceOpensocialAPIEndPoint()+"/people/";
		if (userIds.length == 0 || userIds[0] <= 0) {
			url += "@me";
		} else {
			for (int i=0;i<userIds.length;i++) {
				if (i != 0) url += ",";
				url += userIds[i];
			}
		}
		url += "/@self";
		Map<String,String> params = new HashMap<String,String>();
		params.put("oauth_token", accessToken);
		if (additionalFields != null && additionalFields.length > 0) {
			String fields = null;
			for (String field : additionalFields) {
				if (fields == null) fields = field;
				else fields += ","+field;
			}
			params.put("fields",fields);
		}
		String response = doHttpRequest(url,"GET",params);
		if (log.isLoggable(LOG_LEVEL)) log.log(LOG_LEVEL,"url: "+url+", response: "+response);
		JSONObject responseObject = (JSONObject)new JSONParser().parse(response); 	// read timeout
		return new RestfulCollection(responseObject);
	}
	
	/**
	 * Fetch a user's friends
	 * 
	 * @param accessToken of requester
	 * @param index from first result to start returning
	 * @param resultsPerPage items to be returned
	 * @param additionalFields of person to inflate
	 * @return restful collection of person objects
	 * @throws ApiException on API error
	 * @throws IOException on connection error
	 * @throws ParseException for invalid JSON
	 */
	public RestfulCollection fetchFriends(String accessToken, int index, int resultsPerPage, String... additionalFields) throws ApiException, IOException, ParseException {
		String url = GameUtils.getMocoSpaceOpensocialAPIEndPoint()+"/people/@me/@friends";
		Map<String,String> params = new HashMap<String,String>();
		params.put("oauth_token", accessToken);
		params.put("count",Integer.toString(resultsPerPage));
		params.put("startIndex",Integer.toString(index));
		if (additionalFields != null && additionalFields.length > 0) {
			String fields = null;
			for (String field : additionalFields) {
				if (fields == null) fields = field;
				else fields += ","+field;
			}
			params.put("fields",fields);
		}
		String response = doHttpRequest(url,"GET",params);
		if (log.isLoggable(LOG_LEVEL)) log.log(LOG_LEVEL,"url: "+url+", response: "+response);
		JSONObject responseObject = (JSONObject)new JSONParser().parse(response); 	// read timeout
		return new RestfulCollection(responseObject);
	}
	
	/**
	 * @param userId of other user
	 * @param accessToken of requesting user
	 * @return if requesting user and other user are friends
	 * @throws ApiException on API error
	 * @throws IOException on connection error
	 * @throws ParseException for invalid JSON
	 */
	public boolean areFriends(int userId, String accessToken) throws ApiException, IOException, ParseException {
		if (userId <= 0) return false;
		String url = GameUtils.getMocoSpaceOpensocialAPIEndPoint()+"/people/@me/@areFriends";
		Map<String,String> params = new HashMap<String,String>(1);
		params.put("oauth_token",accessToken);
		String response = doHttpRequest(url,"GET",params);
		if (log.isLoggable(LOG_LEVEL)) log.log(LOG_LEVEL,"url: "+url+", response: "+response);
		JSONObject responseObject = (JSONObject)new JSONParser().parse(response); 	// read timeout
	    return (Boolean)((JSONArray)responseObject.get("entry")).get(0);
	}
	
	/**
	 * Retrieves player's moco oauth token.  Must use game admin oauth token
	 * 
	 * @param userId of user
	 * @param oauthToken of game admin
	 * @return user's oauth token
	 * @throws ApiException on API error
	 * @throws IOException on connection error
	 * @throws ParseException for invalid JSON
	 */
	public String fetchOAuthToken(int userId, String accessToken) throws ApiException, IOException, ParseException {
		String url = GameUtils.getMocoSpaceOpensocialAPIEndPoint()+"/featureOAuth/"+userId;
		Map<String,String> params = new HashMap<String,String>();
		params.put("oauth_token", accessToken);
		String response = doHttpRequest(url,"GET",params);
		if (log.isLoggable(LOG_LEVEL)) log.log(LOG_LEVEL,"url: "+url+", response: "+response);
		JSONObject responseObject = (JSONObject)new JSONParser().parse(response); 	// read timeout
		return (String)responseObject.get("entry");
	}
	
	/**
	 * Set a player's score on a leaderboard
	 * 
	 * @param type specifying the leaderboard
	 * @param userId of user whose score is set
	 * @param score to be set
	 * @param forceOverride if true then update score event if new score is lower than what is in leaderboard
	 * @throws ApiException on API error
	 * @throws IOException on connection error
	 */
	@SuppressWarnings("unchecked")
	public void setScore(short type, int userId, long score, boolean forceOverride) throws IOException, ApiException {
		String url = GameUtils.getMocoSpaceOpensocialAPIEndPoint()+"/leaderboard?oauth_token="+GameUtils.getGameAdminToken();		
		JSONObject scores = new JSONObject();		
		scores.put("userId",userId);
		scores.put(type,score);
		if (forceOverride) {
			JSONArray overrideTypes = new JSONArray();
			overrideTypes.add(type);
			scores.put("forceOverride",type);
		}
		doHttpPost(url,scores.toJSONString());		
	}
	
	/**
	 * How leaderboard users should be filtered
	 */
	public static enum LEADERBOARD_FILTER {
		/** all the players */
		ALL,
		/** only player's friends */
		FRIENDS,
		/** players in player's area */
		NEAR
	}
	
	/**
	 * Within what range of update times should be included
	 */
	public static enum LEADERBOARD_DATE_RANGE {
		/** include scores updated within the last day */
		DAY((short)1),
		/** include scores updated within the last week */
		WEEK((short)7),
		/** include all scores */
		ALL((short)-1);
		/** days value*/
		private final short days;
		/**
		 * Default constructor
		 * @param days value associated
		 */
		private LEADERBOARD_DATE_RANGE(short days) {this.days = days;}
		/** @return days associated with enum */
		public short getDays() { return this.days; }
	}

	/**
	 * Get a user's leaderboard score
	 * 
	 * @param type specifying the leaderboard
	 * @param userId of user 
	 * @return user's score on leaderboard or null if not present
	 * @throws ApiException on API error
	 * @throws IOException on connection error
	 * @throws ParseException for invalid JSON
	 */
	public Score getScore(short type, int userId) throws ApiException, IOException, ParseException {		
		RestfulCollection restfulCollection = getLeaderboard(type, userId, LEADERBOARD_DATE_RANGE.ALL, LEADERBOARD_FILTER.ALL, 0, 0);
		if (restfulCollection.getTotalResults() == 0 || restfulCollection.getEntries() == null || restfulCollection.getEntries().isEmpty()) return null;
		return new Score((JSONObject)restfulCollection.getEntries().get(0));
	}
	
	/**
	 * Get a leaderboard
	 * 
	 * @param type specifying the leaderboard
	 * @param userId of user 
	 * @param timeRange all, week, or daily
	 * @param filter all, near, or friends
	 * @param index from top of leaderboard
	 * @param itemsPerPage items per page to return
	 * @return restful collection of scores
	 * @throws ApiException on API error
	 * @throws IOException on connection error
	 * @throws ParseException for invalid JSON
	 */
	public RestfulCollection getLeaderboard(short type, int userId, LEADERBOARD_DATE_RANGE timeRange, LEADERBOARD_FILTER filter, int index, int itemsPerPage) throws ApiException, IOException, ParseException {	
		String url = GameUtils.getMocoSpaceOpensocialAPIEndPoint()+"/leaderboard/"+userId+"/"+type+"/"+filter+"/"+timeRange;
		Map<String, String> params = new HashMap<String, String>(3,1f);
		params.put("oauth_token", GameUtils.getGameAdminToken());
		params.put("count", Integer.toString(itemsPerPage));
		params.put("startIndex", Integer.toString(index));
		
		String response = doHttpRequest(url,"GET",params);
		if (log.isLoggable(LOG_LEVEL)) log.log(LOG_LEVEL,"url: "+url+", response: "+response);
		return new RestfulCollection((JSONObject)new JSONParser().parse(response));
	}
	
	/**
	 * Direct debit gold 
	 * 
	 * @param userId of user to be billed
	 * @param amount of gold to bill
	 * @param description of item being purchased with gold
	 * @return unique id for transaction
	 * @throws GoldTopupRequiredException if user does not have sufficient gold, will include redirect url for top-up
	 * @throws ApiException on API error or unexpected content
	 * @throws IOException on connection error
	 * @throws ParseException for invalid JSON
	 */
	@SuppressWarnings("unchecked")
	public String doDirectDebit(int userId, int amount, String description) throws GoldTopupRequiredException, ApiException, IOException, ParseException {
		String url = GameUtils.getMocoSpaceOpensocialAPIEndPoint()+"gold/"+userId+"?oauth_token="+GameUtils.getGameAdminToken();		
		JSONObject json = new JSONObject();
		json.put("amount", amount);
		json.put("name", description);
		json.put("secret", GameUtils.getGameGoldSecret());
		
		String response = doHttpPost(url,json.toJSONString());			
		if (log.isLoggable(LOG_LEVEL)) log.log(LOG_LEVEL,"url: "+url+", response: "+response);
		JSONObject jsonContent = (JSONObject)new JSONParser().parse(response);
		// get as string and parse to work around server bug
		JSONObject jsonPayload = (JSONObject)jsonContent.get("entry");
		String redirectUrl = (String)jsonPayload.get("redirectUrl");
		JSONArray transactionArray = (JSONArray)jsonPayload.get("GamePlatformPointsTransaction");
		if (redirectUrl != null) {
			throw new GoldTopupRequiredException(redirectUrl);
		} else if (transactionArray != null) {
			// verify transaction
			jsonPayload = (JSONObject)transactionArray.get(0);
			String token = (String)jsonPayload.get("token");
			String id = (String)jsonPayload.get("id");
			int amountReply = (Integer)jsonPayload.get("amount");
			long timestamp = (Long)jsonPayload.get("timestamp");
			if (token != null && token.equals(DigestUtils.md5Hex(id + amountReply + timestamp + GameUtils.getGameGoldSecret())) && amountReply == amount) {
				return id;
			} else {
				throw new ApiException(HttpServletResponse.SC_OK, "invalid response from debit");
			}
		} else {
			throw new ApiException(HttpServletResponse.SC_OK, "unexpected entry payload: " + response);
		}			
	}
	
	/**
	 * Sends a POST request setting body
	 * 
	 * @param url URL that may contain a port number and parameters
	 * @param body to be sent
	 * @return response content
	 * @throws IOException exception that may be thrown during connection
	 * @throws ApiException on API error
	 */
	private static String doHttpPost(String url, String body) throws IOException, ApiException {
		return doHttpRequest(url,"POST",null,body,
				Integer.parseInt(System.getProperty("url.connect.timeout","2000")),
				Integer.parseInt(System.getProperty("url.read.timeout","2000")));
	}
	
	/**
	 * Sends an HTTP request to a URL using default connect/read timeout. The URL may contain a port number and parameters
	 * 
	 * @param url URL that may contain a port number and parameters
	 * @param method HTTP method (currently supported: GET, POST, DELETE)
	 * @param params parameters to be appended to the URL or header for POST
	 * @return response content
	 * @throws IOException exception that may be thrown during connection
	 * @throws ApiException on API error
	 */
	private static String doHttpRequest(String url, String method, Map<?,?> params) throws IOException, ApiException {
		return doHttpRequest(url,method,params,null,
				Integer.parseInt(System.getProperty("url.connect.timeout","2000")),
				Integer.parseInt(System.getProperty("url.read.timeout","2000")));
	}
	
	/**
	 * Sends an HTTP request to a URL. The URL may contain a port number and parameters
	 * @param url URL that may contain a port number and parameters
	 * @param method HTTP method (currently supported: GET, POST, DELETE)
	 * @param params parameters to be appended to the URL or header for POST
	 * @param postBody if set will write direct to post.  Use INSTEAD of params
	 * @param connectTimeout timeout for estabilishing a TCP connection
	 * @param readTimeout timeout for reading input
	 * @return response content
	 * @throws IOException exception that may be thrown during connection
	 * @throws ApiException on API error
	 */
	private static String doHttpRequest(String url, String method, Map<?,?> params, String postBody, int connectTimeout, int readTimeout) throws IOException, ApiException {		
		BufferedReader in = null;
		Writer out = null;
		try {			
			if(!"post".equalsIgnoreCase(method) && params != null && !params.isEmpty()) {				
				StringBuilder s = new StringBuilder(url);				
				if(url.contains("?")) s.append('&');
				else s.append('?');
				
				boolean amp = false;
				for(Map.Entry<?, ?> entry : params.entrySet()){
					
					if(amp) s.append('&');
					s.append(entry.getKey().toString());
					s.append('=');
					s.append(entry.getValue().toString());
					
					if(!amp) amp = true;
				}
				
				url = s.toString();
				
			}			
			URL urlObj = new URL(url);
			HttpURLConnection c = (HttpURLConnection) urlObj.openConnection();
			if (connectTimeout > -1)
				c.setConnectTimeout(connectTimeout);
			if (readTimeout > -1)
				c.setReadTimeout(readTimeout);
			
			if ("post".equalsIgnoreCase(method) && params != null && !params.isEmpty()) {
				c.setDoOutput(true);
				c.setUseCaches(false);
			}			
			if (method != null) {
				c.setRequestMethod(method);
			}

			if ("post".equalsIgnoreCase(method) && ((params != null && !params.isEmpty()) || postBody != null)) {
				out = new BufferedWriter(new OutputStreamWriter(c.getOutputStream()));
				// prioritize post body over params
				if (postBody != null) {
					out.write(postBody);
				} else {
					boolean amp = false;
					for(Map.Entry<?,?> entry : params.entrySet()){
						
						if(amp) out.write('&');
						
						
						if(entry.getValue() != null){
						
							out.write(entry.getKey().toString());
							out.write('=');
							out.write(entry.getValue().toString());
							
							if(!amp) amp = true;
						
						}
					}		
				}
				out.flush();
				out.close();
			}

			StringBuilder response = new StringBuilder();			
			try {
				// Read the response
				in = new BufferedReader(new InputStreamReader(c.getInputStream()));

				String line;
				while ((line = in.readLine()) != null) {
					response.append(line);
					response.append('\n');
				}
			} catch(IOException e) {
				// we got an error, let's try to extract the actual server response
				String errResponse = "";
				try {
					in = new BufferedReader(new InputStreamReader(c.getErrorStream()));
					String line;
					while ((line = in.readLine()) != null) {
						errResponse += line;
					}
				} catch(Throwable t) {
					// we can't do anything in this case, throw original exception
					throw e;
				}
				// duplicate current exception and add the server response
				IOException e2 = new IOException(e.getMessage() + " server_response:" + errResponse);
				e2.setStackTrace(e.getStackTrace());
				throw e2;
			}

			if (c.getResponseCode() != HttpServletResponse.SC_OK) {
				throw new ApiException(c.getResponseCode(),response.toString());
			}
			return response.toString();
		} finally {
			try { out.close(); }
			catch (Exception e) {} // ignore
			try { in.close(); }
			catch (Exception e) {} // ignore
		}
	}
	
	/**
	 * Occurs when request to opensocial server returns non-500 response
	 * @author kwright
	 */
	@SuppressWarnings("serial")
	public static class ApiException extends Exception {
		/** response status code */
		private int statusCode;
		
		/**
		 * @param statusCode from response
		 * @param errorMessage from response
		 */
		public ApiException(int statusCode, String errorMessage) {
			super(errorMessage);
			this.statusCode = statusCode;
		}
		
		/** @return response status code from server */
		public int getStatusCode() { return this.statusCode; }
	}
	
	/**
	 * Indicates that a gold debit failed as the user has insufficient funds
	 * @author kwright
	 */
	@SuppressWarnings("serial")
	public static class GoldTopupRequiredException extends Exception {
		/** redirect url for top-up */
		private String redirectUrl;
		
		/** @param redirectUrl for top-up */
		public GoldTopupRequiredException(String redirectUrl) {
			this.redirectUrl = redirectUrl;
		}
		
		/** @return redirectUrl for top-up */
		public String getRedirectUrl() { return this.redirectUrl; }
	}
} 
