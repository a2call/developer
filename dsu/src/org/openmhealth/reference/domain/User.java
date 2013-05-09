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
package org.openmhealth.reference.domain;

import jbcrypt.BCrypt;

import org.openmhealth.reference.exception.OmhException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <p>
 * A user in the system.
 * </p>
 * 
 * <p>
 * This class is immutable.
 * </p>
 *
 * @author John Jenkins
 */
public class User implements OmhObject {
	/**
	 * The version of this class for serialization purposes.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The JSON key for the user's user-name.
	 */
	public static final String JSON_KEY_USERNAME = "username";
	/**
	 * The JSON key for the user's password.
	 */
	public static final String JSON_KEY_PASSWORD = "password";

	/**
	 * The user's user-name.
	 */
	@JsonProperty(JSON_KEY_USERNAME)
	private final String username;
	/**
	 * The user's password.
	 */
	@JsonIgnore
	private final String password;

	/**
	 * Creates a new user.
	 * 
	 * @param username
	 *        This user's user-name.
	 * 
	 * @param password
	 *        This user's password.
	 * 
	 * @throws OmhException
	 *         The user-name was invalid.
	 */
	@JsonCreator
	public User(
		@JsonProperty(JSON_KEY_USERNAME) final String username,
		@JsonProperty(JSON_KEY_PASSWORD) final String password)
		throws OmhException {
		
		if(username == null) {
			throw new OmhException("The username is null.");
		}
		if(username.trim().length() == 0) {
			throw new OmhException("The username is empty.");
		}
		if(password == null) {
			throw new OmhException("The password is null.");
		}
		if(password.trim().length() == 0) {
			throw new OmhException("The password is empty.");
		}
		
		this.username = validateUsername(username);
		this.password = password;
	}
	
	/**
	 * Returns the user's user-name.
	 * 
	 * @return The user's user-name.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Returns the user's hashed password.
	 * 
	 * @return The user's hashed password. 
	 */
	public String getPassword() {
		return password;
	}
	
	/**
	 * Checks if a given password matches the database's password.
	 * 
	 * @param password
	 *        The plain-text password to check.
	 * 
	 * @return True if the passwords match; false, otherwise.
	 * 
	 * @throws OmhException
	 *         The password is null.
	 */
	public boolean checkPassword(final String password) throws OmhException {
		// Validate the parameter.
		if(password == null) {
			throw new OmhException("The password is null.");
		}
		
		// Use BCrypt to check the password.
		return BCrypt.checkpw(password, this.password);
	}
	
	/**
	 * Validates that a user-name is valid.
	 * 
	 * @param username
	 *        The user-name to validate.
	 * 
	 * @return The trimmed user-name.
	 * 
	 * @throws OmhException
	 *         The user-name was invalid.
	 */
	public static String validateUsername(
		final String username)
		throws OmhException {
		
		if(username == null) {
			throw new OmhException("The username is null.");
		}
		if(username.trim().length() == 0) {
			throw new OmhException("The username is empty.");
		}
		
		return username.trim();
	}
}