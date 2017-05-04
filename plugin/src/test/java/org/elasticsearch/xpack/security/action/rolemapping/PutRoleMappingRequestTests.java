/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.action.rolemapping;

import java.util.Collections;

import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.common.ValidationException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.security.authc.support.mapper.expressiondsl.RoleMapperExpression;
import org.junit.Before;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

public class PutRoleMappingRequestTests extends ESTestCase {

    private PutRoleMappingRequestBuilder builder;

    @Before
    public void setupBuilder() {
        final ElasticsearchClient client = Mockito.mock(ElasticsearchClient.class);
        builder = new PutRoleMappingRequestBuilder(client, PutRoleMappingAction.INSTANCE);
    }

    public void testValidateMissingName() throws Exception {
        final PutRoleMappingRequest request = builder
                .roles("superuser")
                .expression(Mockito.mock(RoleMapperExpression.class))
                .request();
        assertValidationFailure(request, "name");
    }

    public void testValidateMissingRoles() throws Exception {
        final PutRoleMappingRequest request = builder
                .name("test")
                .expression(Mockito.mock(RoleMapperExpression.class))
                .request();
        assertValidationFailure(request, "roles");
    }

    public void testValidateMissingRules() throws Exception {
        final PutRoleMappingRequest request = builder
                .name("test")
                .roles("superuser")
                .request();
        assertValidationFailure(request, "rules");
    }

    public void testValidateMetadataKeys() throws Exception {
        final PutRoleMappingRequest request = builder
                .name("test")
                .roles("superuser")
                .expression(Mockito.mock(RoleMapperExpression.class))
                .metadata(Collections.singletonMap("_secret", false))
                .request();
        assertValidationFailure(request, "metadata key");
    }

    private void assertValidationFailure(PutRoleMappingRequest request, String expectedMessage) {
        final ValidationException ve = request.validate();
        assertThat(ve, notNullValue());
        assertThat(ve.getMessage(), containsString(expectedMessage));
    }

}