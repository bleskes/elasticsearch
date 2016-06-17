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

package org.elasticsearch.example.realm;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.xpack.security.authc.Realm;
import org.elasticsearch.xpack.security.authc.RealmConfig;

public class CustomRealmFactory extends Realm.Factory<CustomRealm> {

    @Inject
    public CustomRealmFactory(RestController controller) {
        super(CustomRealm.TYPE, false);
        controller.registerRelevantHeaders(CustomRealm.USER_HEADER, CustomRealm.PW_HEADER);
    }

    @Override
    public CustomRealm create(RealmConfig config) {
        return new CustomRealm(config);
    }

    @Override
    public CustomRealm createDefault(String name) {
        return null;
    }
}
