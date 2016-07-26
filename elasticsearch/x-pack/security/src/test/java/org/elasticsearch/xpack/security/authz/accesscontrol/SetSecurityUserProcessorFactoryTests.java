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

package org.elasticsearch.xpack.security.authz.accesscontrol;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.security.authz.accesscontrol.SetSecurityUserProcessor.Property;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

public class SetSecurityUserProcessorFactoryTests extends ESTestCase {

    public void testProcessor() throws Exception {
        SetSecurityUserProcessor.Factory factory = new SetSecurityUserProcessor.Factory(null);
        Map<String, Object> config = new HashMap<>();
        config.put("field", "_field");
        SetSecurityUserProcessor processor = factory.create(null, "_tag", config);
        assertThat(processor.getField(), equalTo("_field"));
        assertThat(processor.getProperties(), equalTo(EnumSet.allOf(Property.class)));
    }

    public void testProcessor_noField() throws Exception {
        SetSecurityUserProcessor.Factory factory = new SetSecurityUserProcessor.Factory(null);
        Map<String, Object> config = new HashMap<>();
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class, () -> factory.create(null, "_tag", config));
        assertThat(e.getHeader("property_name").get(0), equalTo("field"));
        assertThat(e.getHeader("processor_type").get(0), equalTo(SetSecurityUserProcessor.TYPE));
        assertThat(e.getHeader("processor_tag").get(0), equalTo("_tag"));
    }

    public void testProcessor_validProperties() throws Exception {
        SetSecurityUserProcessor.Factory factory = new SetSecurityUserProcessor.Factory(null);
        Map<String, Object> config = new HashMap<>();
        config.put("field", "_field");
        config.put("properties", Arrays.asList(Property.USERNAME.name(), Property.ROLES.name()));
        SetSecurityUserProcessor processor = factory.create(null, "_tag", config);
        assertThat(processor.getField(), equalTo("_field"));
        assertThat(processor.getProperties(), equalTo(EnumSet.of(Property.USERNAME, Property.ROLES)));
    }

    public void testProcessor_invalidProperties() throws Exception {
        SetSecurityUserProcessor.Factory factory = new SetSecurityUserProcessor.Factory(null);
        Map<String, Object> config = new HashMap<>();
        config.put("field", "_field");
        config.put("properties", Arrays.asList("invalid"));
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class, () -> factory.create(null, "_tag", config));
        assertThat(e.getHeader("property_name").get(0), equalTo("properties"));
        assertThat(e.getHeader("processor_type").get(0), equalTo(SetSecurityUserProcessor.TYPE));
        assertThat(e.getHeader("processor_tag").get(0), equalTo("_tag"));
    }

}
