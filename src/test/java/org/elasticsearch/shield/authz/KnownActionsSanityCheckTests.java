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

package org.elasticsearch.shield.authz;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.action.Action;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.util.Callback;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.hamcrest.Matchers.hasItem;

/**
 *
 */
public class KnownActionsSanityCheckTests extends ElasticsearchTestCase {

    private ImmutableSet<String> knownActions;

    @Before
    public void init() throws Exception {
        final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        try (InputStream input = KnownActionsSanityCheckTests.class.getResourceAsStream("actions")) {
            Streams.readAllLines(input, new Callback<String>() {
                @Override
                public void handle(String action) {
                    builder.add(action);
                }
            });
        } catch (IOException ioe) {
            throw new ElasticsearchIllegalStateException("Could not load known actions", ioe);
        }
        knownActions = builder.build();
    }

    @Test
    public void testAllShieldActionsAreKnown() throws Exception {
        ClassPath classPath = ClassPath.from(Action.class.getClassLoader());
        ImmutableSet<ClassPath.ClassInfo> infos = classPath.getTopLevelClassesRecursive(Action.class.getPackage().getName());
        for (ClassPath.ClassInfo info : infos) {
            Class clazz = info.load();
            if (Action.class.isAssignableFrom(clazz)) {
                String name = extractActionName(clazz);
                if (name != null) {
                    assertThat("elasticsearch core action [" + name + "] is unknown to shield", knownActions, hasItem(name));
                }
            }
        }
    }

    private String extractActionName(Class clazz) throws Exception {
        if (Modifier.isAbstract(clazz.getModifiers())) {
            return null;
        }
        Field field = getField(clazz, "INSTANCE");
        if (field == null || !Modifier.isStatic(field.getModifiers())) {
            return null;
        }
        Action action = (Action) field.get(null);
        return action.name();

    }

    private static Field getField(Class clazz, String name) {
        try {
            return clazz.getField(name);
        } catch (NoSuchFieldException nsfe) {
            return null;
        }
    }

}
