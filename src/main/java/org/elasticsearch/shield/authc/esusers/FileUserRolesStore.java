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

package org.elasticsearch.shield.authc.esusers;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.base.Charsets;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.internal.Nullable;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.shield.authc.support.UserRolesStore;
import org.elasticsearch.shield.plugin.ShieldPlugin;
import org.elasticsearch.watcher.FileChangesListener;
import org.elasticsearch.watcher.FileWatcher;
import org.elasticsearch.watcher.ResourceWatcherService;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
 */
public class FileUserRolesStore extends AbstractComponent implements UserRolesStore {

    private static final Pattern ROLES_DELIM = Pattern.compile("\\s*,\\s*");

    private final Path file;

    private volatile ImmutableMap<String, String[]> userRoles;

    private final Listener listener;

    @Inject
    public FileUserRolesStore(Settings settings, Environment env, ResourceWatcherService watcherService) {
        this(settings, env, watcherService, Listener.NOOP);
    }

    FileUserRolesStore(Settings settings, Environment env, ResourceWatcherService watcherService, Listener listener) {
        super(settings);
        file = resolveFile(settings, env);
        userRoles = parseFile(file, logger);
        FileWatcher watcher = new FileWatcher(file.getParent().toFile());
        watcher.addListener(new FileListener());
        watcherService.add(watcher);
        this.listener = listener;
    }

    public String[] roles(String username) {
        if (userRoles == null) {
            return Strings.EMPTY_ARRAY;
        }
        String[] roles = userRoles.get(username);
        return roles == null ? Strings.EMPTY_ARRAY : userRoles.get(username);
    }

    public static Path resolveFile(Settings settings, Environment env) {
        String location = settings.get("shield.authc.esusers.files.users_roles");
        if (location == null) {
            return ShieldPlugin.resolveConfigFile(env, ".users_roles");
        }
        return Paths.get(location);
    }

    /**
     * parses the users_roles file. Should never return return {@code null}, if the file doesn't exist
     * an empty map is returned
     */
    public static ImmutableMap<String, String[]> parseFile(Path path, @Nullable ESLogger logger) {
        if (logger != null) {
            logger.trace("Reading users roles file located at [{}]", path);
        }

        if (!Files.exists(path)) {
            return ImmutableMap.of();
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(path, Charsets.UTF_8);
        } catch (IOException ioe) {
            throw new ElasticsearchException("Could not read users file [" + path.toAbsolutePath() + "]", ioe);
        }

        ImmutableMap.Builder<String, String[]> usersRoles = ImmutableMap.builder();

        int lineNr = 0;
        for (String line : lines) {
            lineNr++;
            int i = line.indexOf(":");
            if (i <= 0 || i == line.length() - 1) {
                if (logger != null) {
                    logger.error("Invalid entry in users file [" + path.toAbsolutePath() + "], line [" + lineNr + "]. Skipping...");
                }
                continue;
            }
            String username = line.substring(0, i).trim();
            if (Strings.isEmpty(username)) {
                if (logger != null) {
                    logger.error("Invalid username entry in users file [" + path.toAbsolutePath() + "], line [" + lineNr + "]. Skipping...");
                }
                continue;
            }
            String rolesStr = line.substring(i + 1).trim();
            if (Strings.isEmpty(rolesStr)) {
                if (logger != null) {
                    logger.error("Invalid roles entry in users file [" + path.toAbsolutePath() + "], line [" + lineNr + "]. Skipping...");
                }
                continue;
            }
            String[] roles = ROLES_DELIM.split(rolesStr);
            if (roles.length == 0) {
                if (logger != null) {
                    logger.error("Invalid roles entry in users file [" + path.toAbsolutePath() + "], line [" + lineNr + "]. Skipping...");
                }
                continue;
            }
            usersRoles.put(username, roles);
        }

        return usersRoles.build();
    }

    public static void writeFile(Map<String, String[]> userRoles, Path path) {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path, Charsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {
            for (Map.Entry<String, String[]> entry : userRoles.entrySet()) {
                writer.printf(Locale.ROOT, "%s:%s%s", entry.getKey(), Strings.arrayToCommaDelimitedString(entry.getValue()), System.lineSeparator());
            }
        } catch (IOException ioe) {
            throw new ElasticsearchException("Could not write users file [" + path.toAbsolutePath() + "], please check file permissions");
        }
    }

    private class FileListener extends FileChangesListener {
        @Override
        public void onFileCreated(File file) {
            if (file.equals(FileUserRolesStore.this.file.toFile())) {
                userRoles = parseFile(file.toPath(), logger);
                listener.onRefresh();
            }
        }

        @Override
        public void onFileDeleted(File file) {
            if (file.equals(FileUserRolesStore.this.file.toFile())) {
                userRoles = ImmutableMap.of();
                listener.onRefresh();
            }
        }

        @Override
        public void onFileChanged(File file) {
            if (file.equals(FileUserRolesStore.this.file.toFile())) {
                userRoles = parseFile(file.toPath(), logger);
                listener.onRefresh();
            }
        }
    }

    public static interface Listener {

        static final Listener NOOP = new Listener() {
            @Override
            public void onRefresh() {
            }
        };

        void onRefresh();
    }
}
