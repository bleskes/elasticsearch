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

package org.elasticsearch.xpack.security.authz.privilege;

import org.apache.lucene.util.automaton.Automaton;
import org.elasticsearch.action.admin.cluster.shards.ClusterSearchShardsAction;
import org.elasticsearch.action.admin.indices.alias.exists.AliasesExistAction;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesAction;
import org.elasticsearch.action.admin.indices.create.CreateIndexAction;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexAction;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsAction;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsAction;
import org.elasticsearch.action.admin.indices.get.GetIndexAction;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsAction;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsAction;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingAction;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsAction;
import org.elasticsearch.action.admin.indices.validate.query.ValidateQueryAction;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.xpack.security.support.Automatons;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static org.elasticsearch.xpack.security.support.Automatons.patterns;
import static org.elasticsearch.xpack.security.support.Automatons.unionAndMinimize;

public final class IndexPrivilege extends Privilege {

    private static final Automaton ALL_AUTOMATON = patterns("indices:*");
    private static final Automaton READ_AUTOMATON = patterns("indices:data/read/*");
    private static final Automaton CREATE_AUTOMATON = patterns("indices:data/write/index*", "indices:data/write/bulk*",
            PutMappingAction.NAME);
    private static final Automaton INDEX_AUTOMATON =
            patterns("indices:data/write/index*", "indices:data/write/bulk*", "indices:data/write/update*", PutMappingAction.NAME);
    private static final Automaton DELETE_AUTOMATON = patterns("indices:data/write/delete*", "indices:data/write/bulk*");
    private static final Automaton WRITE_AUTOMATON = patterns("indices:data/write/*", PutMappingAction.NAME);
    private static final Automaton MONITOR_AUTOMATON = patterns("indices:monitor/*");
    private static final Automaton MANAGE_AUTOMATON =
            unionAndMinimize(Arrays.asList(MONITOR_AUTOMATON, patterns("indices:admin/*")));
    private static final Automaton CREATE_INDEX_AUTOMATON = patterns(CreateIndexAction.NAME);
    private static final Automaton DELETE_INDEX_AUTOMATON = patterns(DeleteIndexAction.NAME);
    private static final Automaton VIEW_METADATA_AUTOMATON = patterns(GetAliasesAction.NAME, AliasesExistAction.NAME,
            GetIndexAction.NAME, IndicesExistsAction.NAME, GetFieldMappingsAction.NAME + "*", GetMappingsAction.NAME,
            ClusterSearchShardsAction.NAME, TypesExistsAction.NAME, ValidateQueryAction.NAME + "*", GetSettingsAction.NAME);

    public static final IndexPrivilege NONE =                new IndexPrivilege("none",             Automatons.EMPTY);
    public static final IndexPrivilege ALL =                 new IndexPrivilege("all",              ALL_AUTOMATON);
    public static final IndexPrivilege READ =                new IndexPrivilege("read",                READ_AUTOMATON);
    public static final IndexPrivilege CREATE =              new IndexPrivilege("create",              CREATE_AUTOMATON);
    public static final IndexPrivilege INDEX =               new IndexPrivilege("index",               INDEX_AUTOMATON);
    public static final IndexPrivilege DELETE =              new IndexPrivilege("delete",              DELETE_AUTOMATON);
    public static final IndexPrivilege WRITE =               new IndexPrivilege("write",               WRITE_AUTOMATON);
    public static final IndexPrivilege MONITOR =             new IndexPrivilege("monitor",             MONITOR_AUTOMATON);
    public static final IndexPrivilege MANAGE =              new IndexPrivilege("manage",              MANAGE_AUTOMATON);
    public static final IndexPrivilege DELETE_INDEX =        new IndexPrivilege("delete_index",        DELETE_INDEX_AUTOMATON);
    public static final IndexPrivilege CREATE_INDEX =        new IndexPrivilege("create_index",        CREATE_INDEX_AUTOMATON);
    public static final IndexPrivilege VIEW_METADATA =       new IndexPrivilege("view_index_metadata", VIEW_METADATA_AUTOMATON);

    private static final Map<String, IndexPrivilege> VALUES = MapBuilder.<String, IndexPrivilege>newMapBuilder()
            .put("none", NONE)
            .put("all", ALL)
            .put("manage", MANAGE)
            .put("create_index", CREATE_INDEX)
            .put("monitor", MONITOR)
            .put("read", READ)
            .put("index", INDEX)
            .put("delete", DELETE)
            .put("write", WRITE)
            .put("create", CREATE)
            .put("delete_index", DELETE_INDEX)
            .put("view_index_metadata", VIEW_METADATA)
            .immutableMap();

    public static final Predicate<String> ACTION_MATCHER = ALL.predicate();
    public static final Predicate<String> CREATE_INDEX_MATCHER = CREATE_INDEX.predicate();

    private static final ConcurrentHashMap<Set<String>, IndexPrivilege> CACHE = new ConcurrentHashMap<>();

    private IndexPrivilege(String name, Automaton automaton) {
        super(Collections.singleton(name), automaton);
    }

    private IndexPrivilege(Set<String> name, Automaton automaton) {
        super(name, automaton);
    }

    public static IndexPrivilege get(Set<String> name) {
        return CACHE.computeIfAbsent(name, (theName) -> {
            if (theName.isEmpty()) {
                return NONE;
            } else {
                return resolve(theName);
            }
        });
    }

    private static IndexPrivilege resolve(Set<String> name) {
        final int size = name.size();
        if (size == 0) {
            throw new IllegalArgumentException("empty set should not be used");
        }

        Set<String> actions = new HashSet<>();
        Set<Automaton> automata = new HashSet<>();
        for (String part : name) {
            part = part.toLowerCase(Locale.ROOT);
            if (ACTION_MATCHER.test(part)) {
                actions.add(actionToPattern(part));
            } else {
                IndexPrivilege indexPrivilege = VALUES.get(part);
                if (indexPrivilege != null && size == 1) {
                    return indexPrivilege;
                } else if (indexPrivilege != null) {
                    automata.add(indexPrivilege.automaton);
                } else {
                    throw new IllegalArgumentException("unknown index privilege [" + part + "]. a privilege must be either " +
                            "one of the predefined fixed indices privileges [" +
                            Strings.collectionToCommaDelimitedString(VALUES.entrySet()) + "] or a pattern over one of the available index" +
                            " actions");
                }
            }
        }

        if (actions.isEmpty() == false) {
            automata.add(patterns(actions));
        }
        return new IndexPrivilege(name, unionAndMinimize(automata));
    }

    static Map<String, IndexPrivilege> values() {
        return VALUES;
    }
}
