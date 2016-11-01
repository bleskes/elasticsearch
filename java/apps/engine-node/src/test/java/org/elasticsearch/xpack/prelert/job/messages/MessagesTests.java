/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.job.messages;

import org.elasticsearch.test.ESTestCase;

public class MessagesTests extends ESTestCase {


    // NORELEASE fix this test so it doesn't use reflect and break forbidden
    // apis
    // public void testAllStringsAreInTheResourceBundle()
    // throws IllegalArgumentException, IllegalAccessException {
    // ResourceBundle bundle = Messages.load();
    //
    // // get all the public string constants
    // // excluding BUNDLE_NAME
    // List<Field> publicStrings = new ArrayList<>();
    // Field[] allFields = Messages.class.getDeclaredFields();
    // for (Field field : allFields)
    // {
    // if (Modifier.isPublic(field.getModifiers()))
    // {
    // if (field.getType() == String.class && field.getName() != "BUNDLE_NAME")
    // {
    // publicStrings.add(field);
    // }
    // }
    // }
    //
    // assertTrue(bundle.keySet().size() > 0);
    //
    // // Make debugging easier- print any keys not defined in Messages
    // Set<String> keys = bundle.keySet();
    // for (Field field : publicStrings)
    // {
    // String key = (String)field.get(new String());
    // keys.remove(key);
    // }
    //
    // assertEquals(bundle.keySet().size(), publicStrings.size());
    //
    // for (Field field : publicStrings)
    // {
    // String key = (String)field.get(new String());
    //
    // assertTrue("String constant " + field.getName() + " = " + key + " not in
    // resource bundle",
    // bundle.containsKey(key));
    // }
    // }

}
