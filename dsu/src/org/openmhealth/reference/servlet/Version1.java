/*******************************************************************************
 * Copyright 2013 Open mHealth
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.openmhealth.reference.servlet;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.amber.oauth2.as.request.OAuthAuthzRequest;
import org.apache.amber.oauth2.as.request.OAuthTokenRequest;
import org.apache.amber.oauth2.as.response.OAuthASResponse;
import org.apache.amber.oauth2.common.error.OAuthError.CodeResponse;
import org.apache.amber.oauth2.common.error.OAuthError.TokenResponse;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.message.OAuthResponse;
import org.apache.amber.oauth2.common.message.types.GrantType;
import org.apache.amber.oauth2.common.message.types.ResponseType;
import org.apache.amber.oauth2.common.message.types.TokenType;
import org.openmhealth.reference.data.AuthorizationCodeBin;
import org.openmhealth.reference.data.AuthorizationCodeVerificationBin;
import org.openmhealth.reference.data.AuthorizationTokenBin;
import org.openmhealth.reference.data.Registry;
import org.openmhealth.reference.data.ThirdPartyBin;
import org.openmhealth.reference.domain.AuthorizationCode;
import org.openmhealth.reference.domain.AuthorizationCodeVerification;
import org.openmhealth.reference.domain.AuthorizationToken;
import org.openmhealth.reference.domain.ThirdParty;
import org.openmhealth.reference.domain.User;
import org.openmhealth.reference.exception.InvalidAuthenticationException;
import org.openmhealth.reference.exception.OmhException;
import org.openmhealth.reference.request.AuthenticationRequest;
import org.openmhealth.reference.request.DataReadRequest;
import org.openmhealth.reference.request.DataWriteRequest;
import org.openmhealth.reference.request.ListRequest;
import org.openmhealth.reference.request.Request;
import org.openmhealth.reference.request.SchemaRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * <p>
 * The controller for the version 1 of the Open mHealth API.
 * </p>
 * 
 * <p>
 * This class has no state and, therefore, is immutable.
 * </p>
 *
 * @author John Jenkins
 */
@Controller
@RequestMapping("/v1")
public class Version1 {
	/**
	 * The username parameter for the authenticate requests.
	 */
	public static final String PARAM_AUTHENTICATION_USERNAME = "username";
	/**
	 * The password parameter for the authenticate requests.
	 */
	public static final String PARAM_AUTHENTICATION_PASSWORD = "password";
	/**
	 * The authentication token parameter for requests that require
	 * authentication.
	 */
	public static final String PARAM_AUTHENTICATION_AUTH_TOKEN = 
		"omh_auth_token";
	/**
	 * The authorization flag that indicates if the user granted the
	 * third-party access.
	 */
	public static final String PARAM_AUTHORIZATION_GRANTED = "granted";
	/**
	 * The key for the code parameter when the user is responding to a request
	 * to grant access to a third-party.
	 */
	public static final String PARAM_AUTHORIZATION_CODE = "code";

	/**
	 * The parameter for the number of records to skip in requests that use
	 * paging.
	 */
	public static final String PARAM_PAGING_NUM_TO_SKIP = "num_to_skip";
	/**
	 * The parameter for the number of records to return in requests that use
	 * paging.
	 */
	public static final String PARAM_PAGING_NUM_TO_RETURN = "num_to_return";
	
	/**
	 * The parameter for the unique identifier for a schema. This is sometimes
	 * used as part of the URI for the RESTful implementation.
	 */
	public static final String PARAM_SCHEMA_ID = "schema_id";
	/**
	 * The parameter for the version of a schema. This is sometimes used as
	 * part of the URI for the RESTful implementation.
	 */
	public static final String PARAM_SCHEMA_VERSION = "schema_version";
	
	/**
	 * A parameter that limits the results to only those that were created on
	 * or after the given date.
	 */
	public static final String PARAM_DATE_START = "t_start";
	/**
	 * A parameter that limits the results to only those that were created on
	 * or before the given date.
	 */
	public static final String PARAM_DATE_END = "t_end";
	
	/**
	 * The parameter that indicates to which user the data should pertain.
	 */
	public static final String PARAM_OWNER = "owner";
	/**
	 * The parameter that indicates that the data should be summarized, if
	 * possible.
	 */
	public static final String PARAM_SUMMARIZE = "summarize";
	/**
	 * The parameter that indicates which columns of the data should be
	 * returned.
	 */
	public static final String PARAM_COLUMN_LIST = "column_list";
	
	/**
	 * The parameter for the data when it is being uploaded.
	 */
	public static final String PARAM_DATA = "data";
	
	/**
	 * Creates an authentication request, authenticates the user and, if
	 * successful, returns the user's credentials.
	 * 
	 * @param username
	 *        The username of the user attempting to authenticate.
	 * 
	 * @param password
	 *        The password of the user attempting to authenticate.
	 * 
	 * @return A View object that will contain the user's authentication token.
	 */
	@RequestMapping(value = "auth", method = RequestMethod.POST)
	public @ResponseBody Object getAuthentication(
		@RequestParam(
			value = PARAM_AUTHENTICATION_USERNAME,
			required = true)
			final String username,
		@RequestParam(
			value = PARAM_AUTHENTICATION_PASSWORD,
			required = true)
			final String password,
		final HttpServletResponse response) {

		return
			handleRequest(
				new AuthenticationRequest(username, password), response);
	}
	
	/**
	 * <p>
	 * The OAuth call where a user has been redirected to us by some
	 * third-party in order for us to present them with an authorization
	 * request, verify that the user is who they say they are, and grant or
	 * deny the request.
	 * </p>
	 * 
	 * <p>
	 * This call will either redirect the user to the authorization HTML page
	 * with the parameters embedded or it will return a non-2xx response with a
	 * message indicating what was wrong with the request. Unfortunately,
	 * because the problem with the request may be that the given client ID is
	 * unknown, we have no way to direct the user back. If we simply force the
	 * browser to "go back", it may result in an infinite loop where the
	 * third-party continuously redirects them back to us and visa-versa. To
	 * avoid this, we should simply return an error string and let the user
	 * decide.
	 * </p>
	 * 
	 * @param request
	 *        The HTTP request.
	 * 
	 * @param response
	 *        The HTTP response.
	 * 
	 * @return An response that indicates what was wrong with the request.
	 * 
	 * @throws IOException
	 *         There was a problem responding to the client.
	 * 
	 * @throws OAuthSystemException
	 *         The OAuth library encountered an error.
	 */
	@RequestMapping(
		value = "auth/oauth/authorize",
		method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String receiveAuthorizationCodeRequest(
		final HttpServletRequest request,
		final HttpServletResponse response)
		throws IOException, OAuthSystemException {
		
		// Create the OAuth request from the HTTP request.
		OAuthAuthzRequest oauthRequest;
		try {
			oauthRequest = new OAuthAuthzRequest(request);
		}
		// The request does not conform to the RFC, so we return a HTTP 400
		// with a reason.
		catch(OAuthProblemException e) {
			// Create the OAuth response.
			OAuthResponse oauthResponse =
				OAuthASResponse
					.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
					.error(e)
					.buildJSONMessage();
			
			// Set the status and return the error message.
			response.setStatus(oauthResponse.getResponseStatus());
			return oauthResponse.getBody();
		}
		
		// Validate that the user is requesting a "code" response type, which
		// is the only response type we accept.
		try {
			if(!
				ResponseType
					.CODE.toString().equals(oauthRequest.getResponseType())) {
				
				// Create the OAuth response.
				OAuthResponse oauthResponse =
					OAuthASResponse
						.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
						.setError(CodeResponse.UNSUPPORTED_RESPONSE_TYPE)
						.setErrorDescription(
							"The response type must be '" +
								ResponseType.CODE.toString() +
								"' but was instead: " + 
								oauthRequest.getResponseType())
						.setState(oauthRequest.getState())
						.buildJSONMessage();
				
				// Set the status and return the error message.
				response.setStatus(oauthResponse.getResponseStatus());
				return oauthResponse.getBody();
			}
		}
		catch(IllegalArgumentException e) {
			// Create the OAuth response.
			OAuthResponse oauthResponse =
				OAuthASResponse
					.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
					.setError(CodeResponse.UNSUPPORTED_RESPONSE_TYPE)
					.setErrorDescription(
						"The response type is unknown: " + 
							oauthRequest.getResponseType())
					.setState(oauthRequest.getState())
					.buildJSONMessage();
			
			// Set the status and return the error message.
			response.setStatus(oauthResponse.getResponseStatus());
			return oauthResponse.getBody();
		}
		
		// Make sure no redirect URI was given.
		if(oauthRequest.getRedirectURI() != null) {
			// Create the OAuth response.
			OAuthResponse oauthResponse =
				OAuthASResponse
					.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
					.setError(CodeResponse.INVALID_REQUEST)
					.setErrorDescription(
						"A URI must not be given. Instead, the one given " +
							"when the account was created will be used.")
					.setState(oauthRequest.getState())
					.buildJSONMessage();
			
			// Set the status and return the error message.
			response.setStatus(oauthResponse.getResponseStatus());
			return oauthResponse.getBody();
		}
		
		// Attempt to get the third-party.
		ThirdParty thirdParty = 
			ThirdPartyBin
				.getInstance().getThirdParty(oauthRequest.getClientId());
		// If the third-party is unknown, reject the request.
		if(thirdParty == null) {
			// Create the OAuth response.
			OAuthResponse oauthResponse =
				OAuthASResponse
					.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
					.setError(CodeResponse.INVALID_REQUEST)
					.setErrorDescription(
						"The client ID is unknown: " + 
							oauthRequest.getClientId())
					.setState(oauthRequest.getState())
					.buildJSONMessage();
			
			// Set the status and return the error message.
			response.setStatus(oauthResponse.getResponseStatus());
			return oauthResponse.getBody();
		}
		
		// Attempt to get the scopes.
		Set<String> scopes = oauthRequest.getScopes();
		if((scopes == null) || (scopes.size() == 0)) {
			// Create the OAuth response.
			OAuthResponse oauthResponse =
				OAuthASResponse
					.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
					.setError(CodeResponse.INVALID_SCOPE)
					.setErrorDescription("A scope is required.")
					.setState(oauthRequest.getState())
					.buildJSONMessage();
			
			// Set the status and return the error message.
			response.setStatus(oauthResponse.getResponseStatus());
			return oauthResponse.getBody();
		}
		// Validate the scopes.
		Registry registry = Registry.getInstance();
		for(String scope : scopes) {
			if(registry.getSchemas(scope, null, 0, 1).size() != 1) {
				// Create the OAuth response.
				OAuthResponse oauthResponse =
					OAuthASResponse
						.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
						.setError(CodeResponse.INVALID_SCOPE)
						.setErrorDescription(
							"Each scope must be a known schema ID: " + scope)
						.setState(oauthRequest.getState())
						.buildJSONMessage();
				
				// Set the status and return the error message.
				response.setStatus(oauthResponse.getResponseStatus());
				return oauthResponse.getBody();
			}
		}
		
		// Create the temporary code to be granted or rejected by the user.
		AuthorizationCode code = 
			new AuthorizationCode(
				thirdParty, 
				oauthRequest.getScopes(), 
				oauthRequest.getState());
		
		// Store the authorization code.
		AuthorizationCodeBin.getInstance().storeCode(code);
		
		// Build the scope as specified by the OAuth specification.
		StringBuilder scopeBuilder = new StringBuilder();
		for(String scope : code.getScopes()) {
			// Add a space unless it's the first entity.
			if(scopeBuilder.length() != 0) {
				scopeBuilder.append(' ');
			}
			// Add the scope.
			scopeBuilder.append(scope);
		}
		
		// Set the redirect.
		response
			.sendRedirect(
				OAuthASResponse
					.authorizationResponse(
						request,
						HttpServletResponse.SC_FOUND)
					.setCode(code.getCode())
					.location("Authorize.html")
					.setScope(scopeBuilder.toString())
					.setParam(ThirdParty.JSON_KEY_NAME, thirdParty.getName())
					.setParam(
						ThirdParty.JSON_KEY_DESCRIPTION,
						thirdParty.getDescription())
					.buildQueryMessage()
					.getLocationUri());
		// Since we are redirecting the user, we don't need to return anything.
		return null;
	}
	
	/**
	 * <p>
	 * Handles the response from the user regarding whether or not the user
	 * granted permission to a third-party via OAuth. If the user's credentials
	 * are invalid or there was a general error reading the request, an error
	 * message will be returned and displayed to the user. Once we have the
	 * third-party's information, we will do a best-effort to redirect the user
	 * back to the third-party with a code, which the third-party can then use
	 * to call us later to determine the actual failure.
	 * </p>
	 * 
	 * @param username
	 *        The user's username.
	 * 
	 * @param password
	 *        The user's password.
	 * 
	 * @param granted
	 *        Whether or not the permission was granted.
	 * 
	 * @param code
	 *        The code that was created, but not yet validated by the user.
	 */
	@RequestMapping(
		value = "auth/oauth/authorization",
		method = RequestMethod.POST)
	public void authenticateAuthorizationCodeRequest(
		@RequestParam(
			value = PARAM_AUTHENTICATION_USERNAME,
			required = true)
			final String username,
		@RequestParam(
			value = PARAM_AUTHENTICATION_PASSWORD,
			required = true)
			final String password,
		@RequestParam(
			value = PARAM_AUTHORIZATION_GRANTED,
			required = true)
			final boolean granted,
		@RequestParam(
			value = PARAM_AUTHORIZATION_CODE,
			required = false)
			final String code,
		final HttpServletRequest request,
		final HttpServletResponse response)
		throws IOException, OAuthSystemException {
		
		// Get the user. If the user's credentials are invalid for whatever
		// reason, an exception will be thrown and the page will echo back the
		// reason.
		User user = AuthenticationRequest.getUser(username, password);
		
		// Get the authorization code.
		AuthorizationCode authCode = 
			AuthorizationCodeBin.getInstance().getCode(code);
		// If the code is unknown, we cannot redirect back to the third-party
		// because we don't know who they are.
		if(authCode == null) {
			throw new OmhException("The authorization code is unknown.");
		}
		
		// Verify that the code has not yet expired.
		if(System.currentTimeMillis() > authCode.getExpirationTime()) {
			response
				.sendRedirect(
					OAuthASResponse
						.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
						.setError(CodeResponse.ACCESS_DENIED)
						.setErrorDescription("The code has expired.")
						.location(
							authCode
								.getThirdParty().getRedirectUri().toString())
						.setState(authCode.getState())
						.buildQueryMessage()
						.getLocationUri());
			return;
		}
		
		// Get the verification if it already exists.
		AuthorizationCodeVerification verification =
			AuthorizationCodeVerificationBin
				.getInstance().getVerification(code);
		
		// If the verification does not exist, attempt to create a new one and
		// save it.
		if(verification == null) {
			// Create the new code.
			verification =
				new AuthorizationCodeVerification(authCode, user, granted);
			
			// Store it.
			AuthorizationCodeVerificationBin
				.getInstance().storeVerification(verification);
		}
		// Make sure it is being verified by the same user.
		else if(
			! user
				.getUsername().equals(verification.getOwner().getUsername())) {
			
			response
				.sendRedirect(
					OAuthASResponse
						.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
						.setError(CodeResponse.ACCESS_DENIED)
						.setErrorDescription(
							"The code has already been verified by another " +
								"user.")
						.location(
							authCode
								.getThirdParty().getRedirectUri().toString())
						.setState(authCode.getState())
						.buildQueryMessage()
						.getLocationUri());
		}
		// Make sure the same verification response is being made.
		else if(granted == verification.getGranted()) {
			response
				.sendRedirect(
					OAuthASResponse
						.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
						.setError(CodeResponse.ACCESS_DENIED)
						.setErrorDescription(
							"The user has re-submitted the same " +
								"authorization code twice with competing " +
								"grant values.")
						.location(
							authCode
								.getThirdParty().getRedirectUri().toString())
						.setState(authCode.getState())
						.buildQueryMessage()
						.getLocationUri());
		}
		// Otherwise, this is simply a repeat of the same request as before,
		// and we can simply ignore it.
		
		// Redirect the user back to the third-party with the authorization
		// code and state.
		response
			.sendRedirect(
				OAuthASResponse
					.authorizationResponse(
						request,
						HttpServletResponse.SC_OK)
					.location(
						authCode.getThirdParty().getRedirectUri().toString())
					.setCode(authCode.getCode())
					.setParam("state", authCode.getState())
					.buildQueryMessage()
					.getLocationUri());
	}
	
	/**
	 * <p>
	 * The OAuth call when a third-party is attempting to exchange their
	 * authorization request token for a valid authorization token. Because
	 * this is a back-channel communication from the third-party, their ID and
	 * secret must be given to authenticate them. They will then be returned
	 * either an authorization token or an error message indicating what was
	 * wrong with the request.
	 * </p> 
	 * 
	 * @throws OAuthSystemException The OAuth library encountered an error.
	 */
	@RequestMapping(
		value = "auth/oauth/token",
		method = RequestMethod.POST)
	public @ResponseBody Object createAuthorizationToken(
		final HttpServletRequest request,
		final HttpServletResponse response)
		throws OAuthSystemException, IOException {
		
		// Attempt to build an OAuth request from the HTTP request.
		OAuthTokenRequest oauthRequest;
		try {
			oauthRequest = new OAuthTokenRequest(request);
	    }
		// If the HTTP request was not a valid OAuth token request, then we
		// have no other choice but to reject it as a bad request.
		catch(OAuthProblemException e) {
			// Build the OAuth response.
	        OAuthResponse oauthResponse = 
	        	OAuthResponse
	            	.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
	            	.error(e)
	            	.buildJSONMessage();

	        // Set the HTTP response status code from the OAuth response.
	        response.setStatus(oauthResponse.getResponseStatus());
	        
	        // Return the error message.
	        return oauthResponse.getBody();
	    }
		
		// Attempt to get the client.
		ThirdParty thirdParty =
			ThirdPartyBin
				.getInstance().getThirdParty(oauthRequest.getClientId());
		// If the client is unknown, respond as such.
		if(thirdParty == null) {
			// Create the OAuth response.
			OAuthResponse oauthResponse =
				OAuthASResponse
					.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
					.setError(TokenResponse.INVALID_CLIENT)
					.setErrorDescription(
						"The client is unknown: " + oauthRequest.getClientId())
					.buildJSONMessage();
			
			// Set the status and return the error message.
			response.setStatus(oauthResponse.getResponseStatus());
			return oauthResponse.getBody();
		}
		
		// Get the given client secret.
		String thirdPartySecret = oauthRequest.getClientSecret();
		if(thirdPartySecret == null) {
			// Create the OAuth response.
			OAuthResponse oauthResponse =
				OAuthASResponse
					.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
					.setError(TokenResponse.INVALID_CLIENT)
					.setErrorDescription("The client secret is required.")
					.buildJSONMessage();
			
			// Set the status and return the error message.
			response.setStatus(oauthResponse.getResponseStatus());
			return oauthResponse.getBody();
		}
		// Make sure the client gave the right secret.
		else if(! thirdPartySecret.equals(thirdParty.getSecret())) {
			// Create the OAuth response.
			OAuthResponse oauthResponse =
				OAuthASResponse
					.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
					.setError(TokenResponse.INVALID_CLIENT)
					.setErrorDescription("The client secret is incorrect.")
					.buildJSONMessage();
			
			// Set the status and return the error message.
			response.setStatus(oauthResponse.getResponseStatus());
			return oauthResponse.getBody();
		}
		
		// Get the grant-type.
		GrantType grantType;
		String grantTypeString = oauthRequest.getGrantType();
		if(GrantType.AUTHORIZATION_CODE.toString().equals(grantTypeString)) {
			grantType = GrantType.AUTHORIZATION_CODE;
		}
		else if(GrantType.CLIENT_CREDENTIALS.toString().equals(grantTypeString)) {
			grantType = GrantType.CLIENT_CREDENTIALS;
		}
		else if(GrantType.PASSWORD.toString().equals(grantTypeString)) {
			grantType = GrantType.PASSWORD;
		}
		else if(GrantType.REFRESH_TOKEN.toString().equals(grantTypeString)) {
			grantType = GrantType.REFRESH_TOKEN;
		}
		else {
			// Create the OAuth response.
			OAuthResponse oauthResponse =
				OAuthASResponse
					.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
					.setError(TokenResponse.INVALID_GRANT)
					.setErrorDescription(
						"The grant type is unknown: " + grantTypeString)
					.buildJSONMessage();
			
			// Set the status and return the error message.
			response.setStatus(oauthResponse.getResponseStatus());
			return oauthResponse.getBody();
		}
		
		// Handle the different types of token requests.
		AuthorizationToken token;
		if(GrantType.AUTHORIZATION_CODE.equals(grantType)) {
			// Attempt to get the code.
			String codeString = oauthRequest.getCode();
			if(codeString == null) {
				// Create the OAuth response.
				OAuthResponse oauthResponse =
					OAuthASResponse
						.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
						.setError(TokenResponse.INVALID_REQUEST)
						.setErrorDescription(
							"An authorization code must be given to be " +
								"exchanged for an authorization token.")
						.buildJSONMessage();
				
				// Set the status and return the error message.
				response.setStatus(oauthResponse.getResponseStatus());
				return oauthResponse.getBody();
			}
			
			// Attempt to lookup the actual AuthorizationCode object.
			AuthorizationCode code =
				AuthorizationCodeBin.getInstance().getCode(codeString);
			// If the code doesn't exist, reject the request.
			if(code == null) {
				// Create the OAuth response.
				OAuthResponse oauthResponse =
					OAuthASResponse
						.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
						.setError(TokenResponse.INVALID_REQUEST)
						.setErrorDescription(
							"The given authorization code is unknown: " +
								codeString)
						.buildJSONMessage();
				
				// Set the status and return the error message.
				response.setStatus(oauthResponse.getResponseStatus());
				return oauthResponse.getBody();
			}
			
			// Verify that the client asking for a token is the same as the one
			// that requested the code.
			if(! code.getThirdParty().getId().equals(thirdParty.getId())) {
				// Create the OAuth response.
				OAuthResponse oauthResponse =
					OAuthASResponse
						.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
						.setError(TokenResponse.INVALID_REQUEST)
						.setErrorDescription(
							"This client is not allowed to reference this " +
								"code: " +
								codeString)
						.buildJSONMessage();
				
				// Set the status and return the error message.
				response.setStatus(oauthResponse.getResponseStatus());
				return oauthResponse.getBody();
			}

			// If the code has expired, reject the request.
			if(System.currentTimeMillis() > code.getExpirationTime()) {
				// Create the OAuth response.
				OAuthResponse oauthResponse =
					OAuthASResponse
						.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
						.setError(TokenResponse.INVALID_REQUEST)
						.setErrorDescription(
							"The given authorization code has expired: " +
								codeString)
						.buildJSONMessage();
				
				// Set the status and return the error message.
				response.setStatus(oauthResponse.getResponseStatus());
				return oauthResponse.getBody();
			}
			
			// Use the code to lookup the verification information and error
			// out if a user has not yet verified it.
			AuthorizationCodeVerification verification =
				AuthorizationCodeVerificationBin
					.getInstance().getVerification(code.getCode());
			if(verification == null) {
				// Create the OAuth response.
				OAuthResponse oauthResponse =
					OAuthASResponse
						.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
						.setError(TokenResponse.INVALID_REQUEST)
						.setErrorDescription(
							"A user has not yet verified the code: " +
								codeString)
						.buildJSONMessage();
				
				// Set the status and return the error message.
				response.setStatus(oauthResponse.getResponseStatus());
				return oauthResponse.getBody();
			}
			
			// Determine if the user granted access and, if not, error out.
			if(! verification.getGranted()) {
				// Create the OAuth response.
				OAuthResponse oauthResponse =
					OAuthASResponse
						.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
						.setError(TokenResponse.INVALID_REQUEST)
						.setErrorDescription(
							"The user denied the authorization: " + codeString)
						.buildJSONMessage();
				
				// Set the status and return the error message.
				response.setStatus(oauthResponse.getResponseStatus());
				return oauthResponse.getBody();
			}
			
			// Create a new token.
			token = new AuthorizationToken(verification);
		}
		// Handle a third-party refreshing an existing token.
		else if(GrantType.REFRESH_TOKEN.equals(grantType)) {
			// Get the refresh token from the request.
			String refreshToken = oauthRequest.getRefreshToken();
			if(refreshToken == null) {
				// Create the OAuth response.
				OAuthResponse oauthResponse =
					OAuthASResponse
						.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
						.setError(TokenResponse.INVALID_REQUEST)
						.setErrorDescription(
							"An refresh token must be given to be exchanged " +
								"for a new authorization token.")
						.buildJSONMessage();
				
				// Set the status and return the error message.
				response.setStatus(oauthResponse.getResponseStatus());
				return oauthResponse.getBody();
			}
			// Use the refresh token to lookup the actual refresh token.
			AuthorizationToken currentToken =
				AuthorizationTokenBin
					.getInstance().getTokenFromRefreshToken(refreshToken);
			if(currentToken == null) {
				// Create the OAuth response.
				OAuthResponse oauthResponse =
					OAuthASResponse
						.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
						.setError(TokenResponse.INVALID_REQUEST)
						.setErrorDescription("The refresh token is unknown.")
						.buildJSONMessage();
				
				// Set the status and return the error message.
				response.setStatus(oauthResponse.getResponseStatus());
				return oauthResponse.getBody();
			}
			
			// Verify that the client asking for a token is the same as the one
			// that was issued the refresh token.
			// This is probably a very serious offense and should probably
			// raise some serious red flags!
			if(!
				currentToken
					.getThirdParty().getId().equals(thirdParty.getId())) {
				
				// Create the OAuth response.
				OAuthResponse oauthResponse =
					OAuthASResponse
						.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
						.setError(TokenResponse.INVALID_REQUEST)
						.setErrorDescription(
							"This token does not belong to this client.")
						.buildJSONMessage();
				
				// Set the status and return the error message.
				response.setStatus(oauthResponse.getResponseStatus());
				return oauthResponse.getBody();
			}
			
			// Create a new authorization token from the current one.
			token = new AuthorizationToken(currentToken);
		}
		// If the grant-type is unknown, then we do not yet understand how
		// the request is built and, therefore, can do nothing more than
		// reject it via an OmhException.
		else {
			// Create the OAuth response.
			OAuthResponse oauthResponse =
				OAuthASResponse
					.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
					.setError(TokenResponse.UNSUPPORTED_GRANT_TYPE)
					.setErrorDescription(
						"The grant type must be one of '" +
							GrantType.AUTHORIZATION_CODE.toString() +
							"' or '" +
							GrantType.REFRESH_TOKEN.toString() +
							"': " +
							grantType.toString())
					.buildJSONMessage();
			
			// Set the status and return the error message.
			response.setStatus(oauthResponse.getResponseStatus());
			return oauthResponse.getBody();
		}
		
		// Store the new token.
		AuthorizationTokenBin.getInstance().storeToken(token);
		
		// Build the response.
		OAuthResponse oauthResponse =
			OAuthASResponse
				.tokenResponse(HttpServletResponse.SC_OK)
				.setAccessToken(token.getAccessToken())
				.setExpiresIn(Long.valueOf(token.getExpirationIn() / 1000).toString())
				.setRefreshToken(token.getRefreshToken())
				.setTokenType(TokenType.BEARER.toString())
				.buildJSONMessage();
		
		// Set the status.
		response.setStatus(oauthResponse.getResponseStatus());
		
		// Set the content-type.
		response.setContentType("application/json");

		// Add the headers.
		Map<String, String> headers = oauthResponse.getHeaders();
		for(String headerKey : headers.keySet()) {
			response.addHeader(headerKey, headers.get(headerKey));
		}
		
		// Return the body.
		return oauthResponse.getBody();
	}
	
	/**
	 * If the root of the hierarchy is requested, return the registry, which is
	 * a map of all of the schema IDs to their high-level information, e.g.
	 * name, description, latest version, etc.
	 * 
	 * @return All of the known schema ID-version pairs and their corresponding
	 *         schema, based on paging.
	 */
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public @ResponseBody Object getIds(
		@RequestParam(
			value = PARAM_PAGING_NUM_TO_SKIP,
			required = false,
			defaultValue = ListRequest.DEFAULT_NUMBER_TO_SKIP_STRING)
			final long numToSkip,
		@RequestParam(
			value = PARAM_PAGING_NUM_TO_RETURN,
			required = false,
			defaultValue = ListRequest.DEFAULT_NUMBER_TO_RETURN_STRING)
			final long numToReturn,
		final HttpServletResponse response) {
		
		return
			handleRequest(
				new SchemaRequest(null, null, numToSkip, numToReturn),
				response);
	}
	
	/**
	 * Creates a request to get the information about the given schema ID,
	 * e.g. the name, description, version list, etc.
	 * 
	 * @request schemaId The schema ID from the URL.
	 * 
	 * @return The schema for each version of the schema ID, based on paging.
	 */
	@RequestMapping(
		value = "{" + PARAM_SCHEMA_ID + "}",
		method = RequestMethod.GET)
	public @ResponseBody Object getVersions(
		@PathVariable(PARAM_SCHEMA_ID) final String schemaId,
		@RequestParam(
			value = PARAM_PAGING_NUM_TO_SKIP,
			required = false,
			defaultValue = ListRequest.DEFAULT_NUMBER_TO_SKIP_STRING)
			final long numToSkip,
		@RequestParam(
			value = PARAM_PAGING_NUM_TO_RETURN,
			required = false,
			defaultValue = ListRequest.DEFAULT_NUMBER_TO_RETURN_STRING)
			final long numToReturn,
		final HttpServletResponse response) {
		
		return 
			handleRequest(
				new SchemaRequest(schemaId, null, numToSkip, numToReturn),
				response);
	}
	
	/**
	 * Creates a request to get the definition of a specific schema ID's
	 * version.
	 * 
	 * @param schemaId
	 *        The schema ID from the URL.
	 * 
	 * @param version
	 *        The schema version from the URL.
	 * 
	 * @return The definition of the schema ID-version pair.
	 */
	@RequestMapping(
		value = "{" + PARAM_SCHEMA_ID + "}/{" + PARAM_SCHEMA_VERSION + "}",
		method = RequestMethod.GET)
	public @ResponseBody Object getDefinition(
		@PathVariable(PARAM_SCHEMA_ID) final String schemaId,
		@PathVariable(PARAM_SCHEMA_VERSION) final Long version,
		@RequestParam(
			value = PARAM_PAGING_NUM_TO_SKIP,
			required = false,
			defaultValue = ListRequest.DEFAULT_NUMBER_TO_SKIP_STRING)
			final long numToSkip,
		@RequestParam(
			value = PARAM_PAGING_NUM_TO_RETURN,
			required = false,
			defaultValue = ListRequest.DEFAULT_NUMBER_TO_RETURN_STRING)
			final long numToReturn,
		final HttpServletResponse response) {
		
		return 
			handleRequest(
				new SchemaRequest(schemaId, version, numToSkip, numToReturn),
				response);
	}
	
	/**
	 * Retrieves the requested data.
	 * 
	 * @param authTokenCookie
	 *        The authentication token as a cookie. The token must be provided
	 *        either here or as a parameter.
	 * 
	 * @param authTokenParameter
	 *        The authentication token as a parameter. The token must be
	 *        provided either here or as a cookie.
	 * 
	 * @param schemaId
	 *        The ID for the schema to which the data pertains. This is part of
	 *        the request's path.
	 * 
	 * @param version
	 *        The version of the schema to which the data pertains. This is
	 *        part of the request's path.
	 * 
	 * @param owner
	 *        The user that owns the desired data.
	 * 
	 * @param columnList
	 *        The list of columns to return to the user.
	 * 
	 * @param numToSkip
	 *        The number of data points to skip to facilitate paging.
	 * 
	 * @param numToReturn
	 *        The number of data points to return to facilitate paging.
	 * 
	 * @param response
	 *        The HTTP response object.
	 * 
	 * @return The data as a JSON array of JSON objects where each object
	 *         represents a single data point.
	 */
	@RequestMapping(
		value = "{" + PARAM_SCHEMA_ID + "}/{" + PARAM_SCHEMA_VERSION + "}/data",
		method = RequestMethod.GET)
	public @ResponseBody Object getData(
		@CookieValue(
			value = PARAM_AUTHENTICATION_AUTH_TOKEN,
			required = false)
			final String authTokenCookie,
		@RequestParam(
			value = PARAM_AUTHENTICATION_AUTH_TOKEN,
			required = false)
			final String authTokenParameter,
		@PathVariable(PARAM_SCHEMA_ID) final String schemaId,
		@PathVariable(PARAM_SCHEMA_VERSION) final Long version,
		@RequestParam(
			value = PARAM_OWNER,
			required = false)
			final String owner,
		@RequestParam(
			value = PARAM_COLUMN_LIST,
			required = false)
			final List<String> columnList,
		@RequestParam(
			value = PARAM_PAGING_NUM_TO_SKIP,
			required = false,
			defaultValue = ListRequest.DEFAULT_NUMBER_TO_SKIP_STRING)
			final long numToSkip,
		@RequestParam(
			value = PARAM_PAGING_NUM_TO_RETURN,
			required = false,
			defaultValue = ListRequest.DEFAULT_NUMBER_TO_RETURN_STRING)
			final long numToReturn,
		final HttpServletResponse response) {
		
		// Handle authentication.
		String authToken =
			handleAuthentication(authTokenCookie, authTokenParameter, false);

		// Handle the request.
		return 
			handleRequest(
				new DataReadRequest(
					authToken,
					schemaId,
					version,
					owner,
					columnList,
					numToSkip,
					numToReturn),
				response);
	}
	
	/**
	 * Writes the requested data.
	 * 
	 * @param authTokenParameter
	 *        The authentication token as a parameter. The token must be
	 *        provided either here or as a cookie.
	 * 
	 * @param schemaId
	 *        The ID for the schema to which the data pertains. This is part of
	 *        the request's path.
	 * 
	 * @param version
	 *        The version of the schema to which the data pertains. This is
	 *        part of the request's path.
	 *        
	 * @param data
	 *        The data to be uploaded, which should be a JSON array of JSON
	 *        objects where each object is a single data point.
	 * 
	 * @param response
	 *        The HTTP response object.
	 */
	@RequestMapping(
		value = "{" + PARAM_SCHEMA_ID + "}/{" + PARAM_SCHEMA_VERSION + "}/data",
		method = RequestMethod.POST)
	public @ResponseBody void putData(
		@RequestParam(
			value = PARAM_AUTHENTICATION_AUTH_TOKEN,
			required = true)
			final String authToken,
		@PathVariable(PARAM_SCHEMA_ID) final String schemaId,
		@PathVariable(PARAM_SCHEMA_VERSION) final Long version,
		@RequestParam(
			value = PARAM_DATA,
			required = true)
			final String data,
		final HttpServletResponse response) {
		
		// Handle the request.
		handleRequest(
			new DataWriteRequest(
				authToken,
				schemaId,
				version,
				data),
			response);
	}
	
	/**
	 * Checks the cookie and parameter authentication tokens and returns the
	 * appropriate one or null if none were given.
	 * 
	 * @param cookie
	 *        The authentication token from the HTTP cookies.
	 * 
	 * @param parameter
	 *        The authentication token from the parameters.
	 * 
	 * @param onlyParameter
	 *        A flag indicating if the authentication token may only be a
	 *        parameter. If true, there will still be a check to ensure that,
	 *        if a cookie is given, it matches the parameter, if given.
	 * 
	 * @return The most appropriate authentication token.
	 * 
	 * @throws InvalidAuthenticationException
	 *         The authentication tokens did not match, or it was only given as
	 *         a cookie but required to be a parameter.
	 */
	private String handleAuthentication(
		final String cookie,
		final String parameter,
		final boolean onlyParameter)
		throws OmhException {
		
		// If neither was given, then return null.
		if((cookie == null) && (parameter == null)) {
			return null;
		}
		// If they were both given,
		else if((cookie != null) && (parameter != null)) {
			// If they are equal, then return one.
			if(cookie.equals(parameter)) {
				return parameter;
			}
			// Otherwise, complain about them not being equal.
			else {
				throw
					new InvalidAuthenticationException(
						"The authentication token cookie was not equal to " +
							"the authentication token parameter.");
			}
		}
		// If only the cookie was given, then check to be sure it is allowed as
		// only a cookie.
		else if(cookie != null) {
			if(onlyParameter) {
				throw
					new InvalidAuthenticationException(
						"The authentication token was only given as a " +
							"cookie, but it is required to be a parameter.");
			}
			else {
				return cookie;
			}
		}
		// If they were both given, then return that.
		else {
			return parameter;
		}
	}
	
	/**
	 * Handles a request then sets the meta-data as HTTP headers and returns
	 * the data to be returned to the user.
	 * 
	 * @param request
	 *        The already-built request to be serviced.
	 * 
	 * @param response
	 *        The HTTP response to use to set the headers.
	 * 
	 * @return The object to be returned to the user.
	 */
	private Object handleRequest(
		final Request request,
		final HttpServletResponse response) {
		
		// Service the request.
		request.service();
		
		// Retrieve the meta-data and add it as HTTP headers.
		Map<String, Object> metaData = request.getMetaData();
		if(metaData != null) {
			for(String metaDataKey : metaData.keySet()) {
				response
					.setHeader(
						metaDataKey,
						metaData.get(metaDataKey).toString());
			}
		}
		
		// Return the data.
		return request.getData();
	}
}