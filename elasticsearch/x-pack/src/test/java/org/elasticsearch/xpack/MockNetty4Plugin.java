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

package org.elasticsearch.xpack;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.transport.Netty3Plugin;
import org.elasticsearch.transport.Netty4Plugin;

public final class MockNetty4Plugin extends Netty4Plugin {

    // see Netty4Plugin.... this runs without the permission from the netty4 module so it will fail since reindex can't set the property
    // to make it still work we disable that check for pseudo integ tests
    public MockNetty4Plugin(Settings settings) {
        super(Settings.builder().put(settings).put("netty.assert.buglevel", false).build());
    }

}
