/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.action.delete;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.support.replication.DocWriteRequest;

import static org.elasticsearch.action.ValidateActions.addValidationError;

/**
 * A request to delete a document from an index based on its type and id. Best created using
 * {@link org.elasticsearch.client.Requests#deleteRequest(String)}.
 * <p/>
 * <p>The operation requires the {@link #index()}, {@link #type(String)} and {@link #id(String)} to
 * be set.
 *
 * @see DeleteResponse
 * @see org.elasticsearch.client.Client#delete(DeleteRequest)
 * @see org.elasticsearch.client.Requests#deleteRequest(String)
 */
public class DeleteRequest extends DocWriteRequest<DeleteRequest> {

    public DeleteRequest() {
    }

    /**
     * Constructs a new delete request against the specified index. The {@link #type(String)} and {@link #id(String)}
     * must be set.
     */
    public DeleteRequest(String index) {
        super(index);
    }

    /**
     * Constructs a new delete request against the specified index with the type and id.
     *
     * @param index The index to get the document from
     * @param type  The type of the document
     * @param id    The id of the document
     */
    public DeleteRequest(String index, String type, String id) {
        super(index, type, id);
    }

    /**
     * Copy constructor that creates a new delete request that is a copy of the one provided as an argument.
     */
    public DeleteRequest(DeleteRequest request) {
        super(request);
    }

    /**
     * Copy constructor that creates a new delete request that is a copy of the one provided as an argument.
     * The new request will inherit though headers and context from the original request that caused it.
     */
    public DeleteRequest(DeleteRequest request, ActionRequest originalRequest) {
        super(request, originalRequest);
    }

    /**
     * Creates a delete request caused by some other request, which is provided as an
     * argument so that its headers and context can be copied to the new request
     */
    public DeleteRequest(ActionRequest request) {
        super(request);
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = super.validate();
        if (id() == null) {
            validationException = addValidationError("id is missing", validationException);
        }
        return validationException;
    }

    @Override
    protected String getRequestName() {
        return "delete";
    }
}
