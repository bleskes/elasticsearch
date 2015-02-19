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

package org.elasticsearch.alerts.input.simple;

import org.elasticsearch.alerts.Payload;
import org.elasticsearch.alerts.input.Input;
import org.elasticsearch.alerts.input.InputException;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 */
public class SimpleInputTests extends ElasticsearchTestCase {

    @Test
    public void textExecute() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("foo", "bar");
        data.put("baz", new ArrayList<String>() );
        Input staticInput = new SimpleInput(logger, new Payload.Simple(data));

        Input.Result staticResult = staticInput.execute(null);
        assertEquals(staticResult.payload().data().get("foo"), "bar");
        List baz = (List)staticResult.payload().data().get("baz");
        assertTrue(baz.isEmpty());
    }


    @Test
    public void testParser_Valid() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("foo", "bar");
        data.put("baz", new ArrayList<String>() );

        XContentBuilder jsonBuilder = jsonBuilder();
        jsonBuilder.startObject();
        jsonBuilder.field(Input.Result.PAYLOAD_FIELD.getPreferredName(), data);
        jsonBuilder.endObject();

        Input.Parser parser = new SimpleInput.Parser(ImmutableSettings.builder().build());
        Input input = parser.parse(XContentFactory.xContent(jsonBuilder.bytes()).createParser(jsonBuilder.bytes()));
        assertEquals(input.type(), SimpleInput.TYPE);


        Input.Result staticResult = input.execute(null);
        assertEquals(staticResult.payload().data().get("foo"), "bar");
        List baz = (List)staticResult.payload().data().get("baz");
        assertTrue(baz.isEmpty());
    }


    @Test(expected = InputException.class)
    public void testParser_Invalid() throws Exception {

        XContentBuilder jsonBuilder = jsonBuilder();
        jsonBuilder.startObject();
        jsonBuilder.endObject();

        Input.Parser parser = new SimpleInput.Parser(ImmutableSettings.builder().build());
        parser.parse(XContentFactory.xContent(jsonBuilder.bytes()).createParser(jsonBuilder.bytes()));
        fail("[simple] input parse should fail with an InputException for an empty json object");
    }


    @Test
    public void testResultParser_Valid() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("foo", "bar");
        data.put("baz", new ArrayList<String>() );

        XContentBuilder jsonBuilder = jsonBuilder();
        jsonBuilder.startObject();
        jsonBuilder.field(Input.Result.PAYLOAD_FIELD.getPreferredName(), data);
        jsonBuilder.endObject();

        Input.Parser parser = new SimpleInput.Parser(ImmutableSettings.builder().build());
        Input.Result staticResult  = parser.parseResult(XContentFactory.xContent(jsonBuilder.bytes()).createParser(jsonBuilder.bytes()));
        assertEquals(staticResult.type(), SimpleInput.TYPE);

        assertEquals(staticResult.payload().data().get("foo"), "bar");
        List baz = (List)staticResult.payload().data().get("baz");
        assertTrue(baz.isEmpty());
    }

    @Test(expected = InputException.class)
    public void testResultParser_Invalid() throws Exception {
        XContentBuilder jsonBuilder = jsonBuilder();
        jsonBuilder.startObject();
        jsonBuilder.endObject();

        Input.Parser parser = new SimpleInput.Parser(ImmutableSettings.builder().build());
        parser.parseResult(XContentFactory.xContent(jsonBuilder.bytes()).createParser(jsonBuilder.bytes()));
        fail("[simple] input result parse should fail with an InputException for an empty json object");
    }

}
