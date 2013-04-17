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
package org.openmhealth.reference.data.mongodb;

import org.mongojack.DBCursor;
import org.mongojack.JacksonDBCollection;
import org.openmhealth.reference.data.AuthTokenBin;
import org.openmhealth.reference.data.mongodb.domain.MongoAuthToken;
import org.openmhealth.reference.domain.AuthToken;
import org.openmhealth.reference.exception.OmhException;

import com.mongodb.BasicDBObject;
import com.mongodb.QueryBuilder;

/**
 * <p>
 * The interface to the database-backed authentication token repository.
 * </p>
 *
 * @author John Jenkins
 */
public class MongoAuthTokenBin extends AuthTokenBin {
	/**
	 * Default constructor.
	 */
	protected MongoAuthTokenBin() {
		// Do nothing.
	}

	/*
	 * (non-Javadoc)
	 * @see org.openmhealth.reference.data.AuthTokenBin#storeToken(org.openmhealth.reference.domain.AuthToken)
	 */
	@Override
	public void storeToken(final AuthToken token) throws OmhException {
		// Validate the parameter.
		if(token == null) {
			throw new OmhException("The token is null.");
		}
		
		// Get the authentication token collection.
		JacksonDBCollection<AuthToken, Object> collection =
			JacksonDBCollection
				.wrap(
					MongoDao
						.getInstance()
						.getDb()
						.getCollection(AUTH_TOKEN_BIN_DB_NAME),
					AuthToken.class);
		
		// Make sure the token doesn't already exist.
		if(collection.count(new BasicDBObject(AuthToken.JSON_KEY_TOKEN, 1)) > 0) {
			throw new OmhException("The token already exists.");
		}
		
		// Save it.
		collection.insert(token);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.openmhealth.reference.data.AuthTokenBin#getUser(java.lang.String)
	 */
	@Override
	public AuthToken getUser(final String token) throws OmhException {
		// Get the connection to the registry with the Jackson wrapper.
		JacksonDBCollection<MongoAuthToken, Object> collection =
			JacksonDBCollection
				.wrap(
					MongoDao
						.getInstance()
						.getDb()
						.getCollection(AUTH_TOKEN_BIN_DB_NAME),
					MongoAuthToken.class);
		
		// Build the query.
		QueryBuilder queryBuilder = QueryBuilder.start();
		
		// Add the authentication token to the query
		queryBuilder.and(MongoAuthToken.JSON_KEY_TOKEN).is(token);
		
		// Add the expiration timer to ensure that this token has not expired.
		queryBuilder
			.and(MongoAuthToken.JSON_KEY_EXPIRES)
			.greaterThan(System.currentTimeMillis());
		
		// Execute query.
		DBCursor<MongoAuthToken> result = collection.find(queryBuilder.get());
		
		// If multiple authentication tokens were returned, that is a violation
		// of the system.
		if(result.count() > 1) {
			throw
				new OmhException(
					"Multiple copies of the same authentication token " +
						"exist: " +
						token);
		}
		
		// If no tokens were returned, then return null.
		if(result.count() == 0) {
			return null;
		}
		else {
			return result.next();
		}
	}
}
