package org.kafka;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GithubSourceConnector extends SourceConnector {
    private static Logger log = LoggerFactory.getLogger(GithubSourceConnector.class);
    private GitHubSourceConnectorConfig config;
    @Override
    public void start(Map<String, String> map) {
        config = new GitHubSourceConnectorConfig(map);
    }

    @Override
    public Class<? extends Task> taskClass() {
        return null;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int i) {
        // Define the individual task configurations that will be executed.
        // for multiple tasks, add configs to arraylist
        ArrayList<Map<String, String>> configs = new ArrayList<>(1);
        configs.add(config.originalsStrings());
        return configs;
    }

    @Override
    public void stop() {
        // Do things that are necessary to stop your connector like closing connection.
    }

    @Override
    public ConfigDef config() {
        return GitHubSourceConnectorConfig.conf();
    }

    @Override
    public String version() {
        return VersionUtil.getVersion();
    }
}
