/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.alerts.transport.actions.config;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ValidateActions;
import org.elasticsearch.action.support.master.MasterNodeOperationRequest;
import org.elasticsearch.alerts.ConfigurationManager;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 * A delete alert request to delete an alert by name (id)
 */
public class ConfigAlertRequest extends MasterNodeOperationRequest<ConfigAlertRequest> {

    private BytesReference configSource;
    private boolean configSourceUnsafe;


    public ConfigAlertRequest() {
    }



    /**
     * The source of the config
     * @return
     */
    public BytesReference getConfigSource() {
        return configSource;
    }

    /**
     * The source of the config document
     * @param configSource
     */
    public void setConfigSource(BytesReference configSource) {
        this.configSource = configSource;
        this.configSourceUnsafe = false;
    }

    /**
     * Is the ByteRef configSource safe
     * @return
     */
    public boolean isConfigSourceUnsafe() {
        return configSourceUnsafe;
    }

    public void setConfigSourceUnsafe(boolean configSourceUnsafe) {
        this.configSourceUnsafe = configSourceUnsafe;
    }


    /**
     * Set the source of the config with boolean to control source safety
     * @param configSource
     * @param configSourceUnsafe
     */
    public void setConfigSource(BytesReference configSource, boolean configSourceUnsafe) {
        this.configSource = configSource;
        this.configSourceUnsafe = configSourceUnsafe;
    }


    public void beforeLocalFork() {
        if (configSourceUnsafe) {
            configSource = configSource.copyBytesArray();
            configSourceUnsafe = false;
        }
    }


    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (configSource == null){
            validationException = ValidateActions.addValidationError("configName is missing", validationException);
        }
        return validationException;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        configSource = in.readBytesReference();
        configSourceUnsafe = false;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBytesReference(configSource);
    }

    @Override
    public String toString() {
        return "delete {[" + ConfigurationManager.CONFIG_INDEX + "][" + ConfigurationManager.CONFIG_TYPE + "]}";
    }
}
