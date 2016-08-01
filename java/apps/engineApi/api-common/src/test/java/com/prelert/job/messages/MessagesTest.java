/****************************************************************************
 *                                                                          *
 * Copyright 2015-2016 Prelert Ltd                                          *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 *                                                                          *
 ***************************************************************************/
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
