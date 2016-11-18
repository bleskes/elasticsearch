/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2016] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.action.user;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.test.ESTestCase;

import java.util.Collections;
import java.util.Date;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class PutUserRequestTests extends ESTestCase {

    public void testValidateReturnsNullForCorrectData() throws Exception {
        final PutUserRequest request = new PutUserRequest();
        request.username("foo");
        request.roles("bar");
        request.metadata(Collections.singletonMap("created", new Date()));
        final ActionRequestValidationException validation = request.validate();
        assertThat(validation, is(nullValue()));
    }

    public void testValidateRejectsNullUserName() throws Exception {
        final PutUserRequest request = new PutUserRequest();
        request.username(null);
        request.roles("bar");
        final ActionRequestValidationException validation = request.validate();
        assertThat(validation, is(notNullValue()));
        assertThat(validation.validationErrors(), contains(is("user is missing")));
        assertThat(validation.validationErrors().size(), is(1));
    }

    public void testValidateRejectsUserNameThatStartsWithAnInvalidCharacter() throws Exception {
        final PutUserRequest request = new PutUserRequest();
        request.username("$foo");
        request.roles("bar");
        final ActionRequestValidationException validation = request.validate();
        assertThat(validation, is(notNullValue()));
        assertThat(validation.validationErrors(), contains(containsString("must begin with")));
        assertThat(validation.validationErrors().size(), is(1));
    }

    public void testValidateRejectsMetaDataWithLeadingUnderscore() throws Exception {
        final PutUserRequest request = new PutUserRequest();
        request.username("foo");
        request.roles("bar");
        request.metadata(Collections.singletonMap("_created", new Date()));
        final ActionRequestValidationException validation = request.validate();
        assertThat(validation, is(notNullValue()));
        assertThat(validation.validationErrors(), contains(containsString("metadata keys")));
        assertThat(validation.validationErrors().size(), is(1));
    }
}
