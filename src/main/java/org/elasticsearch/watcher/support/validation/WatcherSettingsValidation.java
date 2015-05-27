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

package org.elasticsearch.watcher.support.validation;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.support.LoggerMessageFormat;
import org.elasticsearch.common.settings.Settings;

import java.util.*;

/**
 *
 */
public class WatcherSettingsValidation extends AbstractLifecycleComponent<WatcherSettingsValidation> {

    private List<String> errors = new ArrayList<>();

    @Inject
    public WatcherSettingsValidation(Settings settings) {
        super(settings);
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        validate();
    }

    @Override
    protected void doStop() throws ElasticsearchException {
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }

    public void addError(String setting, String reason) {
        errors.add(LoggerMessageFormat.format("", "invalid [{}] setting value [{}]. {}", setting, settings.get(setting), reason));
    }

    private void validate() throws ElasticsearchException {
        if (errors.isEmpty()) {
            return;
        }
        WatcherSettingsException exception = new WatcherSettingsException();
        for (String error : errors) {
            exception.addError(error);
        }
        throw exception;
    }
}
