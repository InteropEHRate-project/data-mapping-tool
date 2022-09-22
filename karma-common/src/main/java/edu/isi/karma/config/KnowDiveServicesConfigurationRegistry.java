package edu.isi.karma.config;

import java.util.concurrent.ConcurrentHashMap;

/**
 * a registry class used to define or modify KnowDive microservices configuration settings for specific workspace.
 *
 * @author Danish Cheema
 * @date 24/01/2020
 */
public class KnowDiveServicesConfigurationRegistry {
	private static KnowDiveServicesConfigurationRegistry singleton = new KnowDiveServicesConfigurationRegistry();
	private final ConcurrentHashMap<String, KnowDiveServicesConfiguration> contextIdToKDConfiguration = new ConcurrentHashMap<>();

	public static KnowDiveServicesConfigurationRegistry getInstance() {
		return singleton;
	}

	public KnowDiveServicesConfiguration register(String contextId)
	{
		if(!contextIdToKDConfiguration.containsKey(contextId))
		{
			KnowDiveServicesConfiguration kdConfiguration = new KnowDiveServicesConfiguration(contextId);
			kdConfiguration.loadConfig();
			contextIdToKDConfiguration.putIfAbsent(contextId, kdConfiguration);
		}
		return contextIdToKDConfiguration.get(contextId);
	}

	public KnowDiveServicesConfiguration getKnowDiveConfiguration(String contextId) {
		return register(contextId);
	}

	public void deregister(String contextId) {
		contextIdToKDConfiguration.remove(contextId);
	}
}
