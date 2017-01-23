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

package org.elasticsearch.xpack.watcher;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.NamedDiff;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.Booleans;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.common.logging.LoggerMessageFormat;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.ScriptSettings;
import org.elasticsearch.threadpool.ExecutorBuilder;
import org.elasticsearch.threadpool.FixedExecutorBuilder;
import org.elasticsearch.xpack.XPackPlugin;
import org.elasticsearch.xpack.XPackSettings;
import org.elasticsearch.xpack.common.http.HttpClient;
import org.elasticsearch.xpack.common.http.HttpRequestTemplate;
import org.elasticsearch.xpack.common.text.TextTemplateEngine;
import org.elasticsearch.xpack.notification.email.EmailService;
import org.elasticsearch.xpack.notification.email.attachment.EmailAttachmentsParser;
import org.elasticsearch.xpack.notification.hipchat.HipChatService;
import org.elasticsearch.xpack.notification.jira.JiraService;
import org.elasticsearch.xpack.notification.pagerduty.PagerDutyService;
import org.elasticsearch.xpack.notification.slack.SlackService;
import org.elasticsearch.xpack.security.InternalClient;
import org.elasticsearch.xpack.support.clock.Clock;
import org.elasticsearch.xpack.watcher.actions.ActionFactory;
import org.elasticsearch.xpack.watcher.actions.ActionRegistry;
import org.elasticsearch.xpack.watcher.actions.email.EmailAction;
import org.elasticsearch.xpack.watcher.actions.email.EmailActionFactory;
import org.elasticsearch.xpack.watcher.actions.hipchat.HipChatAction;
import org.elasticsearch.xpack.watcher.actions.hipchat.HipChatActionFactory;
import org.elasticsearch.xpack.watcher.actions.index.IndexAction;
import org.elasticsearch.xpack.watcher.actions.index.IndexActionFactory;
import org.elasticsearch.xpack.watcher.actions.jira.JiraAction;
import org.elasticsearch.xpack.watcher.actions.jira.JiraActionFactory;
import org.elasticsearch.xpack.watcher.actions.logging.LoggingAction;
import org.elasticsearch.xpack.watcher.actions.logging.LoggingActionFactory;
import org.elasticsearch.xpack.watcher.actions.pagerduty.PagerDutyAction;
import org.elasticsearch.xpack.watcher.actions.pagerduty.PagerDutyActionFactory;
import org.elasticsearch.xpack.watcher.actions.slack.SlackAction;
import org.elasticsearch.xpack.watcher.actions.slack.SlackActionFactory;
import org.elasticsearch.xpack.watcher.actions.webhook.WebhookAction;
import org.elasticsearch.xpack.watcher.actions.webhook.WebhookActionFactory;
import org.elasticsearch.xpack.watcher.client.WatcherClientModule;
import org.elasticsearch.xpack.watcher.condition.AlwaysCondition;
import org.elasticsearch.xpack.watcher.condition.ArrayCompareCondition;
import org.elasticsearch.xpack.watcher.condition.CompareCondition;
import org.elasticsearch.xpack.watcher.condition.ConditionFactory;
import org.elasticsearch.xpack.watcher.condition.ConditionRegistry;
import org.elasticsearch.xpack.watcher.condition.NeverCondition;
import org.elasticsearch.xpack.watcher.condition.ScriptCondition;
import org.elasticsearch.xpack.watcher.execution.ExecutionModule;
import org.elasticsearch.xpack.watcher.execution.ExecutionService;
import org.elasticsearch.xpack.watcher.execution.InternalWatchExecutor;
import org.elasticsearch.xpack.watcher.execution.TriggeredWatchStore;
import org.elasticsearch.xpack.watcher.history.HistoryModule;
import org.elasticsearch.xpack.watcher.history.HistoryStore;
import org.elasticsearch.xpack.watcher.input.InputModule;
import org.elasticsearch.xpack.watcher.rest.action.RestAckWatchAction;
import org.elasticsearch.xpack.watcher.rest.action.RestActivateWatchAction;
import org.elasticsearch.xpack.watcher.rest.action.RestDeleteWatchAction;
import org.elasticsearch.xpack.watcher.rest.action.RestExecuteWatchAction;
import org.elasticsearch.xpack.watcher.rest.action.RestGetWatchAction;
import org.elasticsearch.xpack.watcher.rest.action.RestHijackOperationAction;
import org.elasticsearch.xpack.watcher.rest.action.RestPutWatchAction;
import org.elasticsearch.xpack.watcher.rest.action.RestWatchServiceAction;
import org.elasticsearch.xpack.watcher.rest.action.RestWatcherStatsAction;
import org.elasticsearch.xpack.watcher.support.WatcherIndexTemplateRegistry;
import org.elasticsearch.xpack.watcher.support.WatcherIndexTemplateRegistry.TemplateConfig;
import org.elasticsearch.xpack.watcher.transform.TransformFactory;
import org.elasticsearch.xpack.watcher.transform.TransformRegistry;
import org.elasticsearch.xpack.watcher.transform.script.ScriptTransform;
import org.elasticsearch.xpack.watcher.transform.script.ScriptTransformFactory;
import org.elasticsearch.xpack.watcher.transform.search.SearchTransform;
import org.elasticsearch.xpack.watcher.transform.search.SearchTransformFactory;
import org.elasticsearch.xpack.watcher.transport.actions.ack.AckWatchAction;
import org.elasticsearch.xpack.watcher.transport.actions.ack.TransportAckWatchAction;
import org.elasticsearch.xpack.watcher.transport.actions.activate.ActivateWatchAction;
import org.elasticsearch.xpack.watcher.transport.actions.activate.TransportActivateWatchAction;
import org.elasticsearch.xpack.watcher.transport.actions.delete.DeleteWatchAction;
import org.elasticsearch.xpack.watcher.transport.actions.delete.TransportDeleteWatchAction;
import org.elasticsearch.xpack.watcher.transport.actions.execute.ExecuteWatchAction;
import org.elasticsearch.xpack.watcher.transport.actions.execute.TransportExecuteWatchAction;
import org.elasticsearch.xpack.watcher.transport.actions.get.GetWatchAction;
import org.elasticsearch.xpack.watcher.transport.actions.get.TransportGetWatchAction;
import org.elasticsearch.xpack.watcher.transport.actions.put.PutWatchAction;
import org.elasticsearch.xpack.watcher.transport.actions.put.TransportPutWatchAction;
import org.elasticsearch.xpack.watcher.transport.actions.service.TransportWatcherServiceAction;
import org.elasticsearch.xpack.watcher.transport.actions.service.WatcherServiceAction;
import org.elasticsearch.xpack.watcher.transport.actions.stats.TransportWatcherStatsAction;
import org.elasticsearch.xpack.watcher.transport.actions.stats.WatcherStatsAction;
import org.elasticsearch.xpack.watcher.trigger.TriggerModule;
import org.elasticsearch.xpack.watcher.trigger.schedule.ScheduleModule;
import org.elasticsearch.xpack.watcher.watch.WatchModule;
import org.elasticsearch.xpack.watcher.watch.WatchStore;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public class Watcher implements ActionPlugin, ScriptPlugin {

    public static final Setting<String> INDEX_WATCHER_VERSION_SETTING =
            new Setting<>("index.xpack.watcher.plugin.version", "", Function.identity(), Setting.Property.IndexScope);
    public static final Setting<String> INDEX_WATCHER_TEMPLATE_VERSION_SETTING =
            new Setting<>("index.xpack.watcher.template.version", "", Function.identity(), Setting.Property.IndexScope);
    public static final Setting<Boolean> ENCRYPT_SENSITIVE_DATA_SETTING =
            Setting.boolSetting("xpack.watcher.encrypt_sensitive_data", false, Setting.Property.NodeScope);
    public static final Setting<TimeValue> MAX_STOP_TIMEOUT_SETTING =
        Setting.timeSetting("xpack.watcher.stop.timeout", TimeValue.timeValueSeconds(30), Setting.Property.NodeScope);
    private static final String SETTING_KEY_AUTO_CREATE_INDEX = "action.auto_create_index";

    private static final ScriptContext.Plugin SCRIPT_PLUGIN = new ScriptContext.Plugin("xpack", "watch");
    public static final ScriptContext SCRIPT_CONTEXT = SCRIPT_PLUGIN::getKey;

    private static final Logger logger = Loggers.getLogger(XPackPlugin.class);

    private static final DeprecationLogger DEPRECATION_LOGGER = new DeprecationLogger(logger);

    public List<NamedWriteableRegistry.Entry> getNamedWriteables() {
        List<NamedWriteableRegistry.Entry> entries = new ArrayList<>();
        entries.add(new NamedWriteableRegistry.Entry(MetaData.Custom.class, WatcherMetaData.TYPE, WatcherMetaData::new));
        entries.add(new NamedWriteableRegistry.Entry(NamedDiff.class, WatcherMetaData.TYPE, WatcherMetaData::readDiffFrom));
        return entries;
    }

    public List<NamedXContentRegistry.Entry> getNamedXContent() {
        List<NamedXContentRegistry.Entry> entries = new ArrayList<>();
        // Metadata
        entries.add(new NamedXContentRegistry.Entry(MetaData.Custom.class, new ParseField(WatcherMetaData.TYPE),
                WatcherMetaData::fromXContent));
        return entries;
    }

    protected final Settings settings;
    protected final boolean transportClient;
    protected final boolean enabled;

    public Watcher(Settings settings) {
        this.settings = settings;
        transportClient = "transport".equals(settings.get(Client.CLIENT_TYPE_SETTING_S.getKey()));
        this.enabled = XPackSettings.WATCHER_ENABLED.get(settings);
        validAutoCreateIndex(settings);
    }

    public Collection<Object> createComponents(Clock clock, ScriptService scriptService, InternalClient internalClient,
                                               XPackLicenseState licenseState,
                                               HttpClient httpClient, NamedXContentRegistry xContentRegistry,
                                               Collection<Object> components) {
        final Map<String, ConditionFactory> parsers = new HashMap<>();
        parsers.put(AlwaysCondition.TYPE, (c, id, p, upgrade) -> AlwaysCondition.parse(id, p));
        parsers.put(NeverCondition.TYPE, (c, id, p, upgrade) -> NeverCondition.parse(id, p));
        parsers.put(ArrayCompareCondition.TYPE, (c, id, p, upgrade) -> ArrayCompareCondition.parse(c, id, p));
        parsers.put(CompareCondition.TYPE, (c, id, p, upgrade) -> CompareCondition.parse(c, id, p));
        String defaultLegacyScriptLanguage = ScriptSettings.getLegacyDefaultLang(settings);
        parsers.put(ScriptCondition.TYPE, (c, id, p, upgrade) -> ScriptCondition.parse(scriptService, id, p, upgrade,
                defaultLegacyScriptLanguage));

        final ConditionRegistry conditionRegistry = new ConditionRegistry(Collections.unmodifiableMap(parsers), clock);
        final Map<String, TransformFactory> transformFactories = new HashMap<>();
        transformFactories.put(ScriptTransform.TYPE, new ScriptTransformFactory(settings, scriptService));
        transformFactories.put(SearchTransform.TYPE, new SearchTransformFactory(settings, internalClient, xContentRegistry, scriptService));
        final TransformRegistry transformRegistry = new TransformRegistry(settings, Collections.unmodifiableMap(transformFactories));

        final Map<String, ActionFactory> actionFactoryMap = new HashMap<>();
        TextTemplateEngine templateEngine = getService(TextTemplateEngine.class, components);
        actionFactoryMap.put(EmailAction.TYPE, new EmailActionFactory(settings, getService(EmailService.class, components), templateEngine,
                getService(EmailAttachmentsParser.class, components)));
        actionFactoryMap.put(WebhookAction.TYPE, new WebhookActionFactory(settings, httpClient,
                getService(HttpRequestTemplate.Parser.class, components), templateEngine));
        actionFactoryMap.put(IndexAction.TYPE, new IndexActionFactory(settings, internalClient));
        actionFactoryMap.put(LoggingAction.TYPE, new LoggingActionFactory(settings, templateEngine));
        actionFactoryMap.put(HipChatAction.TYPE, new HipChatActionFactory(settings, templateEngine,
                getService(HipChatService.class, components)));
        actionFactoryMap.put(JiraAction.TYPE, new JiraActionFactory(settings, templateEngine,
                getService(JiraService.class, components)));
        actionFactoryMap.put(SlackAction.TYPE, new SlackActionFactory(settings, templateEngine,
                getService(SlackService.class, components)));
        actionFactoryMap.put(PagerDutyAction.TYPE, new PagerDutyActionFactory(settings, templateEngine,
                getService(PagerDutyService.class, components)));
        final ActionRegistry registry = new ActionRegistry(actionFactoryMap, conditionRegistry, transformRegistry, clock, licenseState);

        return Collections.singleton(registry);
    }

    private <T> T getService(Class<T> serviceClass, Collection<Object> services) {
        List<Object> collect = services.stream().filter(o -> o.getClass() == serviceClass).collect(Collectors.toList());
        if (collect.isEmpty()) {
            throw new IllegalArgumentException("no service for class " + serviceClass.getName());
        } else if (collect.size() > 1) {
            throw new IllegalArgumentException("more than one service for class " + serviceClass.getName());
        }
        return (T) collect.get(0);
    }

    public Collection<Module> nodeModules() {
        List<Module> modules = new ArrayList<>();
        modules.add(new WatcherModule(enabled, transportClient));
        if (enabled && transportClient == false) {
            modules.add(new WatchModule());
            modules.add(new WatcherClientModule());
            modules.add(new TriggerModule(settings));
            modules.add(new ScheduleModule());
            modules.add(new InputModule());
            modules.add(new HistoryModule());
            modules.add(new ExecutionModule());
        }
        return modules;
    }

    public Settings additionalSettings() {
        return Settings.EMPTY;
    }


    public List<Setting<?>> getSettings() {
        List<Setting<?>> settings = new ArrayList<>();
        for (TemplateConfig templateConfig : WatcherIndexTemplateRegistry.TEMPLATE_CONFIGS) {
            settings.add(templateConfig.getSetting());
        }
        settings.add(INDEX_WATCHER_VERSION_SETTING);
        settings.add(INDEX_WATCHER_TEMPLATE_VERSION_SETTING);
        settings.add(MAX_STOP_TIMEOUT_SETTING);
        settings.add(ExecutionService.DEFAULT_THROTTLE_PERIOD_SETTING);
        settings.add(Setting.intSetting("xpack.watcher.execution.scroll.size", 0, Setting.Property.NodeScope));
        settings.add(Setting.intSetting("xpack.watcher.watch.scroll.size", 0, Setting.Property.NodeScope));
        settings.add(ENCRYPT_SENSITIVE_DATA_SETTING);

        settings.add(Setting.simpleString("xpack.watcher.internal.ops.search.default_timeout", Setting.Property.NodeScope));
        settings.add(Setting.simpleString("xpack.watcher.internal.ops.bulk.default_timeout", Setting.Property.NodeScope));
        settings.add(Setting.simpleString("xpack.watcher.internal.ops.index.default_timeout", Setting.Property.NodeScope));
        settings.add(Setting.simpleString("xpack.watcher.actions.index.default_timeout", Setting.Property.NodeScope));
        settings.add(Setting.simpleString("xpack.watcher.index.rest.direct_access", Setting.Property.NodeScope));
        settings.add(Setting.simpleString("xpack.watcher.trigger.schedule.engine", Setting.Property.NodeScope));
        settings.add(Setting.simpleString("xpack.watcher.input.search.default_timeout", Setting.Property.NodeScope));
        settings.add(Setting.simpleString("xpack.watcher.transform.search.default_timeout", Setting.Property.NodeScope));
        settings.add(Setting.simpleString("xpack.watcher.trigger.schedule.ticker.tick_interval", Setting.Property.NodeScope));
        settings.add(Setting.simpleString("xpack.watcher.execution.scroll.timeout", Setting.Property.NodeScope));
        settings.add(Setting.simpleString("xpack.watcher.start_immediately", Setting.Property.NodeScope));

        return settings;
    }

    public List<ExecutorBuilder<?>> getExecutorBuilders(final Settings settings) {
        if (enabled) {
            final FixedExecutorBuilder builder =
                    new FixedExecutorBuilder(
                            settings,
                            InternalWatchExecutor.THREAD_POOL_NAME,
                            5 * EsExecutors.boundedNumberOfProcessors(settings),
                            1000,
                            "xpack.watcher.thread_pool");
            return Collections.singletonList(builder);
        }
        return Collections.emptyList();
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        if (false == enabled) {
            return emptyList();
        }
        return Arrays.asList(new ActionHandler<>(PutWatchAction.INSTANCE, TransportPutWatchAction.class),
                new ActionHandler<>(DeleteWatchAction.INSTANCE, TransportDeleteWatchAction.class),
                new ActionHandler<>(GetWatchAction.INSTANCE, TransportGetWatchAction.class),
                new ActionHandler<>(WatcherStatsAction.INSTANCE, TransportWatcherStatsAction.class),
                new ActionHandler<>(AckWatchAction.INSTANCE, TransportAckWatchAction.class),
                new ActionHandler<>(ActivateWatchAction.INSTANCE, TransportActivateWatchAction.class),
                new ActionHandler<>(WatcherServiceAction.INSTANCE, TransportWatcherServiceAction.class),
                new ActionHandler<>(ExecuteWatchAction.INSTANCE, TransportExecuteWatchAction.class));
    }

    @Override
    public List<RestHandler> getRestHandlers(Settings settings, RestController restController, ClusterSettings clusterSettings,
            IndexScopedSettings indexScopedSettings, SettingsFilter settingsFilter, IndexNameExpressionResolver indexNameExpressionResolver,
            Supplier<DiscoveryNodes> nodesInCluster) {
        if (false == enabled) {
            return emptyList();
        }
        return Arrays.asList(
                new RestPutWatchAction(settings, restController),
                new RestDeleteWatchAction(settings, restController),
                new RestWatcherStatsAction(settings, restController),
                new RestGetWatchAction(settings, restController),
                new RestWatchServiceAction(settings, restController),
                new RestAckWatchAction(settings, restController),
                new RestActivateWatchAction(settings, restController),
                new RestExecuteWatchAction(settings, restController),
                new RestHijackOperationAction(settings, restController));
    }

    @Override
    public ScriptContext.Plugin getCustomScriptContexts() {
        return SCRIPT_PLUGIN;
    }

    static void validAutoCreateIndex(Settings settings) {
        String value = settings.get(SETTING_KEY_AUTO_CREATE_INDEX);
        if (value == null) {
            return;
        }

        String errorMessage = LoggerMessageFormat.format("the [{}] setting value [{}] is too restrictive. disable [{}] or " +
                "set it to [{}, {}, {}*]", (Object) SETTING_KEY_AUTO_CREATE_INDEX, value, SETTING_KEY_AUTO_CREATE_INDEX, WatchStore.INDEX,
                TriggeredWatchStore.INDEX_NAME, HistoryStore.INDEX_PREFIX);
        if (Booleans.isExplicitFalse(value)) {
            if (Booleans.isStrictlyBoolean(value) == false) {
                DEPRECATION_LOGGER.deprecated("Expected [false] for setting [{}] but got [{}]", SETTING_KEY_AUTO_CREATE_INDEX, value);
            }
            throw new IllegalArgumentException(errorMessage);
        }

        if (Booleans.isExplicitTrue(value)) {
            if (Booleans.isStrictlyBoolean(value) == false) {
                DEPRECATION_LOGGER.deprecated("Expected [true] for setting [{}] but got [{}]", SETTING_KEY_AUTO_CREATE_INDEX, value);
            }
            return;
        }

        String[] matches = Strings.commaDelimitedListToStringArray(value);
        List<String> indices = new ArrayList<>();
        indices.add(".watches");
        indices.add(".triggered_watches");
        DateTime now = new DateTime(DateTimeZone.UTC);
        indices.add(HistoryStore.getHistoryIndexNameForTime(now));
        indices.add(HistoryStore.getHistoryIndexNameForTime(now.plusDays(1)));
        indices.add(HistoryStore.getHistoryIndexNameForTime(now.plusMonths(1)));
        indices.add(HistoryStore.getHistoryIndexNameForTime(now.plusMonths(2)));
        indices.add(HistoryStore.getHistoryIndexNameForTime(now.plusMonths(3)));
        indices.add(HistoryStore.getHistoryIndexNameForTime(now.plusMonths(4)));
        indices.add(HistoryStore.getHistoryIndexNameForTime(now.plusMonths(5)));
        indices.add(HistoryStore.getHistoryIndexNameForTime(now.plusMonths(6)));
        for (String index : indices) {
            boolean matched = false;
            for (String match : matches) {
                char c = match.charAt(0);
                if (c == '-') {
                    if (Regex.simpleMatch(match.substring(1), index)) {
                        throw new IllegalArgumentException(errorMessage);
                    }
                } else if (c == '+') {
                    if (Regex.simpleMatch(match.substring(1), index)) {
                        matched = true;
                        break;
                    }
                } else {
                    if (Regex.simpleMatch(match, index)) {
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched) {
                throw new IllegalArgumentException(errorMessage);
            }
        }
        logger.warn("the [action.auto_create_index] setting is configured to be restrictive [{}]. " +
                " for the next 6 months daily history indices are allowed to be created, but please make sure" +
                " that any future history indices after 6 months with the pattern " +
                "[.watcher-history-YYYY.MM.dd] are allowed to be created", value);
    }
}
