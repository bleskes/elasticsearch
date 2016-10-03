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

package org.elasticsearch.xpack.notification.pagerduty;

import org.elasticsearch.common.settings.Settings;

import java.util.Objects;

/**
 * Get trigger default configurations either from global settings or specific account settings and merge them
 */
public class IncidentEventDefaults {

    final String description;
    final String incidentKey;
    final String client;
    final String clientUrl;
    final String eventType;
    final boolean attachPayload;
    final Context.LinkDefaults link;
    final Context.ImageDefaults image;

    public IncidentEventDefaults(Settings accountSettings) {
        description = accountSettings.get(IncidentEvent.XField.DESCRIPTION.getPreferredName(), null);
        incidentKey = accountSettings.get(IncidentEvent.XField.INCIDENT_KEY.getPreferredName(), null);
        client = accountSettings.get(IncidentEvent.XField.CLIENT.getPreferredName(), null);
        clientUrl = accountSettings.get(IncidentEvent.XField.CLIENT_URL.getPreferredName(), null);
        eventType = accountSettings.get(IncidentEvent.XField.EVENT_TYPE.getPreferredName(), null);
        attachPayload = accountSettings.getAsBoolean(IncidentEvent.XField.ATTACH_PAYLOAD.getPreferredName(), false);
        link = new Context.LinkDefaults(accountSettings.getAsSettings("link"));
        image = new Context.ImageDefaults(accountSettings.getAsSettings("image"));

    }

    static class Context {

        static class LinkDefaults {

            final String href;
            final String text;

            public LinkDefaults(Settings settings) {
                href = settings.get(IncidentEventContext.XField.HREF.getPreferredName(), null);
                text = settings.get(IncidentEventContext.XField.TEXT.getPreferredName(), null);
            }

            @Override
            public int hashCode() {
                return Objects.hash(href, text);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null || getClass() != obj.getClass()){
                    return false;
                }
                final LinkDefaults other = (LinkDefaults) obj;
                return Objects.equals(href, other.href) && Objects.equals(text, other.text);
            }
        }

        static class ImageDefaults {

            final String href;
            final String src;
            final String alt;

            public ImageDefaults(Settings settings) {
                href = settings.get(IncidentEventContext.XField.HREF.getPreferredName(), null);
                src = settings.get(IncidentEventContext.XField.SRC.getPreferredName(), null);
                alt = settings.get(IncidentEventContext.XField.ALT.getPreferredName(), null);
            }

            @Override
            public int hashCode() {
                return Objects.hash(href, src, alt);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null || getClass() != obj.getClass()){
                    return false;
                }
                final ImageDefaults other = (ImageDefaults) obj;
                return Objects.equals(href, other.href) && Objects.equals(src, other.src) && Objects.equals(alt, other.alt);
            }
        }
    }


}
