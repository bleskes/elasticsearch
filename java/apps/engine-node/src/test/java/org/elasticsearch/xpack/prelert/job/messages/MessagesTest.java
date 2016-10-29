
package org.elasticsearch.xpack.prelert.job.messages;

import org.elasticsearch.test.ESTestCase;

public class MessagesTest extends ESTestCase {


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
