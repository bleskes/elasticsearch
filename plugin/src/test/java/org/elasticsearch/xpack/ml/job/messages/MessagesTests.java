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
package org.elasticsearch.xpack.ml.job.messages;

import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.test.ESTestCase;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

@SuppressForbidden(reason = "Need to use reflection to make sure all constants are resolvable")
public class MessagesTests extends ESTestCase {

    public void testAllStringsAreInTheResourceBundle() throws IllegalArgumentException, IllegalAccessException {
        ResourceBundle bundle = Messages.load();

        // get all the public string constants
        // excluding BUNDLE_NAME
        List<Field> publicStrings = new ArrayList<>();
        Field[] allFields = Messages.class.getDeclaredFields();
        for (Field field : allFields) {
            if (Modifier.isPublic(field.getModifiers())) {
                if (field.getType() == String.class && field.getName() != "BUNDLE_NAME") {
                    publicStrings.add(field);
                }
            }
        }

        assertTrue(bundle.keySet().size() > 0);

        // Make debugging easier- print any keys not defined in Messages
        Set<String> keys = bundle.keySet();
        for (Field field : publicStrings) {
            String key = (String) field.get(new String());
            keys.remove(key);
        }

        assertEquals(bundle.keySet().size(), publicStrings.size());

        for (Field field : publicStrings) {
            String key = (String) field.get(new String());

            assertTrue("String constant " + field.getName() + " = " + key + " not in resource bundle", bundle.containsKey(key));
        }
    }

}
