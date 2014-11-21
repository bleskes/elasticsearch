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

package org.elasticsearch.shield.transport.n2n;

import org.elasticsearch.common.netty.handler.ipfilter.IpFilterRule;

import java.net.InetAddress;

/**
 * helper interface for filter rules, which takes a tcp transport profile into account
 */
public class ProfileIpFilterRule {

    private final String profile;
    private final IpFilterRule ipFilterRule;

    public ProfileIpFilterRule(String profile, IpFilterRule ipFilterRule) {
        this.profile = profile;
        this.ipFilterRule = ipFilterRule;
    }

    public boolean contains(String profile, InetAddress inetAddress) {
        return this.profile.equals(profile) && ipFilterRule.contains(inetAddress);
    }

    public boolean isAllowRule() {
        return ipFilterRule.isAllowRule();
    }

    public boolean isDenyRule() {
        return ipFilterRule.isDenyRule();
    }
}
