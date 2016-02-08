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

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexModule;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.shield.action.ShieldActionFilter;
import org.elasticsearch.shield.action.ShieldActionModule;
import org.elasticsearch.shield.action.realm.ClearRealmCacheAction;
import org.elasticsearch.shield.action.realm.TransportClearRealmCacheAction;
import org.elasticsearch.shield.action.role.AddRoleAction;
import org.elasticsearch.shield.action.role.ClearRolesCacheAction;
import org.elasticsearch.shield.action.role.DeleteRoleAction;
import org.elasticsearch.shield.action.role.GetRolesAction;
import org.elasticsearch.shield.action.role.TransportAddRoleAction;
import org.elasticsearch.shield.action.role.TransportClearRolesCacheAction;
import org.elasticsearch.shield.action.role.TransportDeleteRoleAction;
import org.elasticsearch.shield.action.role.TransportGetRolesAction;
import org.elasticsearch.shield.action.user.AddUserAction;
import org.elasticsearch.shield.action.user.DeleteUserAction;
import org.elasticsearch.shield.action.user.GetUsersAction;
import org.elasticsearch.shield.action.user.TransportAddUserAction;
import org.elasticsearch.shield.action.user.TransportDeleteUserAction;
import org.elasticsearch.shield.action.user.TransportGetUsersAction;
import org.elasticsearch.shield.audit.AuditTrailModule;
import org.elasticsearch.shield.audit.logfile.LoggingAuditTrail;
import org.elasticsearch.shield.authc.AuthenticationModule;
import org.elasticsearch.shield.authc.Realms;
import org.elasticsearch.shield.authc.ldap.support.SessionFactory;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.shield.authc.support.UsernamePasswordToken;
import org.elasticsearch.shield.authz.AuthorizationModule;
import org.elasticsearch.shield.authz.accesscontrol.OptOutQueryCache;
import org.elasticsearch.shield.authz.accesscontrol.ShieldIndexSearcherWrapper;
import org.elasticsearch.shield.authz.store.FileRolesStore;
import org.elasticsearch.shield.crypto.CryptoModule;
import org.elasticsearch.shield.crypto.InternalCryptoService;
import org.elasticsearch.shield.license.LicenseModule;
import org.elasticsearch.shield.license.ShieldLicenseState;
import org.elasticsearch.shield.license.ShieldLicensee;
import org.elasticsearch.shield.rest.ShieldRestModule;
import org.elasticsearch.shield.rest.action.RestAuthenticateAction;
import org.elasticsearch.shield.rest.action.RestShieldInfoAction;
import org.elasticsearch.shield.rest.action.realm.RestClearRealmCacheAction;
import org.elasticsearch.shield.rest.action.role.RestAddRoleAction;
import org.elasticsearch.shield.rest.action.role.RestClearRolesCacheAction;
import org.elasticsearch.shield.rest.action.role.RestDeleteRoleAction;
import org.elasticsearch.shield.rest.action.role.RestGetRolesAction;
import org.elasticsearch.shield.rest.action.user.RestAddUserAction;
import org.elasticsearch.shield.rest.action.user.RestDeleteUserAction;
import org.elasticsearch.shield.rest.action.user.RestGetUsersAction;
import org.elasticsearch.shield.ssl.SSLModule;
import org.elasticsearch.shield.transport.ShieldClientTransportService;
import org.elasticsearch.shield.transport.ShieldServerTransportService;
import org.elasticsearch.shield.transport.ShieldTransportModule;
import org.elasticsearch.shield.transport.filter.IPFilter;
import org.elasticsearch.shield.transport.netty.ShieldNettyHttpServerTransport;
import org.elasticsearch.shield.transport.netty.ShieldNettyTransport;
import org.elasticsearch.xpack.XPackPlugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ShieldPlugin extends Plugin {

    public static final String NAME = "shield";
    public static final String ENABLED_SETTING_NAME = NAME + ".enabled";
    public static final String OPT_OUT_QUERY_CACHE = "opt_out_cache";
    public static final String DLS_FLS_ENABLED_SETTING = "shield.dls_fls.enabled";

    private static final boolean DEFAULT_ENABLED_SETTING = true;

    private final Settings settings;
    private final boolean enabled;
    private final boolean clientMode;
    private ShieldLicenseState shieldLicenseState;

    public ShieldPlugin(Settings settings) {
        this.settings = settings;
        this.enabled = shieldEnabled(settings);
        this.clientMode = clientMode(settings);
        if (enabled && clientMode == false) {
            failIfShieldQueryCacheIsNotActive(settings, true);
        }
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

        if (!enabled) {
            return Collections.singletonList(new ShieldDisabledModule(settings));
        }

        if (clientMode) {
            return Arrays.<Module>asList(
                    new ShieldTransportModule(settings),
                    new SSLModule(settings));
        }

        // we can't load that at construction time since the license plugin might not have been loaded at that point
        // which might not be the case during Plugin class instantiation. Once nodeModules are pulled
        // everything should have been loaded
        shieldLicenseState = new ShieldLicenseState();
        return Arrays.<Module>asList(
                new ShieldModule(settings),
                new LicenseModule(settings, shieldLicenseState),
                new CryptoModule(settings),
                new AuthenticationModule(settings),
                new AuthorizationModule(settings),
                new AuditTrailModule(settings),
                new ShieldRestModule(settings),
                new ShieldActionModule(settings),
                new ShieldTransportModule(settings),
                new SSLModule(settings));
    }

    @Override
    public Collection<Class<? extends LifecycleComponent>> nodeServices() {
        if (enabled && clientMode == false) {
            List<Class<? extends LifecycleComponent>> list = new ArrayList<>();
            if (AuditTrailModule.fileAuditLoggingEnabled(settings)) {
                list.add(LoggingAuditTrail.class);
            }
            list.add(ShieldLicensee.class);
            list.add(InternalCryptoService.class);
            list.add(FileRolesStore.class);
            list.add(Realms.class);
            return list;
        }
        return Collections.emptyList();
    }

    @Override
    public Settings additionalSettings() {
        if (enabled == false) {
            return Settings.EMPTY;
        }

        Settings.Builder settingsBuilder = Settings.settingsBuilder();
        settingsBuilder.put(NetworkModule.TRANSPORT_TYPE_KEY, ShieldPlugin.NAME);
        settingsBuilder.put(NetworkModule.TRANSPORT_SERVICE_TYPE_KEY, ShieldPlugin.NAME);
        settingsBuilder.put(NetworkModule.HTTP_TYPE_SETTING.getKey(), ShieldPlugin.NAME);
        addUserSettings(settingsBuilder);
        addTribeSettings(settingsBuilder);
        addQueryCacheSettings(settingsBuilder);
        return settingsBuilder.build();
    }

    public void onModule(SettingsModule settingsModule) {
        settingsModule.registerSetting(IPFilter.IP_FILTER_ENABLED_SETTING);
        settingsModule.registerSetting(IPFilter.IP_FILTER_ENABLED_HTTP_SETTING);
        settingsModule.registerSetting(IPFilter.HTTP_FILTER_ALLOW_SETTING);
        settingsModule.registerSetting(IPFilter.HTTP_FILTER_DENY_SETTING);
        settingsModule.registerSetting(IPFilter.TRANSPORT_FILTER_ALLOW_SETTING);
        settingsModule.registerSetting(IPFilter.TRANSPORT_FILTER_DENY_SETTING);
        settingsModule.registerSetting(Setting.boolSetting("plugins.load_classpath_plugins", true, false, Setting.Scope.CLUSTER));
        // TODO add real settings for this wildcard here
        settingsModule.registerSetting(Setting.groupSetting("shield.", false, Setting.Scope.CLUSTER));
        String[] asArray = settings.getAsArray("shield.hide_settings");
        for (String pattern : asArray) {
            settingsModule.registerSettingsFilter(pattern);
        }
        settingsModule.registerSettingsFilter("shield.hide_settings");
        settingsModule.registerSettingsFilter("shield.ssl.*");
        settingsModule.registerSettingsFilter("shield.authc.realms.*.bind_dn");
        settingsModule.registerSettingsFilter("shield.authc.realms.*.bind_password");
        settingsModule.registerSettingsFilter("shield.authc.realms.*." + SessionFactory.HOSTNAME_VERIFICATION_SETTING);
        settingsModule.registerSettingsFilter("shield.authc.realms.*.truststore.password");
        settingsModule.registerSettingsFilter("shield.authc.realms.*.truststore.path");
        settingsModule.registerSettingsFilter("shield.authc.realms.*.truststore.algorithm");
        settingsModule.registerSettingsFilter("transport.profiles.*.shield.*");
    }

    @Override
    public void onIndexModule(IndexModule module) {
        if (enabled == false) {
            return;
        }
        assert shieldLicenseState != null;
        if (flsDlsEnabled(settings)) {
            module.setSearcherWrapper((indexService) -> new ShieldIndexSearcherWrapper(indexService.getIndexSettings(),
                    indexService.newQueryShardContext(), indexService.mapperService(),
                    indexService.cache().bitsetFilterCache(), indexService.getIndexServices().getThreadPool().getThreadContext(),
                    shieldLicenseState));
        }
        if (clientMode == false) {
            module.registerQueryCache(ShieldPlugin.OPT_OUT_QUERY_CACHE, OptOutQueryCache::new);
            failIfShieldQueryCacheIsNotActive(module.getSettings(), false);
        }
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
        module.registerAction(ClearRolesCacheAction.INSTANCE, TransportClearRolesCacheAction.class);
        module.registerAction(GetUsersAction.INSTANCE, TransportGetUsersAction.class);
        module.registerAction(AddUserAction.INSTANCE, TransportAddUserAction.class);
        module.registerAction(DeleteUserAction.INSTANCE, TransportDeleteUserAction.class);
        module.registerAction(GetRolesAction.INSTANCE, TransportGetRolesAction.class);
        module.registerAction(AddRoleAction.INSTANCE, TransportAddRoleAction.class);
        module.registerAction(DeleteRoleAction.INSTANCE, TransportDeleteRoleAction.class);
    }

    public void onModule(NetworkModule module) {
        if (clientMode == false) {
            // we want to expose the shield rest action even when the plugin is disabled
            module.registerRestHandler(RestShieldInfoAction.class);
        }

        if (enabled == false) {
            return;
        }

        module.registerTransport(ShieldPlugin.NAME, ShieldNettyTransport.class);
        if (clientMode) {
            module.registerTransportService(ShieldPlugin.NAME, ShieldClientTransportService.class);
        } else {
            module.registerTransportService(ShieldPlugin.NAME, ShieldServerTransportService.class);
        }

        if (clientMode == false) {
            module.registerRestHandler(RestAuthenticateAction.class);
            module.registerRestHandler(RestClearRealmCacheAction.class);
            module.registerRestHandler(RestClearRolesCacheAction.class);
            module.registerRestHandler(RestGetUsersAction.class);
            module.registerRestHandler(RestAddUserAction.class);
            module.registerRestHandler(RestDeleteUserAction.class);
            module.registerRestHandler(RestGetRolesAction.class);
            module.registerRestHandler(RestAddRoleAction.class);
            module.registerRestHandler(RestDeleteRoleAction.class);
            module.registerHttpTransport(ShieldPlugin.NAME, ShieldNettyHttpServerTransport.class);
        }
    }

    private void addUserSettings(Settings.Builder settingsBuilder) {
        String authHeaderSettingName = ThreadContext.PREFIX + "." + UsernamePasswordToken.BASIC_AUTH_HEADER;
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
        settingsBuilder.put(authHeaderSettingName, UsernamePasswordToken.basicAuthHeaderValue(username, new SecuredString(password
                .toCharArray())));
    }

    /*
     We inject additional settings on each tribe client if the current node is a tribe node, to make sure that every tribe has shield
     installed and enabled too:
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

            // we copy over existing mandatory plugins under additional settings, as they would get overridden otherwise (arrays don't get
            // merged)
            String[] existingMandatoryPlugins = tribeSettings.getValue().getAsArray("plugin.mandatory", null);
            if (existingMandatoryPlugins == null) {
                //shield is mandatory on every tribe if installed and enabled on the tribe node
                settingsBuilder.putArray(tribePrefix + "plugin.mandatory", NAME);
            } else {
                if (!isShieldMandatory(existingMandatoryPlugins)) {
                    throw new IllegalStateException("when [plugin.mandatory] is explicitly configured, [" + NAME + "] must be included in" +
                            " this list");
                }
            }

            final String tribeEnabledSetting = tribePrefix + ENABLED_SETTING_NAME;
            if (settings.get(tribeEnabledSetting) != null) {
                boolean enabled = shieldEnabled(tribeSettings.getValue());
                if (!enabled) {
                    throw new IllegalStateException("tribe setting [" + tribeEnabledSetting + "] must be set to true but the value is ["
                            + settings.get(tribeEnabledSetting) + "]");
                }
            } else {
                //shield must be enabled on every tribe if it's enabled on the tribe node
                settingsBuilder.put(tribeEnabledSetting, true);
            }
        }
    }

    /*
        We need to forcefully overwrite the query cache implementation to use Shield's opt out query cache implementation.
        This impl. disabled the query cache if field level security is used for a particular request. If we wouldn't do
        forcefully overwrite the query cache implementation then we leave the system vulnerable to leakages of data to
        unauthorized users.
     */
    private void addQueryCacheSettings(Settings.Builder settingsBuilder) {
        settingsBuilder.put(IndexModule.INDEX_QUERY_CACHE_TYPE_SETTING.getKey(), OPT_OUT_QUERY_CACHE);
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
        return env.configFile().resolve(XPackPlugin.NAME);
    }

    public static Path resolveConfigFile(Environment env, String name) {
        return configDir(env).resolve(name);
    }

    public static boolean clientMode(Settings settings) {
        return !"node".equals(settings.get(Client.CLIENT_TYPE_SETTING_S.getKey()));
    }

    public static boolean shieldEnabled(Settings settings) {
        return settings.getAsBoolean(ENABLED_SETTING_NAME, DEFAULT_ENABLED_SETTING);
    }

    public static boolean flsDlsEnabled(Settings settings) {
        return settings.getAsBoolean(DLS_FLS_ENABLED_SETTING, true);
    }

    private void failIfShieldQueryCacheIsNotActive(Settings settings, boolean nodeSettings) {
        String queryCacheImplementation;
        if (nodeSettings) {
            // in case this are node settings then the plugin additional settings have not been applied yet,
            // so we use 'opt_out_cache' as default. So in that case we only fail if the node settings contain
            // another cache impl than 'opt_out_cache'.
            queryCacheImplementation = settings.get(IndexModule.INDEX_QUERY_CACHE_TYPE_SETTING.getKey(), OPT_OUT_QUERY_CACHE);
        } else {
            queryCacheImplementation = settings.get(IndexModule.INDEX_QUERY_CACHE_TYPE_SETTING.getKey());
        }
        if (OPT_OUT_QUERY_CACHE.equals(queryCacheImplementation) == false) {
            throw new IllegalStateException("shield does not support a user specified query cache. remove the setting [" + IndexModule
                    .INDEX_QUERY_CACHE_TYPE_SETTING.getKey() + "] with value [" + queryCacheImplementation + "]");
        }
    }
}
