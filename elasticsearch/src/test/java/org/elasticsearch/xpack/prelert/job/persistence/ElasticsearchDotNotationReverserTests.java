/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
                "\"user\":\"10\",\"wait\":\"1\"},\"simple\":\"simon\"}";

        String actual = XContentFactory.jsonBuilder().map(reverser.getResultsMap()).string();
        assertEquals(expected, actual);
    }

    public void testMappingsMap() throws Exception {
        ElasticsearchDotNotationReverser reverser = createReverser();

        String expected = "{\"complex\":{\"properties\":{\"nested\":{\"properties\":" +
                "{\"structure\":{\"properties\":{\"first\":{\"type\":\"keyword\"}," +
                "\"second\":{\"type\":\"keyword\"}},\"type\":\"object\"}," +
                "\"value\":{\"type\":\"keyword\"}},\"type\":\"object\"}}," +
                "\"type\":\"object\"},\"cpu\":{\"properties\":{\"system\":" +
                "{\"type\":\"keyword\"},\"user\":{\"type\":\"keyword\"}," +
                "\"wait\":{\"type\":\"keyword\"}},\"type\":\"object\"}," +
                "\"simple\":{\"type\":\"keyword\"}}";

        String actual = XContentFactory.jsonBuilder().map(reverser.getMappingsMap()).string();
        assertEquals(expected, actual);
    }

    private ElasticsearchDotNotationReverser createReverser() {
        ElasticsearchDotNotationReverser reverser = new ElasticsearchDotNotationReverser();
        // This should get ignored as it's a reserved field name
        reverser.add("bucket_span", "3600");
        reverser.add("simple", "simon");
        reverser.add("cpu.user", "10");
        reverser.add("cpu.system", "5");
        reverser.add("cpu.wait", "1");
        // This should get ignored as one of its segments is a reserved field name
        reverser.add("foo.bucket_span", "3600");
        reverser.add("complex.nested.structure.first", "x");
        reverser.add("complex.nested.structure.second", "y");
        reverser.add("complex.nested.value", "z");
        return reverser;
    }
}
