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

package org.elasticsearch.marvel.agent.exporter.http;

import org.elasticsearch.Version;
import org.elasticsearch.test.ESTestCase;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;


public class HttpExporterUtilsTests extends ESTestCase {

    @Test
    public void testLoadTemplate() {
        byte[] template = HttpExporterUtils.loadDefaultTemplate();
        assertNotNull(template);
        assertThat(template.length, Matchers.greaterThan(0));
    }

    @Test
    public void testParseTemplateVersionFromByteArrayTemplate() throws IOException {
        byte[] template = HttpExporterUtils.loadDefaultTemplate();
        assertNotNull(template);

        Version version = HttpExporterUtils.parseTemplateVersion(template);
        assertNotNull(version);
    }

    @Test
    public void testParseTemplateVersionFromStringTemplate() throws IOException {
        List<String> templates = new ArrayList<>();
        templates.add("{\"marvel_version\": \"1.4.0.Beta1\"}");
        templates.add("{\"marvel_version\": \"1.6.2-SNAPSHOT\"}");
        templates.add("{\"marvel_version\": \"1.7.1\"}");
        templates.add("{\"marvel_version\": \"2.0.0-beta1\"}");
        templates.add("{\"marvel_version\": \"2.0.0\"}");
        templates.add("{  \"template\": \".marvel*\",  \"settings\": {    \"marvel_version\": \"2.0.0-beta1-SNAPSHOT\", \"index.number_of_shards\": 1 } }");

        for (String template : templates) {
            Version version = HttpExporterUtils.parseTemplateVersion(template);
            assertNotNull(version);
        }

        Version version = HttpExporterUtils.parseTemplateVersion("{\"marvel.index_format\": \"7\"}");
        assertNull(version);
    }

    @Test
    public void testParseVersion() throws IOException {
        assertNotNull(HttpExporterUtils.parseVersion(HttpExporterUtils.MARVEL_VERSION_FIELD, "{\"marvel_version\": \"2.0.0-beta1\"}"));
        assertNotNull(HttpExporterUtils.parseVersion(HttpExporterUtils.MARVEL_VERSION_FIELD, "{\"marvel_version\": \"2.0.0\"}"));
        assertNotNull(HttpExporterUtils.parseVersion(HttpExporterUtils.MARVEL_VERSION_FIELD, "{\"marvel_version\": \"1.5.2\"}"));
        assertNotNull(HttpExporterUtils.parseVersion(HttpExporterUtils.MARVEL_VERSION_FIELD, "{  \"template\": \".marvel*\",  \"settings\": {    \"marvel_version\": \"2.0.0-beta1-SNAPSHOT\", \"index.number_of_shards\": 1 } }"));
        assertNull(HttpExporterUtils.parseVersion(HttpExporterUtils.MARVEL_VERSION_FIELD, "{\"marvel.index_format\": \"7\"}"));
        assertNull(HttpExporterUtils.parseVersion(HttpExporterUtils.MARVEL_VERSION_FIELD + "unkown", "{\"marvel_version\": \"1.5.2\"}"));
    }


    @Test
    public void testHostParsing() throws MalformedURLException, URISyntaxException {
        URL url = HttpExporterUtils.parseHostWithPath("localhost:9200", "");
        verifyUrl(url, "http", "localhost", 9200, "/");

        url = HttpExporterUtils.parseHostWithPath("localhost", "_bulk");
        verifyUrl(url, "http", "localhost", 9200, "/_bulk");

        url = HttpExporterUtils.parseHostWithPath("http://localhost:9200", "_bulk");
        verifyUrl(url, "http", "localhost", 9200, "/_bulk");

        url = HttpExporterUtils.parseHostWithPath("http://localhost", "_bulk");
        verifyUrl(url, "http", "localhost", 9200, "/_bulk");

        url = HttpExporterUtils.parseHostWithPath("https://localhost:9200", "_bulk");
        verifyUrl(url, "https", "localhost", 9200, "/_bulk");

        url = HttpExporterUtils.parseHostWithPath("https://boaz-air.local:9200", "_bulk");
        verifyUrl(url, "https", "boaz-air.local", 9200, "/_bulk");

        url = HttpExporterUtils.parseHostWithPath("localhost:9200/suburl", "");
        verifyUrl(url, "http", "localhost", 9200, "/suburl/");

        url = HttpExporterUtils.parseHostWithPath("localhost/suburl", "_bulk");
        verifyUrl(url, "http", "localhost", 9200, "/suburl/_bulk");

        url = HttpExporterUtils.parseHostWithPath("http://localhost:9200/suburl/suburl1", "_bulk");
        verifyUrl(url, "http", "localhost", 9200, "/suburl/suburl1/_bulk");

        url = HttpExporterUtils.parseHostWithPath("https://localhost:9200/suburl", "_bulk");
        verifyUrl(url, "https", "localhost", 9200, "/suburl/_bulk");

        url = HttpExporterUtils.parseHostWithPath("https://server_with_underscore:9300", "_bulk");
        verifyUrl(url, "https", "server_with_underscore", 9300, "/_bulk");

        url = HttpExporterUtils.parseHostWithPath("server_with_underscore:9300", "_bulk");
        verifyUrl(url, "http", "server_with_underscore", 9300, "/_bulk");

        url = HttpExporterUtils.parseHostWithPath("server_with_underscore", "_bulk");
        verifyUrl(url, "http", "server_with_underscore", 9200, "/_bulk");

        url = HttpExporterUtils.parseHostWithPath("https://server-dash:9300", "_bulk");
        verifyUrl(url, "https", "server-dash", 9300, "/_bulk");

        url = HttpExporterUtils.parseHostWithPath("server-dash:9300", "_bulk");
        verifyUrl(url, "http", "server-dash", 9300, "/_bulk");

        url = HttpExporterUtils.parseHostWithPath("server-dash", "_bulk");
        verifyUrl(url, "http", "server-dash", 9200, "/_bulk");
    }

    void verifyUrl(URL url, String protocol, String host, int port, String path) throws URISyntaxException {
        assertThat(url.getProtocol(), equalTo(protocol));
        assertThat(url.getHost(), equalTo(host));
        assertThat(url.getPort(), equalTo(port));
        assertThat(url.toURI().getPath(), equalTo(path));
    }
}
