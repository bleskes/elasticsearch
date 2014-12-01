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

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.jackson.dataformat.yaml.snakeyaml.error.YAMLException;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.net.InetAddresses;
import org.elasticsearch.common.netty.handler.ipfilter.IpFilterRule;
import org.elasticsearch.common.netty.handler.ipfilter.IpSubnetFilterRule;
import org.elasticsearch.common.netty.handler.ipfilter.PatternRule;
import org.elasticsearch.common.settings.Settings;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.*;

public class IPFilteringN2NAuthenticator extends AbstractComponent implements N2NAuthenticator {

    private static final ProfileIpFilterRule[] NO_RULES = new ProfileIpFilterRule[0];
    private volatile ProfileIpFilterRule[] rules = NO_RULES;

    @Inject
    public IPFilteringN2NAuthenticator(Settings settings) {
        super(settings);
        rules = parseSettings(settings, logger);
    }

    @Override
    public boolean authenticate(@Nullable Principal peerPrincipal, String profile, InetAddress peerAddress, int peerPort) {
        if (rules == NO_RULES) {
            return true;
        }
        for (ProfileIpFilterRule rule : rules) {
            if (rule.contains(profile, peerAddress)) {
                boolean isAllowed = rule.isAllowRule();
                logger.trace("Authentication rule matched for host [{}]: {}", peerAddress, isAllowed);
                return isAllowed;
            }
        }

        logger.trace("Allowing host {}", peerAddress);
        return true;
    }

    private static ProfileIpFilterRule[] parseSettings(Settings settings, ESLogger logger) {
        if (!settings.getAsBoolean("shield.transport.filter.enabled", true)) {
            return NO_RULES;
        }
        String[] allowed = settings.getAsArray("shield.transport.filter.allow");
        String[] denied = settings.getAsArray("shield.transport.filter.deny");
        List<ProfileIpFilterRule> rules = new ArrayList<>();

        try {
            rules.addAll(parseValue(allowed, "default", true));
            rules.addAll(parseValue(denied, "default", false));

            Map<String, Settings> groupedSettings = settings.getGroups("transport.profiles.");
            for (Map.Entry<String, Settings> entry : groupedSettings.entrySet()) {
                String profile = entry.getKey();
                Settings profileSettings = entry.getValue().getByPrefix("shield.filter.");
                rules.addAll(parseValue(profileSettings.getAsArray("allow"), profile, true));
                rules.addAll(parseValue(profileSettings.getAsArray("deny"), profile, false));
            }

        } catch (IOException | YAMLException e) {
            throw new ElasticsearchParseException("Failed to read & parse rules from settings", e);
        }

        logger.debug("Loaded {} ip filtering rules", rules.size());
        return rules.toArray(new ProfileIpFilterRule[rules.size()]);
    }

    private static Collection<? extends ProfileIpFilterRule> parseValue(String[] values, String profile, boolean isAllowRule) throws UnknownHostException {
        List<ProfileIpFilterRule> rules = new ArrayList<>();
        for (String value : values) {
            rules.add(new ProfileIpFilterRule(profile, getRule(isAllowRule, value)));
        }
        return rules;
    }

    private static IpFilterRule getRule(boolean isAllowRule, String value) throws UnknownHostException {
        if ("_all".equals(value)) {
            return new PatternRule(isAllowRule, "n:*");
        } else if (value.contains("/")) {
            return new IpSubnetFilterRule(isAllowRule, value);
        }

        boolean isInetAddress = InetAddresses.isInetAddress(value);
        String prefix = isInetAddress ? "i:" : "n:";
        return new PatternRule(isAllowRule, prefix + value);
    }

}
