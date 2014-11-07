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

package org.elasticsearch.alerts.actions;

import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

/**
 */
public enum AlertActionState implements ToXContent {
    SEARCH_NEEDED,
    SEARCH_UNDERWAY,
    NO_ACTION_NEEDED,
    ACTION_PERFORMED,
    ERROR;

    public static final String FIELD_NAME = "state";


    @Override
    public String toString(){
        switch (this) {
            case SEARCH_NEEDED:
                return "SEARCH_NEEDED";
            case SEARCH_UNDERWAY:
                return "SEARCH_UNDERWAY";
            case NO_ACTION_NEEDED:
                return "NO_ACTION_NEEDED";
            case ACTION_PERFORMED:
                return "ACTION_PERFORMED";
            case ERROR:
                return "ERROR";
            default:
                return "NO_ACTION_NEEDED";
        }
    }

    public static AlertActionState fromString(String s) {
        switch(s.toUpperCase()) {
            case "SEARCH_NEEDED":
                return SEARCH_NEEDED;
            case "SEARCH_UNDERWAY":
                return SEARCH_UNDERWAY;
            case "NO_ACTION_NEEDED":
                return NO_ACTION_NEEDED;
            case "ACTION_UNDERWAY":
                return ACTION_PERFORMED;
            case "ERROR":
                return ERROR;
            default:
                throw new ElasticsearchIllegalArgumentException("Unknown value [" + s + "] for AlertHistoryState" );
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(FIELD_NAME);
        builder.value(this.toString());
        builder.endObject();
        return builder;
    }
}
