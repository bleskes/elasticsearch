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

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.collect.ObjectArrays;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.jackson.dataformat.yaml.snakeyaml.error.YAMLException;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.audit.AuditTrail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;

public class IPFilter extends AbstractComponent {

    /**
     * .http has been chosen for handling HTTP filters, which are not part of the profiles
     * The profiles are only handled for the transport protocol, so we need an own kind of profile
     * for HTTP. This name starts withs a dot, because no profile name can ever start like that due to
     * how we handle settings
     */
    public static final String HTTP_PROFILE_NAME = ".http";

    public static final ShieldIpFilterRule DEFAULT_PROFILE_ACCEPT_ALL = new ShieldIpFilterRule(true, "default:accept_all") {
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


    private final AuditTrail auditTrail;
    private final Map<String, ShieldIpFilterRule[]> rules;

    @Inject
    public IPFilter(Settings settings, AuditTrail auditTrail) {
        super(settings);
        this.auditTrail = auditTrail;
        rules = parseSettings(settings, logger);
    }

    public boolean accept(String profile, InetAddress peerAddress) {
        if (!rules.containsKey(profile)) {
            return true;
        }

        for (ShieldIpFilterRule rule : rules.get(profile)) {
            if (rule.contains(peerAddress)) {
                boolean isAllowed = rule.isAllowRule();
                if (isAllowed) {
                    auditTrail.connectionGranted(peerAddress, profile, rule);
                } else {
                    auditTrail.connectionDenied(peerAddress, profile, rule);
                }
                return isAllowed;
            }
        }

        auditTrail.connectionGranted(peerAddress, profile, DEFAULT_PROFILE_ACCEPT_ALL);
        return true;
    }

    private static Map<String, ShieldIpFilterRule[]> parseSettings(Settings settings, ESLogger logger) {
        if (!settings.getAsBoolean("shield.transport.filter.enabled", true)) {
            return Collections.EMPTY_MAP;
        }

        Map<String, ShieldIpFilterRule[]> profileRules = Maps.newHashMap();
        String[] allowed = settings.getAsArray("shield.transport.filter.allow");
        String[] denied = settings.getAsArray("shield.transport.filter.deny");
        String[] httpAllowed = settings.getAsArray("shield.http.filter.allow", settings.getAsArray("transport.profiles.default.shield.filter.allow", settings.getAsArray("shield.transport.filter.allow")));
        String[] httpDdenied = settings.getAsArray("shield.http.filter.deny", settings.getAsArray("transport.profiles.default.shield.filter.deny", settings.getAsArray("shield.transport.filter.deny")));

        try {
            profileRules.put("default", ObjectArrays.concat(parseValue(allowed, true), parseValue(denied, false), ShieldIpFilterRule.class));
            profileRules.put(HTTP_PROFILE_NAME, ObjectArrays.concat(parseValue(httpAllowed, true), parseValue(httpDdenied, false), ShieldIpFilterRule.class));

            Map<String, Settings> groupedSettings = settings.getGroups("transport.profiles.");
            for (Map.Entry<String, Settings> entry : groupedSettings.entrySet()) {
                String profile = entry.getKey();
                Settings profileSettings = entry.getValue().getByPrefix("shield.filter.");
                profileRules.put(profile, ObjectArrays.concat(
                        parseValue(profileSettings.getAsArray("allow"), true),
                        parseValue(profileSettings.getAsArray("deny"), false),
                        ShieldIpFilterRule.class));
            }

        } catch (IOException | YAMLException e) {
            throw new ElasticsearchParseException("Failed to read & parse rules from settings", e);
        }

        logger.debug("Loaded ip filtering profiles: {}", profileRules.keySet());
        return ImmutableMap.copyOf(profileRules);
    }

    private static ShieldIpFilterRule[] parseValue(String[] values, boolean isAllowRule) throws UnknownHostException {
        ShieldIpFilterRule[] rules = new ShieldIpFilterRule[values.length];
        for (int i = 0; i < values.length; i++) {
            rules[i] = new ShieldIpFilterRule(isAllowRule, values[i]);
        }
        return rules;
    }
}
