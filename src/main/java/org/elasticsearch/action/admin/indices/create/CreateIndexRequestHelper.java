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

package org.elasticsearch.action.admin.indices.create;

import org.elasticsearch.action.admin.indices.alias.Alias;

import java.util.Set;

/*
 * Helper needed to retrieve aliases from a CreateIndexRequest, as the corresponding getter has package private visibility
 * TODO Remove this class as soon as es core 1.5.0 is out
 */
public final class CreateIndexRequestHelper {

    private CreateIndexRequestHelper() {
    }

    public static Set<Alias> aliases(CreateIndexRequest createIndexRequest) {
        return createIndexRequest.aliases();
    }
}
