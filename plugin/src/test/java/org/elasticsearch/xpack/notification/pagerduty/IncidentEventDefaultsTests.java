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
import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;

/**
 *
 */
public class IncidentEventDefaultsTests extends ESTestCase {

    public void testConstructor() throws Exception {
        Settings settings = randomSettings();
        IncidentEventDefaults defaults = new IncidentEventDefaults(settings);
        assertThat(defaults.incidentKey, is(settings.get("incident_key", null)));
        assertThat(defaults.description, is(settings.get("description", null)));
        assertThat(defaults.clientUrl, is(settings.get("client_url", null)));
        assertThat(defaults.client, is(settings.get("client", null)));
        assertThat(defaults.eventType, is(settings.get("event_type", null)));
        assertThat(defaults.attachPayload, is(settings.getAsBoolean("attach_payload", false)));
        if (settings.getAsSettings("link").names().isEmpty()) {
            IncidentEventDefaults.Context.LinkDefaults linkDefaults = new IncidentEventDefaults.Context.LinkDefaults(Settings.EMPTY);
            assertThat(defaults.link, is(linkDefaults));
        } else {
            assertThat(defaults.link, notNullValue());
            assertThat(defaults.link.href, is(settings.get("link.href", null)));
            assertThat(defaults.link.text, is(settings.get("link.text", null)));
        }
        if (settings.getAsSettings("image").names().isEmpty()) {
            IncidentEventDefaults.Context.ImageDefaults imageDefaults = new IncidentEventDefaults.Context.ImageDefaults(Settings.EMPTY);
            assertThat(defaults.image, is(imageDefaults));
        } else {
            assertThat(defaults.image, notNullValue());
            assertThat(defaults.image.href, is(settings.get("image.href", null)));
            assertThat(defaults.image.alt, is(settings.get("image.alt", null)));
            assertThat(defaults.image.src, is(settings.get("image.src", null)));
        }
    }

    public static Settings randomSettings() {
        Settings.Builder settings = Settings.builder();
        if (randomBoolean()) {
            settings.put("from", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            String[] to = new String[randomIntBetween(1, 3)];
            for (int i = 0; i < to.length; i++) {
                to[i] = randomAsciiOfLength(10);
            }
            settings.putArray("to", to);
        }
        if (randomBoolean()) {
            settings.put("text", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("event_type", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("icon", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.fallback", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.color", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.pretext", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.author_name", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.author_link", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.author_icon", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.title", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.title_link", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.text", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.image_url", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.thumb_url", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.field.title", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.field.value", randomAsciiOfLength(10));
        }
        if (randomBoolean()) {
            settings.put("attachment.field.short", randomBoolean());
        }
        return settings.build();
    }

}
