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

import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.test.ShieldIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.BASIC_AUTH_HEADER;
import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.basicAuthHeaderValue;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.Scope;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

@ClusterScope(scope = Scope.SUITE)
public class IndexAliasesTests extends ShieldIntegrationTest {

    @Override
    protected String configUsers() {
        return super.configUsers() +
                "create_only:{plain}test123\n" +
                "create_test_aliases_test:{plain}test123\n" +
                "create_test_aliases_alias:{plain}test123\n" +
                "create_test_aliases_test_alias:{plain}test123\n" +
                "aliases_only:{plain}test123\n";
    }

    @Override
    protected String configUsersRoles() {
        return super.configUsersRoles() +
                "create_only:create_only\n" +
                "create_test_aliases_test:create_test_aliases_test\n" +
                "create_test_aliases_alias:create_test_aliases_alias\n" +
                "create_test_aliases_test_alias:create_test_aliases_test_alias\n" +
                "aliases_only:aliases_only\n";
    }

    @Override
    protected String configRoles() {
        return super.configRoles() + "\n" +
                //role that has create index only privileges
                "create_only:\n" +
                "  indices:\n" +
                "    '*': create_index\n" +
                //role that has create index and managa aliases on test_*, not enough to manage aliases outside of test_* namespace
                "create_test_aliases_test:\n" +
                "  indices:\n" +
                "    'test_*': create_index,manage_aliases\n" +
                //role that has create index on test_* and manage aliases on alias_*, can't create aliases pointing to test_* though
                "create_test_aliases_alias:\n" +
                "  indices:\n" +
                "    'test_*': create_index\n" +
                "    'alias_*': manage_aliases\n" +
                //role that has create index on test_* and manage_aliases on both alias_* and test_*
                "create_test_aliases_test_alias:\n" +
                "  indices:\n" +
                "    'test_*': create_index\n" +
                "    'alias_*,test_*': manage_aliases\n" +
                //role that has manage_aliases only on both test_* and alias_*
                "aliases_only:\n" +
                "  indices:\n" +
                "    'alias_*,test_*': manage_aliases\n";
    }

    @Before
    public void createBogusIndex() {
        if (randomBoolean()) {
            //randomly create an index with two aliases from user admin, to make sure it doesn't affect any of the test results
            assertAcked(client().admin().indices().prepareCreate("index1").addAlias(new Alias("alias1")).addAlias(new Alias("alias2")));
        }
    }

    @Test
    public void testCreateIndexThenAliasesCreateOnlyPermission() {
        //user has create permission only: allows to create indices, manage_aliases is required to add/remove aliases
        assertAcked(client().admin().indices().prepareCreate("test_1")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_only", new SecuredString("test123".toCharArray()))));

        try {
            client().admin().indices().prepareAliases().addAlias("test_1", "test_alias")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_only", new SecuredString("test123".toCharArray()))).get();
            fail("add alias should have failed due to missing manage_aliases privileges");
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases] is unauthorized for user [create_only]"));
        }

        try {
            client().admin().indices().prepareAliases().addAlias("test_*", "test_alias")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_only", new SecuredString("test123".toCharArray()))).get();
            fail("add alias should have failed due to missing manage_aliases privileges");
        } catch(IndexMissingException e) {
             assertThat(e.getMessage(), containsString("[test_*]"));
        }
    }

    @Test
    public void testCreateIndexAndAliasesCreateOnlyPermission() {
        //user has create permission only: allows to create indices, manage_aliases is required to add aliases although they are part of the same create index request
        try {
            client().admin().indices().prepareCreate("test_1").addAlias(new Alias("test_2"))
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_only", new SecuredString("test123".toCharArray()))).get();
            fail("create index should have failed due to missing manage_aliases privileges");
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases] is unauthorized for user [create_only]"));
        }
    }

    @Test
    public void testDeleteAliasesCreateOnlyPermission() {
        //user has create permission only: allows to create indices, manage_aliases is required to add/remove aliases
        try {
            client().admin().indices().prepareAliases().removeAlias("test_1", "alias_1")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_only", new SecuredString("test123".toCharArray()))).get();
            fail("remove alias should have failed due to missing manage_aliases privileges");
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases] is unauthorized for user [create_only]"));
        }

        try {
            client().admin().indices().prepareAliases().removeAlias("test_1", "alias_*")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_only", new SecuredString("test123".toCharArray()))).get();
            fail("remove alias should have failed due to missing manage_aliases privileges");
        } catch(IndexMissingException e) {
            assertThat(e.getMessage(), containsString("[alias_*"));
        }

        try {
            client().admin().indices().prepareAliases().removeAlias("test_1", "_all")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_only", new SecuredString("test123".toCharArray()))).get();
            fail("remove alias should have failed due to missing manage_aliases privileges");
        } catch(IndexMissingException e) {
            assertThat(e.getMessage(), containsString("[_all]"));
        }
    }

    @Test
    public void testGetAliasesCreateOnlyPermission() {
        //user has create permission only: allows to create indices, manage_aliases is required to retrieve aliases though
        try {
            client().admin().indices().prepareGetAliases("test_1").setIndices("test_1").setIndicesOptions(IndicesOptions.lenientExpandOpen())
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_only", new SecuredString("test123".toCharArray()))).get();
            fail("get alias should have failed due to missing manage_aliases privileges");
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases/get] is unauthorized for user [create_only]"));
        }

        try {
            client().admin().indices().prepareGetAliases("_all").setIndices("test_1").setIndicesOptions(IndicesOptions.lenientExpandOpen())
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_only", new SecuredString("test123".toCharArray()))).get();
            fail("get alias should have failed due to missing manage_aliases privileges");
        } catch(IndexMissingException e) {
            assertThat(e.getMessage(), containsString("[_all]"));
        }

        try {
            client().admin().indices().prepareGetAliases().setIndices("test_1").setIndicesOptions(IndicesOptions.lenientExpandOpen())
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_only", new SecuredString("test123".toCharArray()))).get();
            fail("get alias should have failed due to missing manage_aliases privileges");
        } catch(IndexMissingException e) {
            assertThat(e.getMessage(), containsString("[_all]"));
        }

        try {
            client().admin().indices().prepareGetAliases("test_alias").setIndices("test_*").setIndicesOptions(IndicesOptions.lenientExpandOpen())
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_only", new SecuredString("test123".toCharArray()))).get();
            fail("get alias should have failed due to missing manage_aliases privileges");
        } catch(IndexMissingException e) {
            assertThat(e.getMessage(), containsString("[test_*]"));
        }

        try {
            client().admin().indices().prepareGetAliases()
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_only", new SecuredString("test123".toCharArray()))).get();
            fail("get alias should have failed due to missing manage_aliases privileges");
        } catch(IndexMissingException e) {
            assertThat(e.getMessage(), containsString("[_all]"));
        }
    }

    @Test
    public void testCreateIndexThenAliasesCreateAndAliasesPermission() {
        //user has create and manage_aliases permission on test_*. manage_aliases is required to add/remove aliases on both aliases and indices
        assertAcked(client().admin().indices().prepareCreate("test_1")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))));

        //ok: user has manage_aliases on test_*
        assertAcked(client().admin().indices().prepareAliases().addAlias("test_1", "test_alias")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))));

        //ok: user has manage_aliases on test_*
        assertAcked(client().admin().indices().prepareAliases().addAlias("test_*", "test_alias_2")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))));

        try {
            //fails: user doesn't have manage_aliases on alias_1
            client().admin().indices().prepareAliases().addAlias("test_1", "alias_1", "test_alias")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))).get();
            fail("add alias should have failed due to missing manage_aliases privileges on alias_1");
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases] is unauthorized for user [create_test_aliases_test]"));
        }
    }

    @Test
    public void testCreateIndexAndAliasesCreateAndAliasesPermission() {
        //user has create and manage_aliases permission on test_*. manage_aliases is required to add/remove aliases on both aliases and indices
        //ok: user has manage_aliases on test_*
        assertAcked(client().admin().indices().prepareCreate("test_1").addAlias(new Alias("test_alias"))
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))));

        try {
            //fails: user doesn't have manage_aliases on alias_1
            client().admin().indices().prepareCreate("test_2").addAlias(new Alias("test_alias")).addAlias(new Alias("alias_2"))
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))).get();
            fail("create index should have failed due to missing manage_aliases privileges on alias_2");
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases] is unauthorized for user [create_test_aliases_test]"));
        }
    }

    @Test
    public void testDeleteAliasesCreateAndAliasesPermission() {
        //user has create and manage_aliases permission on test_*. manage_aliases is required to add/remove aliases on both aliases and indices
        //ok: user has manage_aliases on test_*
        assertAcked(client().admin().indices().prepareCreate("test_1").addAlias(new Alias("test_alias_1")).addAlias(new Alias("test_alias_2"))
                .addAlias(new Alias("test_alias_3")).addAlias(new Alias("test_alias_4"))
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))));
        //ok: user has manage_aliases on test_*
        assertAcked(client().admin().indices().prepareAliases().removeAlias("test_1", "test_alias_1")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))));
        //ok: user has manage_aliases on test_*
        assertAcked(client().admin().indices().prepareAliases().removeAlias("test_*", "test_alias_2")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))));
        //ok: user has manage_aliases on test_*
        assertAcked(client().admin().indices().prepareAliases().removeAlias("test_1", "test_alias_*")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))));

        try {
            //fails: all aliases have been deleted, no existing aliases match test_alias_*
            client().admin().indices().prepareAliases().removeAlias("test_1", "test_alias_*")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))).get();
            fail("remove alias should have failed due to no existing matching aliases to expand test_alias_* to");
        } catch(IndexMissingException e) {
            assertThat(e.getMessage(), containsString("[test_alias_*]"));
        }

        try {
            //fails: all aliases have been deleted, no existing aliases match _all
            client().admin().indices().prepareAliases().removeAlias("test_1", "_all")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))).get();
            fail("remove alias should have failed due to no existing matching aliases to expand _all to");
        } catch(IndexMissingException e) {
            assertThat(e.getMessage(), containsString("[_all]"));
        }

        try {
            //fails: user doesn't have manage_aliases on alias_1
            client().admin().indices().prepareAliases().removeAlias("test_1", "alias_1")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))).get();
            fail("remove alias should have failed due to missing manage_aliases privileges on alias_1");
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases] is unauthorized for user [create_test_aliases_test]"));
        }

        try {
            //fails: user doesn't have manage_aliases on alias_1
            client().admin().indices().prepareAliases().removeAlias("test_1", new String[]{"_all", "alias_1"})
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))).get();
            fail("remove alias should have failed due to missing manage_aliases privileges on alias_1");
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases] is unauthorized for user [create_test_aliases_test]"));
        }
    }

    @Test
    public void testGetAliasesCreateAndAliasesPermission() {
        //user has create and manage_aliases permission on test_*. manage_aliases is required to retrieve aliases on both aliases and indices

        assertAcked(client().admin().indices().prepareCreate("test_1").addAlias(new Alias("test_alias"))
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))));

        //ok: user has manage_aliases on test_*
        assertAliases(client().admin().indices().prepareGetAliases().setAliases("test_alias").setIndices("test_1")
                        .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))),
                "test_1", "test_alias");

        //ok: user has manage_aliases on test_*, test_* gets resolved to test_1
        assertAliases(client().admin().indices().prepareGetAliases().setAliases("test_alias").setIndices("test_*")
                        .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))),
                "test_1", "test_alias");

        //ok: user has manage_aliases on test_*, empty indices gets resolved to _all indices (thus test_1)
        assertAliases(client().admin().indices().prepareGetAliases().setAliases("test_alias")
                        .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))),
                "test_1", "test_alias");

        //ok: user has manage_aliases on test_*, _all aliases gets resolved to test_alias and empty indices gets resolved to  _all indices (thus test_1)
        assertAliases(client().admin().indices().prepareGetAliases().setAliases("_all").setIndices("test_1")
                        .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))),
                "test_1", "test_alias");

        //ok: user has manage_aliases on test_*, empty aliases gets resolved to test_alias and empty indices gets resolved to  _all indices (thus test_1)
        assertAliases(client().admin().indices().prepareGetAliases().setIndices("test_1")
                        .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))),
                "test_1", "test_alias");

        //ok: user has manage_aliases on test_*, test_* aliases gets resolved to test_alias and empty indices gets resolved to  _all indices (thus test_1)
        assertAliases(client().admin().indices().prepareGetAliases().setAliases("test_*").setIndices("test_1")
                        .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))),
                "test_1", "test_alias");

        //ok: user has manage_aliases on test_*, _all aliases gets resolved to test_alias and _all indices becomes test_1
        assertAliases(client().admin().indices().prepareGetAliases().setAliases("_all").setIndices("_all")
                        .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))),
                "test_1", "test_alias");

        //ok: user has manage_aliases on test_*, empty aliases gets resolved to test_alias and empty indices becomes test_1
        assertAliases(client().admin().indices().prepareGetAliases()
                        .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))),
                "test_1", "test_alias");

        try {
            //fails: user has manage_aliases on test_*, although _all aliases and empty indices can be resolved, the explicit non authorized alias (alias_1) causes the request to fail
            client().admin().indices().prepareGetAliases().setAliases("_all", "alias_1")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))).get();
            fail("get alias should have failed due to missing manage_aliases privileges on alias_1");
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases/get] is unauthorized for user [create_test_aliases_test]"));
        }

        try {
            //fails: user doesn't have manage_aliases on alias_1
            client().admin().indices().prepareGetAliases().setAliases("alias_1")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test", new SecuredString("test123".toCharArray()))).get();
            fail("get alias should have failed due to missing manage_aliases privileges on alias_1");
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases/get] is unauthorized for user [create_test_aliases_test]"));
        }
    }

    @Test
    public void testCreateIndexThenAliasesCreateAndAliasesPermission2() {
        //user has create permission on test_* and manage_aliases permission on alias_*. manage_aliases is required to add/remove aliases on both aliases and indices
        assertAcked(client().admin().indices().prepareCreate("test_1")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_alias", new SecuredString("test123".toCharArray()))));

        try {
            //fails: user doesn't have manage aliases on test_1
            client().admin().indices().prepareAliases().addAlias("test_1", "test_alias")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_alias", new SecuredString("test123".toCharArray()))).get();
            fail("add alias should have failed due to missing manage_aliases privileges on test_alias and test_1");
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases] is unauthorized for user [create_test_aliases_alias]"));
        }

        try {
            //fails: user doesn't have manage aliases on test_1
            client().admin().indices().prepareAliases().addAlias("test_1", "alias_1")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_alias", new SecuredString("test123".toCharArray()))).get();
            fail("add alias should have failed due to missing manage_aliases privileges on test_1");
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases] is unauthorized for user [create_test_aliases_alias]"));
        }

        try {
            //fails: user doesn't have manage aliases on test_*, no matching indices to replace wildcards
            client().admin().indices().prepareAliases().addAlias("test_*", "alias_1")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_alias", new SecuredString("test123".toCharArray()))).get();
            fail("add alias should have failed due to missing manage_aliases privileges on test_1");
        } catch(IndexMissingException e) {
            assertThat(e.getMessage(), containsString("[test_*]"));
        }
    }

    @Test
    public void testCreateIndexAndAliasesCreateAndAliasesPermission2() {
        //user has create permission on test_* and manage_aliases permission on alias_*. manage_aliases is required to add/remove aliases on both aliases and indices
        try {
            //fails: user doesn't have manage_aliases on test_1, create index is rejected as a whole
            client().admin().indices().prepareCreate("test_1").addAlias(new Alias("test_alias"))
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_alias", new SecuredString("test123".toCharArray()))).get();
            fail("create index should have failed due to missing manage_aliases privileges on test_1 and test_alias");
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases] is unauthorized for user [create_test_aliases_alias]"));
        }

        try {
            //fails: user doesn't have manage_aliases on test_*, create index is rejected as a whole
            client().admin().indices().prepareCreate("test_1").addAlias(new Alias("test_alias")).addAlias(new Alias("alias_1"))
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_alias", new SecuredString("test123".toCharArray()))).get();
            fail("create index should have failed due to missing manage_aliases privileges on test_1 and test_alias");
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases] is unauthorized for user [create_test_aliases_alias]"));
        }
    }

    @Test
    public void testDeleteAliasesCreateAndAliasesPermission2() {
        //user has create permission on test_* and manage_aliases permission on alias_*. manage_aliases is required to add/remove aliases on both aliases and indices
        try {
            //fails: user doesn't have manage_aliases on test_1
            client().admin().indices().prepareAliases().removeAlias("test_1", "test_alias")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_alias", new SecuredString("test123".toCharArray()))).get();
            fail("remove alias should have failed due to missing manage_aliases privileges on test_alias and test_1");
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases] is unauthorized for user [create_test_aliases_alias]"));
        }

        try {
            //fails: user doesn't have manage_aliases on test_1
            client().admin().indices().prepareAliases().removeAlias("test_1", "alias_1")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_alias", new SecuredString("test123".toCharArray()))).get();
            fail("remove alias should have failed due to missing manage_aliases privileges on test_1");
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases] is unauthorized for user [create_test_aliases_alias]"));
        }

        try {
            //fails: user doesn't have manage_aliases on test_*, wildcards can't get replaced
            client().admin().indices().prepareAliases().removeAlias("test_*", "alias_1")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_alias", new SecuredString("test123".toCharArray()))).get();
            fail("remove alias should have failed due to missing manage_aliases privileges on test_*");
        } catch(IndexMissingException e) {
            assertThat(e.getMessage(), containsString("[test_*]"));
        }
    }

    @Test
    public void testGetAliasesCreateAndAliasesPermission2() {
        //user has create permission on test_* and manage_aliases permission on alias_*. manage_aliases is required to retrieve aliases on both aliases and indices
        assertAcked(client().admin().indices().prepareCreate("test_1")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_alias", new SecuredString("test123".toCharArray()))));

        try {
            //fails: user doesn't have manage aliases on test_1, nor test_alias
            client().admin().indices().prepareGetAliases().setAliases("test_alias").setIndices("test_1")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_alias", new SecuredString("test123".toCharArray()))).get();
            fail("get alias should have failed due to missing manage_aliases privileges on test_alias and test_1");
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases/get] is unauthorized for user [create_test_aliases_alias]"));
        }

        try {
            //fails: user doesn't have manage aliases on test_*, no matching indices to replace wildcards
            client().admin().indices().prepareGetAliases().setIndices("test_*").setAliases("test_alias")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_alias", new SecuredString("test123".toCharArray()))).get();
            fail("get alias should have failed due to missing manage_aliases privileges on test_*");
        } catch(IndexMissingException e) {
            assertThat(e.getMessage(), containsString("[test_*]"));
        }

        try {
            //fails: no existing indices to replace empty indices (thus _all)
            client().admin().indices().prepareGetAliases().setAliases("test_alias")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_alias", new SecuredString("test123".toCharArray()))).get();
            fail("get alias should have failed due to missing manage_aliases privileges on any index");
        } catch(IndexMissingException e) {
            assertThat(e.getMessage(), containsString("[_all]"));
        }

        try {
            //fails: no existing aliases to replace wildcards
            client().admin().indices().prepareGetAliases().setIndices("test_1").setAliases("test_*")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_alias", new SecuredString("test123".toCharArray()))).get();
            fail("get alias should have failed due to missing manage_aliases privileges on test_1");
        } catch(IndexMissingException e) {
            assertThat(e.getMessage(), containsString("[test_*]"));
        }

        try {
            //fails: no existing aliases to replace _all
            client().admin().indices().prepareGetAliases().setIndices("test_1").setAliases("_all")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_alias", new SecuredString("test123".toCharArray()))).get();
            fail("get alias should have failed due to missing manage_aliases privileges on test_1");
        } catch(IndexMissingException e) {
            assertThat(e.getMessage(), containsString("[_all]"));
        }

        try {
            //fails: no existing aliases to replace empty aliases
            client().admin().indices().prepareGetAliases().setIndices("test_1")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_alias", new SecuredString("test123".toCharArray()))).get();
            fail("get alias should have failed due to missing manage_aliases privileges on test_1");
        } catch(IndexMissingException e) {
            assertThat(e.getMessage(), containsString("[_all]"));
        }

        try {
            //fails: no existing aliases to replace empty aliases
            client().admin().indices().prepareGetAliases()
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_alias", new SecuredString("test123".toCharArray()))).get();
            fail("get alias should have failed due to missing manage_aliases privileges on test_1");
        } catch(IndexMissingException e) {
            assertThat(e.getMessage(), containsString("[_all]"));
        }
    }

    @Test
    public void testCreateIndexThenAliasesCreateAndAliasesPermission3() {
        //user has create permission on test_* and manage_aliases permission on test_*,alias_*. All good.
        assertAcked(client().admin().indices().prepareCreate("test_1")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))));

        assertAcked(client().admin().indices().prepareAliases().addAlias("test_1", "test_alias")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))));

        assertAcked(client().admin().indices().prepareAliases().addAlias("test_1", "alias_1")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))));

        assertAcked(client().admin().indices().prepareAliases().addAlias("test_*", "alias_2")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))));
    }

    @Test
    public void testCreateIndexAndAliasesCreateAndAliasesPermission3() {
        //user has create permission on test_* and manage_aliases permission on test_*,alias_*. All good.
        assertAcked(client().admin().indices().prepareCreate("test_1").addAlias(new Alias("test_alias"))
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))));

        assertAcked(client().admin().indices().prepareCreate("test_2").addAlias(new Alias("test_alias_2")).addAlias(new Alias("alias_2"))
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))));
    }

    @Test
    public void testDeleteAliasesCreateAndAliasesPermission3() {
        //user has create permission on test_* and manage_aliases permission on test_*,alias_*. All good.
        assertAcked(client().admin().indices().prepareCreate("test_1").addAlias(new Alias("test_alias")).addAlias(new Alias("alias_1"))
                .addAlias(new Alias("alias_2")).addAlias(new Alias("alias_3"))
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))));

        try {
            //fails: user doesn't have manage_aliases privilege on non_authorized
            client().admin().indices().prepareAliases().removeAlias("test_1", "non_authorized").removeAlias("test_1", "test_alias")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))).get();
            fail("remove alias should have failed due to missing manage_aliases privileges on non_authorized");
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases] is unauthorized for user [create_test_aliases_test_alias]"));
        }

        assertAcked(client().admin().indices().prepareAliases().removeAlias("test_1", "alias_1")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))));

        assertAcked(client().admin().indices().prepareAliases().removeAlias("test_*", "_all")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))));


        try {
            //fails: all aliases have been deleted, _all can't be resolved to any existing authorized aliases
            client().admin().indices().prepareAliases().removeAlias("test_1", "_all")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))).get();
            fail("remove alias should have failed due to no existing aliases matching _all");
        } catch(IndexMissingException e) {
            assertThat(e.getMessage(), containsString("[_all]"));
        }
    }

    @Test
    public void testGetAliasesCreateAndAliasesPermission3() {
        //user has create permission on test_* and manage_aliases permission on test_*,alias_*. All good.
        assertAcked(client().admin().indices().prepareCreate("test_1").addAlias(new Alias("test_alias")).addAlias(new Alias("alias_1"))
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))));

        assertAliases(client().admin().indices().prepareGetAliases().setAliases("test_alias").setIndices("test_1")
                        .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))),
                "test_1", "test_alias");

        assertAliases(client().admin().indices().prepareGetAliases().setAliases("alias_1").setIndices("test_1")
                        .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))),
                "test_1", "alias_1");

        assertAliases(client().admin().indices().prepareGetAliases().setAliases("alias_1").setIndices("test_*")
                        .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))),
                "test_1", "alias_1");

        assertAliases(client().admin().indices().prepareGetAliases().setAliases("test_*").setIndices("test_1")
                        .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))),
                "test_1", "test_alias");

        assertAliases(client().admin().indices().prepareGetAliases().setAliases("_all").setIndices("test_1")
                        .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))),
                "test_1", "alias_1", "test_alias");

        assertAliases(client().admin().indices().prepareGetAliases().setAliases("_all")
                        .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))),
                "test_1", "alias_1", "test_alias");

        assertAliases(client().admin().indices().prepareGetAliases().setIndices("test_1")
                        .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))),
                "test_1", "alias_1", "test_alias");

        assertAliases(client().admin().indices().prepareGetAliases()
                        .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))),
                "test_1", "alias_1", "test_alias");

        assertAliases(client().admin().indices().prepareGetAliases().setAliases("alias_*").setIndices("test_*")
                        .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("create_test_aliases_test_alias", new SecuredString("test123".toCharArray()))),
                "test_1", "alias_1");
    }

    @Test(expected = AuthorizationException.class)
    public void testCreateIndexAliasesOnlyPermission() {
        client().admin().indices().prepareCreate("test_1")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("aliases_only", new SecuredString("test123".toCharArray()))).get();
    }

    @Test
    public void testGetAliasesAliasesOnlyPermission() {
        //user has manage_aliases only permissions on both alias_* and test_*

        //ok: manage_aliases on both test_* and alias_*
        GetAliasesResponse getAliasesResponse = client().admin().indices().prepareGetAliases("alias_1").addIndices("test_1").setIndicesOptions(IndicesOptions.lenientExpandOpen())
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("aliases_only", new SecuredString("test123".toCharArray()))).get();
        assertThat(getAliasesResponse.getAliases().isEmpty(), is(true));

        try {
            //fails: no manage_aliases privilege on non_authorized alias
            client().admin().indices().prepareGetAliases("non_authorized").addIndices("test_1").setIndicesOptions(IndicesOptions.lenientExpandOpen())
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("aliases_only", new SecuredString("test123".toCharArray()))).get();
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases/get] is unauthorized for user [aliases_only]"));
        }

        try {
            //fails: no manage_aliases privilege on non_authorized index
            client().admin().indices().prepareGetAliases("alias_1").addIndices("non_authorized").setIndicesOptions(IndicesOptions.lenientExpandOpen())
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("aliases_only", new SecuredString("test123".toCharArray()))).get();
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/aliases/get] is unauthorized for user [aliases_only]"));
        }
    }

    private static void assertAliases(GetAliasesRequestBuilder getAliasesRequestBuilder, String index, String... aliases) {
        GetAliasesResponse getAliasesResponse = getAliasesRequestBuilder.get();
        assertThat(getAliasesResponse.getAliases().size(), equalTo(1));
        assertThat(getAliasesResponse.getAliases().get(index).size(), equalTo(aliases.length));
        for (int i = 0; i < aliases.length; i++) {
            assertThat(getAliasesResponse.getAliases().get(index).get(i).alias(), equalTo(aliases[i]));
        }
    }
}
