/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class MessagesTest {

    @Test
    public void testMessages() throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException
    {
        String[] names = { "wibble", "wobble", "jelly" };
        Class<?> c = Class.forName("com.prelert.job.messages.Messages");
        for (Field f : c.getFields())
        {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class)
            {
                String value = (String) f.get("");
                assert(!value.equals(""));

                String msg = Messages.getMessage(value);
                Pattern p = Pattern.compile("\\{\\d[^\\}]*\\}");
                Matcher m = p.matcher(msg);

                List<Object> args = new ArrayList<>();
                int params = 0;
                while (m.find())
                {
                    if (m.group().matches(".*number.*"))
                    {
                        args.add(params);
                    }
                    else
                    {
                        args.add(names[params % names.length]);
                    }
                    params++;
                }
                msg = Messages.getMessage(value, args.toArray());
                System.out.println("Found message: " + msg);
                m = p.matcher(msg);
                assert(!m.matches());
            }
        }
    }
}
