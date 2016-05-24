/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import org.junit.Test;

public class MessagesTest {

    @Test
    public void testAllStringsAreInTheResourceBundle()
    throws IllegalArgumentException, IllegalAccessException
    {
        ResourceBundle bundle = Messages.load();

        // get all the public string constants
        // excluding BUNDLE_NAME
        List<Field> publicStrings = new ArrayList<>();
        Field[] allFields = Messages.class.getDeclaredFields();
        for (Field field : allFields)
        {
            if (Modifier.isPublic(field.getModifiers()))
            {
                if (field.getType() == String.class && field.getName() != "BUNDLE_NAME")
                {
                    publicStrings.add(field);
                }
            }
        }

        assertTrue(bundle.keySet().size() > 0);

        // Make debugging easier- print any keys not defined in Messages
        Set<String> keys = bundle.keySet();
        for (Field field : publicStrings)
        {
            String key = (String)field.get(new String());
            keys.remove(key);
        }
        for (String key : keys)
        {
            System.out.println(key);
        }

        assertEquals(bundle.keySet().size(), publicStrings.size());

        for (Field field : publicStrings)
        {
            String key = (String)field.get(new String());

            assertTrue("String constant " + field.getName() + " = " + key + " not in resource bundle",
                        bundle.containsKey(key));
        }
    }

}
