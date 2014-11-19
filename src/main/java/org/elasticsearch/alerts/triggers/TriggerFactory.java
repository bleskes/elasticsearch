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

package org.elasticsearch.alerts.triggers;


import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Map;

public interface TriggerFactory {

    /**
     * Creates a trigger form the given parser
     * @param parser The parser containing the definition of the trigger
     * @return The newly created trigger
     * @throws IOException
     */
    AlertTrigger createTrigger(XContentParser parser) throws IOException;

    /**
     * Evaulates if the trigger is triggers based off of the request and response
     *
     * @param trigger
     * @param request
     * @param response
     * @return
     */
    boolean isTriggered(AlertTrigger trigger, SearchRequest request, Map<String, Object> response);

}
