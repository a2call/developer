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

import org.openmhealth.reference.exception.OmhException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * <p>
 * A data point as defined by the Open mHealth specification.
 * </p>
 * 
 * <p>
 * This class is immutable.
 * </p>
 * 
 * @author John Jenkins
 */
public class Data implements OmhObject {
	/**
	 * The version of this class used for serialization purposes.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The JSON key for the identifier of a user that owns this data.
	 */
	public static final String JSON_KEY_OWNER = "owner";
	
	/**
	 * The JSON key for the meta-data.
	 */
	public static final String JSON_KEY_METADATA = "metadata";
	/**
	 * The JSON key for the data.
	 */
	public static final String JSON_KEY_DATA = "data";
	
	/**
	 * The identifier for the user that owns this data.
	 */
	@JsonProperty(JSON_KEY_OWNER)
	private final String owner;

	/**
	 * The schema for this node. This is used when creating the object but may
	 * be null if deserialized.
	 */
	@JsonIgnore
	private final Schema schema;
	/**
	 * The unique identifier for the schema that validated this data. This
	 * should always exist, even if the schema is null.
	 */
	@JsonProperty(Schema.JSON_KEY_ID)
	private final String schemaId;
	/**
	 * The version of the schema that validated this data. This should always
	 * exist, even if the schema is null.
	 */
	@JsonProperty(Schema.JSON_KEY_VERSION)
	private final long schemaVersion;
	
	/**
	 * The meta-data for this point.
	 */
	@JsonProperty(JSON_KEY_METADATA)
	private final MetaData metaData;
	/**
	 * The data for this point.
	 */
	@JsonProperty(JSON_KEY_DATA)
	private final JsonNode data;

	/**
	 * Creates a new data object.
	 * 
	 * @param owner
	 * 		  The identifier for the user that owns the data.
	 * 
	 * @param schema
	 *        The schema to which this data must conform.
	 * 
	 * @param metaData
	 *        The meta-data for this data.
	 * 
	 * @param data
	 *        The data.
	 * 
	 * @throws OmhException
	 *         Any of the parameters is null.
	 */
	public Data(
		final String owner,
		final Schema schema,
		final MetaData metaData,
		final JsonNode data)
		throws OmhException {

		if(owner == null) {
			throw new OmhException("The owner is null.");
		}
		if(schema == null) {
			throw new OmhException("The schema is null.");
		}
		if(data == null) {
			throw new OmhException("The data is null.");
		}

		this.owner = owner;
		
		this.schema = schema;
		schemaId = schema.getId();
		schemaVersion = schema.getVersion();
		
		this.metaData = metaData;
		this.data = data;
	}
	
	/**
	 * Creates a new data object presumably from an existing one since all of
	 * the fields are given. If creating a new data point, it is recommended
	 * that {@link #Data(String, Schema, MetaData, JsonNode)} be used.
	 * 
	 * @param owner
	 * 		  The identifier for the user that owns the data.
	 * 
	 * @param schemaId
	 * 		  The ID of the schema that was used to validate this data.
	 * 
	 * @param schemaVersion
	 * 		  The version of the schema that was used to validate this data.
	 * 
	 * @param metaData
	 *        The meta-data for this data.
	 * 
	 * @param data
	 *        The data.
	 * 
	 * @throws OmhException
	 *         Any of the parameters is null.
	 */
	@JsonCreator
	public Data(
		@JsonProperty(JSON_KEY_OWNER) final String owner,
		@JsonProperty(Schema.JSON_KEY_ID) final String schemaId,
		@JsonProperty(Schema.JSON_KEY_VERSION) final long schemaVersion,
		@JsonProperty(JSON_KEY_METADATA) final MetaData metaData,
		@JsonProperty(JSON_KEY_DATA) final JsonNode data)
		throws OmhException {
		
		if(owner == null) {
			throw new OmhException("The owner is null.");
		}
		if(schemaId == null) {
			throw new OmhException("The schema ID is null.");
		}
		if(data == null) {
			throw new OmhException("The data is null.");
		}
		
		this.owner = owner;
		this.schema = null;
		this.schemaId = schemaId;
		this.schemaVersion = schemaVersion;
		this.metaData = metaData;
		this.data = data;
	}
	
	/**
	 * Returns the username of the owner of this data point.
	 * 
	 * @return The username of the owner of this data point.
	 */
	public String getOwner() {
		return owner;
	}
	
	/**
	 * Returns the ID of the schema to which this point is associated.
	 * 
	 * @return The ID of the schema to which this point is associated.
	 */
	public String getSchemaId() {
		return schemaId;
	}

	/**
	 * Returns the version of the schema to which this point is associated.
	 * 
	 * @return The version of the schema to which this point is associated.
	 */
	public long getSchemaVersion() {
		return schemaVersion;
	}

	/**
	 * Returns the meta-data associated with this point.
	 * 
	 * @return The meta-data associated with this point or null if there is no
	 *         meta-data.
	 */
	public MetaData getMetaData() {
		return metaData;
	}
	
	/**
	 * Returns the data associated with this point.
	 * 
	 * @return The data associated with this point.
	 */
	public JsonNode getData() {
		return data;
	}
}