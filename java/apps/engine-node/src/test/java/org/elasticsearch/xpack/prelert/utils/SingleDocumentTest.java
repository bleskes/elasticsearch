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

package org.elasticsearch.xpack.prelert.utils;

import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;

public class SingleDocumentTest extends ESTestCase {

    public void testConstructorWithNullDocument() {
        expectThrows(NullPointerException.class, () -> new SingleDocument<>("string", null));
    }

    public void testEmptyDocument()
    {
        SingleDocument<String> doc = SingleDocument.empty("string");

        assertFalse(doc.isExists());
        assertEquals("string", doc.getType());
        assertNull(doc.getDocument());
    }

    public void testExistingDocument()
    {
        SingleDocument<String> doc = new SingleDocument<String>("string", "the doc");

        assertTrue(doc.isExists());
        assertEquals("string", doc.getType());
        assertEquals("the doc", doc.getDocument());
    }
}