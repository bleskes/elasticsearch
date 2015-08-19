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

package org.elasticsearch.shield;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.action.ActionModule;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.support.Headers;
import org.elasticsearch.cluster.ClusterModule;
import org.elasticsearch.cluster.settings.Validator;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.http.HttpServerModule;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.shield.action.ShieldActionFilter;
import org.elasticsearch.shield.action.ShieldActionModule;
import org.elasticsearch.shield.action.authc.cache.ClearRealmCacheAction;
import org.elasticsearch.shield.action.authc.cache.TransportClearRealmCacheAction;
import org.elasticsearch.shield.audit.AuditTrailModule;
import org.elasticsearch.shield.authc.AuthenticationModule;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.shield.authc.Realms;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.shield.authc.support.UsernamePasswordToken;
import org.elasticsearch.shield.authz.AuthorizationModule;
import org.elasticsearch.shield.authz.store.FileRolesStore;
import org.elasticsearch.shield.crypto.CryptoModule;
import org.elasticsearch.shield.crypto.InternalCryptoService;
import org.elasticsearch.shield.license.LicenseModule;
import org.elasticsearch.shield.license.LicenseService;
import org.elasticsearch.shield.rest.ShieldRestModule;
import org.elasticsearch.shield.rest.action.RestShieldInfoAction;
import org.elasticsearch.shield.rest.action.authc.cache.RestClearRealmCacheAction;
import org.elasticsearch.shield.ssl.SSLModule;
import org.elasticsearch.shield.transport.ShieldClientTransportService;
import org.elasticsearch.shield.transport.ShieldServerTransportService;
import org.elasticsearch.shield.transport.ShieldTransportModule;
import org.elasticsearch.shield.transport.filter.IPFilter;
import org.elasticsearch.shield.transport.netty.ShieldNettyHttpServerTransport;
import org.elasticsearch.shield.transport.netty.ShieldNettyTransport;
import org.elasticsearch.transport.TransportModule;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 *
 */
public class ShieldPlugin extends Plugin {

    public static final String NAME = "shield";

    public static final String ENABLED_SETTING_NAME = NAME + ".enabled";

    private final Settings settings;
    private final boolean enabled;
    private final boolean clientMode;

    public ShieldPlugin(Settings settings) {
        this.settings = settings;
        this.enabled = shieldEnabled(settings);
        this.clientMode = clientMode(settings);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Elasticsearch Shield (security)";
    }

    @Override
    public Collection<Module> nodeModules() {
        if (enabled == false) {
            return Collections.<Module>singletonList(new ShieldDisabledModule(settings));
        } else if (clientMode) {
            return Arrays.<Module>asList(
                new ShieldTransportModule(settings),
                new SSLModule(settings));
        } else {
            return Arrays.<Module>asList(
                new ShieldModule(settings),
                new LicenseModule(settings),
                new CryptoModule(settings),
                new AuthenticationModule(settings),
                new AuthorizationModule(settings),
                new AuditTrailModule(settings),
                new ShieldRestModule(settings),
                new ShieldActionModule(settings),
                new ShieldTransportModule(settings),
                new SSLModule(settings));
        }
    }

    @Override
    public Collection<Class<? extends LifecycleComponent>> nodeServices() {
        ImmutableList.Builder<Class<? extends LifecycleComponent>> builder = ImmutableList.builder();
        if (enabled && clientMode == false) {
            builder.add(LicenseService.class).add(InternalCryptoService.class).add(FileRolesStore.class).add(Realms.class).add(IPFilter.class);
        }
        return builder.build();
    }

    @Override
    public Settings additionalSettings() {
        if (enabled == false) {
            return Settings.EMPTY;
        }

        Settings.Builder settingsBuilder = Settings.settingsBuilder();
        addUserSettings(settingsBuilder);
        addTribeSettings(settingsBuilder);
        return settingsBuilder.build();
    }

    public void onModule(ClusterModule clusterDynamicSettingsModule) {
        clusterDynamicSettingsModule.registerClusterDynamicSetting("shield.transport.filter.*", Validator.EMPTY);
        clusterDynamicSettingsModule.registerClusterDynamicSetting("shield.http.filter.*", Validator.EMPTY);
        clusterDynamicSettingsModule.registerClusterDynamicSetting("transport.profiles.*", Validator.EMPTY);
        clusterDynamicSettingsModule.registerClusterDynamicSetting(IPFilter.IP_FILTER_ENABLED_SETTING, Validator.EMPTY);
        clusterDynamicSettingsModule.registerClusterDynamicSetting(IPFilter.IP_FILTER_ENABLED_HTTP_SETTING, Validator.EMPTY);
    }

    public void onModule(ActionModule module) {
        if (enabled == false) {
            return;
        }
        // registering the security filter only for nodes
        if (clientMode == false) {
            module.registerFilter(ShieldActionFilter.class);
        }

        // registering all shield actions
        module.registerAction(ClearRealmCacheAction.INSTANCE, TransportClearRealmCacheAction.class);
    }

    public void onModule(TransportModule module) {
        if (enabled == false) {
            return;
        }
        module.setTransport(ShieldNettyTransport.class, ShieldPlugin.NAME);
        if (clientMode) {
            module.setTransportService(ShieldClientTransportService.class, ShieldPlugin.NAME);
        } else {
            module.setTransportService(ShieldServerTransportService.class, ShieldPlugin.NAME);
        }
    }

    public void onModule(HttpServerModule module) {
        if (enabled && clientMode == false) {
            module.setHttpServerTransport(ShieldNettyHttpServerTransport.class, ShieldPlugin.NAME);
        }
    }

    public void onModule(RestModule module) {
        if (enabled && clientMode == false) {
            module.addRestAction(RestClearRealmCacheAction.class);
        }
        // we want to expose the shield rest action even when the plugin is disabled
        module.addRestAction(RestShieldInfoAction.class);
    }

    private void addUserSettings(Settings.Builder settingsBuilder) {
        String authHeaderSettingName = Headers.PREFIX + "." + UsernamePasswordToken.BASIC_AUTH_HEADER;
        if (settings.get(authHeaderSettingName) != null) {
            return;
        }
        String userSetting = settings.get("shield.user");
        if (userSetting == null) {
            return;
        }
        int i = userSetting.indexOf(":");
        if (i < 0 || i == userSetting.length() - 1) {
            throw new IllegalArgumentException("invalid [shield.user] setting. must be in the form of \"<username>:<password>\"");
        }
        String username = userSetting.substring(0, i);
        String password = userSetting.substring(i + 1);
        settingsBuilder.put(authHeaderSettingName, UsernamePasswordToken.basicAuthHeaderValue(username, new SecuredString(password.toCharArray())));
    }

    /*
     We inject additional settings on each tribe client if the current node is a tribe node, to make sure that every tribe has shield installed and enabled too:
     - if shield is loaded on the tribe node we make sure it is also loaded on every tribe, by making it mandatory there
     (this means that the tribe node will fail at startup if shield is not loaded on any tribe due to missing mandatory plugin)
     - if shield is loaded and enabled on the tribe node, we make sure it is also enabled on every tribe, by forcibly enabling it
       (that means it's not possible to disable shield on the tribe clients)
     */
    private void addTribeSettings(Settings.Builder settingsBuilder) {
        Map<String, Settings> tribesSettings = settings.getGroups("tribe", true);
        if (tribesSettings.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Settings> tribeSettings : tribesSettings.entrySet()) {
            String tribePrefix = "tribe." + tribeSettings.getKey() + ".";

            //we copy over existing mandatory plugins under additional settings, as they would get overridden otherwise (arrays don't get merged)
            String[] existingMandatoryPlugins = tribeSettings.getValue().getAsArray("plugin.mandatory", null);
            if (existingMandatoryPlugins == null) {
                //shield is mandatory on every tribe if installed and enabled on the tribe node
                settingsBuilder.putArray(tribePrefix + "plugin.mandatory", NAME);
            } else {
                if (!isShieldMandatory(existingMandatoryPlugins)) {
                    String[] updatedMandatoryPlugins = new String[existingMandatoryPlugins.length + 1];
                    System.arraycopy(existingMandatoryPlugins, 0, updatedMandatoryPlugins, 0, existingMandatoryPlugins.length);
                    updatedMandatoryPlugins[updatedMandatoryPlugins.length - 1] = NAME;
                    //shield is mandatory on every tribe if installed and enabled on the tribe node
                    settingsBuilder.putArray(tribePrefix + "plugin.mandatory", updatedMandatoryPlugins);
                }
            }
            //shield must be enabled on every tribe if it's enabled on the tribe node
            settingsBuilder.put(tribePrefix + ENABLED_SETTING_NAME, true);
        }
    }

    private static boolean isShieldMandatory(String[] existingMandatoryPlugins) {
        for (String existingMandatoryPlugin : existingMandatoryPlugins) {
            if (NAME.equals(existingMandatoryPlugin)) {
                return true;
            }
        }
        return false;
    }

    public static Path configDir(Environment env) {
        return env.configFile().resolve(NAME);
    }

    public static Path resolveConfigFile(Environment env, String name) {
        return configDir(env).resolve(name);
    }

    public static boolean clientMode(Settings settings) {
        return !"node".equals(settings.get(Client.CLIENT_TYPE_SETTING));
    }

    public static boolean shieldEnabled(Settings settings) {
        return settings.getAsBoolean(ENABLED_SETTING_NAME, true);
    }
}
