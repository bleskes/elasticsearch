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

package org.elasticsearch.xpack.security.transport.filter;

import org.elasticsearch.test.ESTestCase;
import org.jboss.netty.handler.ipfilter.IpFilterRule;
import org.jboss.netty.handler.ipfilter.IpSubnetFilterRule;
import org.jboss.netty.handler.ipfilter.PatternRule;

import static org.elasticsearch.xpack.security.transport.filter.SecurityIpFilterRule.ACCEPT_ALL;
import static org.elasticsearch.xpack.security.transport.filter.SecurityIpFilterRule.DENY_ALL;
import static org.elasticsearch.xpack.security.transport.filter.SecurityIpFilterRule.getRule;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Unit tests for the {@link SecurityIpFilterRule}
 */
public class SecurityIpFilterRuleTests extends ESTestCase {
    public void testParseAllRules() {
        IpFilterRule rule = getRule(true, "_all");
        assertThat(rule, sameInstance(ACCEPT_ALL));

        rule = getRule(false, "_all");
        assertThat(rule, sameInstance(DENY_ALL));
    }

    public void testParseAllRuleWithOtherValues() {
        String ruleValue = "_all," + randomFrom("name", "127.0.0.1", "127.0.0.0/24");
        try {
            getRule(randomBoolean(), ruleValue);
            fail("an illegal argument exception should have been thrown!");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testParseIpSubnetFilterRule() throws Exception {
        final boolean allow = randomBoolean();
        IpFilterRule rule = getRule(allow, "127.0.0.0/24");
        assertThat(rule, instanceOf(IpSubnetFilterRule.class));
        assertThat(rule.isAllowRule(), equalTo(allow));
        IpSubnetFilterRule ipSubnetFilterRule = (IpSubnetFilterRule) rule;
        assertThat(ipSubnetFilterRule.contains("127.0.0.1"), equalTo(true));
    }

    public void testParseIpSubnetFilterRuleWithOtherValues() throws Exception {
        try {
            getRule(randomBoolean(), "127.0.0.0/24," + randomFrom("name", "127.0.0.1", "192.0.0.0/24"));
            fail("expected an exception to be thrown because only one subnet can be specified at a time");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    public void testParsePatternRules() {
        final boolean allow = randomBoolean();
        String ruleSpec = "127.0.0.1,::1,192.168.0.*,name*,specific_name";
        IpFilterRule rule = getRule(allow, ruleSpec);
        assertThat(rule, instanceOf(PatternRule.class));
        assertThat(rule.isAllowRule(), equalTo(allow));
    }
}
