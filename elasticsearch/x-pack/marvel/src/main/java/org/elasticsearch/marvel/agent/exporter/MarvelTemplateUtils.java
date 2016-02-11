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

package org.elasticsearch.marvel.agent.exporter;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.Streams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class MarvelTemplateUtils {

    static final String INDEX_TEMPLATE_FILE         = "/monitoring-es.json";
    static final String INDEX_TEMPLATE_NAME_PREFIX  = ".monitoring-es-";

    static final String DATA_TEMPLATE_FILE          = "/monitoring-es-data.json";
    static final String DATA_TEMPLATE_NAME_PREFIX   = ".monitoring-es-data-";

    static final String PROPERTIES_FILE             = "/monitoring.properties";
    static final String TEMPLATE_VERSION_PROPERTY   = "template.version";

    public static final Integer TEMPLATE_VERSION    = loadTemplateVersion();

    private MarvelTemplateUtils() {
    }

    /**
     * Loads the default template for the timestamped indices
     */
    public static byte[] loadTimestampedIndexTemplate() {
        try {
            return load(INDEX_TEMPLATE_FILE);
        } catch (IOException e) {
            throw new IllegalStateException("unable to load monitoring template", e);
        }
    }

    /**
     * Loads the default template for the data index
     */
    public static byte[] loadDataIndexTemplate() {
        try {
            return load(DATA_TEMPLATE_FILE);
        } catch (IOException e) {
            throw new IllegalStateException("unable to load monitoring data template", e);
        }
    }

    /**
     * Loads a resource with a given name and returns it as a byte array.
     */
    static byte[] load(String name) throws IOException {
        try (InputStream is = MarvelTemplateUtils.class.getResourceAsStream(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Streams.copy(is, out);
            return out.toByteArray();
        }
    }

    /**
     * Loads the current version of templates
     *
     * When executing tests in Intellij, the properties file might not be
     * resolved: try running 'gradle processResources' first.
     */
    static Integer loadTemplateVersion() {
        try (InputStream is = MarvelTemplateUtils.class.getResourceAsStream(PROPERTIES_FILE)) {
            Properties properties = new Properties();
            properties.load(is);
            String version = properties.getProperty(TEMPLATE_VERSION_PROPERTY);
            if (Strings.hasLength(version)) {
                return Integer.parseInt(version);
            }
            throw new IllegalArgumentException("no monitoring template version found");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("failed to parse monitoring template version");
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to load monitoring template version");
        }
    }

    public static String indexTemplateName() {
        return indexTemplateName(TEMPLATE_VERSION);
    }

    public static String indexTemplateName(Integer version) {
        return templateName(INDEX_TEMPLATE_NAME_PREFIX, version);
    }

    public static String dataTemplateName() {
        return dataTemplateName(TEMPLATE_VERSION);
    }

    public static String dataTemplateName(Integer version) {
        return templateName(DATA_TEMPLATE_NAME_PREFIX, version);
    }

    static String templateName(String prefix, Integer version) {
        assert version != null && version >= 0 : "version must be not null and greater or equal to zero";
        return prefix + String.valueOf(version);
    }
}
