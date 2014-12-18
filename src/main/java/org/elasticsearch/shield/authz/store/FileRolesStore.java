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

package org.elasticsearch.shield.authz.store;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.jackson.dataformat.yaml.snakeyaml.error.YAMLException;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.yaml.YamlXContent;
import org.elasticsearch.env.Environment;
import org.elasticsearch.shield.ShieldException;
import org.elasticsearch.shield.ShieldPlugin;
import org.elasticsearch.shield.authz.Permission;
import org.elasticsearch.shield.authz.Privilege;
import org.elasticsearch.shield.support.Validation;
import org.elasticsearch.watcher.FileChangesListener;
import org.elasticsearch.watcher.FileWatcher;
import org.elasticsearch.watcher.ResourceWatcherService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 */
public class FileRolesStore extends AbstractLifecycleComponent<RolesStore> implements RolesStore {

    private static final Pattern COMMA_DELIM = Pattern.compile("\\s*,\\s*");

    private final Path file;
    private final Listener listener;

    private volatile ImmutableMap<String, Permission.Global.Role> permissions;

    @Inject
    public FileRolesStore(Settings settings, Environment env, ResourceWatcherService watcherService) {
        this(settings, env, watcherService, Listener.NOOP);
    }

    public FileRolesStore(Settings settings, Environment env, ResourceWatcherService watcherService, Listener listener) {
        super(settings);
        this.file = resolveFile(settings, env);
        this.listener = listener;
        permissions = ImmutableMap.of();

        FileWatcher watcher = new FileWatcher(file.getParent().toFile());
        watcher.addListener(new FileListener());
        watcherService.add(watcher, ResourceWatcherService.Frequency.HIGH);
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        permissions = parseFile(file, logger);
    }

    @Override
    protected void doStop() throws ElasticsearchException {
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }

    @Override
    public Permission.Global.Role role(String role) {
        return permissions.get(role);
    }

    public static Path resolveFile(Settings settings, Environment env) {
        String location = settings.get("shield.authz.store.files.roles");
        if (location == null) {
            return ShieldPlugin.resolveConfigFile(env, "roles.yml");
        }

        return Paths.get(location);
    }

    public static ImmutableMap<String, Permission.Global.Role> parseFile(Path path, ESLogger logger) {
        if (logger != null) {
            logger.trace("Reading roles file located at [{}]", path);
        }

        if (!Files.exists(path)) {
            return ImmutableMap.of();
        }

        ImmutableMap.Builder<String, Permission.Global.Role> roles = ImmutableMap.builder();
        try (InputStream input = Files.newInputStream(path, StandardOpenOption.READ)) {
            XContentParser parser = YamlXContent.yamlXContent.createParser(input);
            XContentParser.Token token;
            String currentFieldName = null;
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT && token != null) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    currentFieldName = parser.currentName();
                } else if (token == XContentParser.Token.START_OBJECT && currentFieldName != null) {
                    String roleName = currentFieldName;
                    Validation.Error validationError = Validation.Roles.validateRoleName(roleName);
                    if (validationError != null) {
                        throw new ShieldException("Invalid role name [" + roleName + "]... " + validationError);
                    }
                    Permission.Global.Role.Builder permission = Permission.Global.Role.builder(roleName);
                    while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                        if (token == XContentParser.Token.FIELD_NAME) {
                            currentFieldName = parser.currentName();
                        } else if ("cluster".equals(currentFieldName)) {
                            Privilege.Name name = null;
                            if (token == XContentParser.Token.VALUE_STRING) {
                                String namesStr = parser.text().trim();
                                if (Strings.hasLength(namesStr)) {
                                    String[] names = COMMA_DELIM.split(namesStr);
                                    name = new Privilege.Name(names);
                                }
                            } else if (token == XContentParser.Token.START_ARRAY) {
                                Set<String> names = new HashSet<>();
                                while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                                    if (token == XContentParser.Token.VALUE_STRING) {
                                        names.add(parser.text());
                                    }
                                }
                                if (!names.isEmpty()) {
                                    name = new Privilege.Name(names);
                                }
                            } else {
                                throw new ShieldException("Invalid roles file format [" + path.toAbsolutePath() +
                                        "]. [cluster] field value can either be a string or a list of strings, but [" + token + "] was found instead in role [" + roleName + "]");
                            }
                            if (name != null) {
                                permission.set(Privilege.Cluster.get(name));
                            }
                        } else if ("indices".equals(currentFieldName)) {
                            if (token == XContentParser.Token.START_OBJECT) {
                                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                                    if (token == XContentParser.Token.FIELD_NAME) {
                                        currentFieldName = parser.currentName();
                                    } else if (Strings.hasLength(currentFieldName)) {
                                        String[] indices = COMMA_DELIM.split(currentFieldName);
                                        Privilege.Name name = null;
                                        if (token == XContentParser.Token.VALUE_STRING) {
                                            String namesStr = parser.text().trim();
                                            if (Strings.hasLength(namesStr)) {
                                                String[] names = COMMA_DELIM.split(parser.text());
                                                name = new Privilege.Name(names);
                                            }
                                        } else if (token == XContentParser.Token.START_ARRAY) {
                                            Set<String> names = new HashSet<>();
                                            while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                                                if (token == XContentParser.Token.VALUE_STRING) {
                                                    names.add(parser.text());
                                                } else {
                                                    throw new ShieldException("Invalid roles file format [" + path.toAbsolutePath() +
                                                            "]. Could not parse [" + token + "] as index privilege in role[" + roleName + "]. Privilege names must be strings");
                                                }
                                            }
                                            if (!names.isEmpty()) {
                                                name = new Privilege.Name(names);
                                            }
                                        } else {
                                            throw new ShieldException("Invalid roles file format [" + path.toAbsolutePath() +
                                                    "]. Could not parse [" + token + "] as index privileges list in role [" + roleName + "]. Privilege lists must either " +
                                                    "be a comma delimited string or an array of strings");
                                        }
                                        if (name != null) {
                                            permission.add(Privilege.Index.get(name), indices);
                                        }
                                    }
                                }
                            } else {
                                throw new ShieldException("Invalid roles file format [" + path.toAbsolutePath() +
                                        "]. [indices] field value must be an array of indices-privileges mappings defined as a string" +
                                        " in the form <comma-separated list of index name patterns>::<comma-separated list of privileges> , but [" + token + "] was found instead in role [" + roleName + "]");
                            }
                        } else {
                            throw new ShieldException("Invalid roles file format [" + path.toAbsolutePath() +
                                    "]. each role may have [cluster] field (holding a list of cluster permissions) and/or " +
                                    "[indices] field (holding a list of indices permissions. But [" + token + "] was found instead in role [" + roleName + "]");
                        }
                    }
                    roles.put(roleName, permission.build());
                }
            }

            return roles.build();

        } catch (YAMLException | IOException ioe) {
            throw new ElasticsearchException("Failed to read roles file [" + path.toAbsolutePath() + "]", ioe);
        }
    }

    static interface Listener {

        static final Listener NOOP = new Listener() {
            @Override
            public void onRefresh() {
            }
        };

        void onRefresh();
    }

    private class FileListener extends FileChangesListener {

        @Override
        public void onFileCreated(File file) {
            onFileChanged(file);
        }

        @Override
        public void onFileDeleted(File file) {
            onFileChanged(file);
        }

        @Override
        public void onFileChanged(File file) {
            if (file.equals(FileRolesStore.this.file.toFile())) {
                try {
                    permissions = parseFile(file.toPath(), logger);
                    logger.info("updated roles (roles file [{}] changed)", file.getAbsolutePath());
                } catch (Throwable t) {
                    logger.error("could not reload roles file [{}]. Current roles remain unmodified", t, file.getAbsolutePath());
                    return;
                }
                listener.onRefresh();
            }
        }
    }
}
