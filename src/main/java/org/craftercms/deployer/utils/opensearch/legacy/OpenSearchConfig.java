/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.craftercms.deployer.utils.opensearch.legacy;

import java.beans.ConstructorProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.lang3.ArrayUtils;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

/**
 * Holds the configuration for connecting to OpenSearch, either a single or multiple clusters
 *
 * @author joseross
 * @since 3.1.5
 */
public class OpenSearchConfig {

    public static final String CONFIG_KEY_GLOBAL_CLUSTER = "target.search.openSearch";

    public static final String CONFIG_KEY_READ_CLUSTER = CONFIG_KEY_GLOBAL_CLUSTER + ".readCluster";

    public static final String CONFIG_KEY_WRITE_CLUSTERS = CONFIG_KEY_GLOBAL_CLUSTER + ".writeClusters";

    public static final String CONFIG_KEY_LOCALE_MAPPING = CONFIG_KEY_GLOBAL_CLUSTER + ".locale.mapping";

    public static final String CONFIG_KEY_INDEX_SETTINGS = "target.search.openSearch.indexSettings";

    public static final String CONFIG_KEY_KEY = "key";

    public static final String CONFIG_KEY_VALUE = "value";

    /**
     * The global cluster, used for connecting to a single cluster for read &amp; write operations
     */
    public final OpenSearchClusterConfig globalCluster;

    /**
     * The read cluster, used for connecting to multiple clusters
     */
    public final OpenSearchClusterConfig readCluster;

    /**
     * The write clusters, used for connecting to multiple clusters
     */
    public final List<OpenSearchClusterConfig> writeClusters;

    /**
     * Mapping of locale codes to OpenSearch language analyzers
     */
    public final Map<String, String> localeMapping = new HashMap<>();

    public final Map<String, String> indexSettings;

    @ConstructorProperties({"config"})
    public OpenSearchConfig(HierarchicalConfiguration<?> config) {
        if (!isEmpty(config.childConfigurationsAt(CONFIG_KEY_GLOBAL_CLUSTER))) {
            globalCluster = new OpenSearchClusterConfig(config.configurationAt(CONFIG_KEY_GLOBAL_CLUSTER));
        } else {
            globalCluster = new OpenSearchClusterConfig();
        }
        if (!isEmpty(config.childConfigurationsAt(CONFIG_KEY_READ_CLUSTER))) {
            readCluster = new OpenSearchClusterConfig(config.configurationAt(CONFIG_KEY_READ_CLUSTER),
                globalCluster.username, globalCluster.password, globalCluster.connectTimeout,
                    globalCluster.socketTimeout, globalCluster.threadCount, globalCluster.keepAlive);
        } else {
            readCluster = new OpenSearchClusterConfig();
        }
        writeClusters = config.configurationsAt(CONFIG_KEY_WRITE_CLUSTERS)
            .stream()
            .map(cluster -> new OpenSearchClusterConfig(cluster, globalCluster.username, globalCluster.password,
                    globalCluster.connectTimeout, globalCluster.socketTimeout, globalCluster.threadCount,
                    globalCluster.keepAlive))
            .collect(toList());
        if (useSingleCluster() && ArrayUtils.isEmpty(globalCluster.urls)) {
            throw new IllegalStateException("Invalid OpenSearch configuration");
        }

        Configuration mapping = config.configurationAt(CONFIG_KEY_LOCALE_MAPPING);
        mapping.getKeys().forEachRemaining(key -> localeMapping.put(key, mapping.getString(key)));

        indexSettings = new HashMap<>();
        config.configurationsAt(CONFIG_KEY_INDEX_SETTINGS).forEach(settingConfig ->
                indexSettings.put(settingConfig.getString(CONFIG_KEY_KEY), settingConfig.getString(CONFIG_KEY_VALUE)));
    }

    /**
     * Indicates if a single cluster should be used, only if any of the read or write clusters is missing
     */
    public boolean useSingleCluster() {
        return ArrayUtils.isEmpty(readCluster.urls) || isEmpty(writeClusters);
    }

    public Map<String, String> getLocaleMapping() {
        return localeMapping;
    }

}
