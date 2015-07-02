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

package org.elasticsearch.shield.transport.filter;

import com.google.common.net.InetAddresses;
import org.elasticsearch.ElasticsearchException;
import org.jboss.netty.handler.ipfilter.IpFilterRule;
import org.jboss.netty.handler.ipfilter.IpSubnetFilterRule;
import org.jboss.netty.handler.ipfilter.PatternRule;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * decorator class to have a useful toString() method for an IpFilterRule
 * as this is needed for audit logging
 */
public class ShieldIpFilterRule implements IpFilterRule {

    public static final ShieldIpFilterRule ACCEPT_ALL = new ShieldIpFilterRule(true, "accept_all") {
        @Override
        public boolean contains(InetAddress inetAddress) {
            return true;
        }

        @Override
        public boolean isAllowRule() {
            return true;
        }

        @Override
        public boolean isDenyRule() {
            return false;
        }
    };

    public static final ShieldIpFilterRule DENY_ALL = new ShieldIpFilterRule(true, "deny_all") {
        @Override
        public boolean contains(InetAddress inetAddress) {
            return true;
        }

        @Override
        public boolean isAllowRule() {
            return false;
        }

        @Override
        public boolean isDenyRule() {
            return true;
        }
    };

    private final IpFilterRule ipFilterRule;
    private final String ruleSpec;

    public ShieldIpFilterRule(boolean isAllowRule, String ruleSpec) {
        this.ipFilterRule = getRule(isAllowRule, ruleSpec);
        this.ruleSpec = ruleSpec;
    }

    @Override
    public boolean contains(InetAddress inetAddress) {
        return ipFilterRule.contains(inetAddress);
    }

    @Override
    public boolean isAllowRule() {
        return ipFilterRule.isAllowRule();
    }

    @Override
    public boolean isDenyRule() {
        return ipFilterRule.isDenyRule();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (isAllowRule()) {
            builder.append("allow ");
        } else {
            builder.append("deny ");
        }

        builder.append(ruleSpec);
        return builder.toString();
    }

    private static IpFilterRule getRule(boolean isAllowRule, String value) {
        if ("_all".equals(value)) {
            return isAllowRule ? ACCEPT_ALL : DENY_ALL;
        }

        if (value.contains("/")) {
            try {
                return new IpSubnetFilterRule(isAllowRule, value);
            } catch (UnknownHostException e) {
                throw new ElasticsearchException("unable to create shield filter for rule [" + (isAllowRule ? "allow " : "deny ") + value + "]", e);
            }
        }

        boolean isInetAddress = InetAddresses.isInetAddress(value);
        String prefix = isInetAddress ? "i:" : "n:";
        return new PatternRule(isAllowRule, prefix + value);
    }

}
