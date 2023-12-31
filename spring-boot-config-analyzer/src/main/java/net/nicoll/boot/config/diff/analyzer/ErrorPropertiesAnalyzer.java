/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.nicoll.boot.config.diff.analyzer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.nicoll.boot.config.loader.AetherDependencyResolver;
import net.nicoll.boot.config.loader.ConfigurationMetadataLoader;
import net.nicoll.boot.metadata.MetadataUtils;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.Deprecation.Level;

/**
 * Lists properties that are deprecated with error level.
 *
 * @author Stephane Nicoll
 */
public class ErrorPropertiesAnalyzer {

	public static void main(String[] args) throws Exception {
		ConfigurationMetadataLoader loader = new ConfigurationMetadataLoader(
				AetherDependencyResolver.withAllRepositories());
		ConfigurationMetadataRepository repo = loader.loadRepository("2.6.0-SNAPSHOT");
		List<ConfigurationMetadataGroup> groups = MetadataUtils.sortGroups(repo.getAllGroups().values());
		List<ConfigurationMetadataProperty> validProperties = new ArrayList<>();
		List<ConfigurationMetadataProperty> invalidProperties = new ArrayList<>();
		for (ConfigurationMetadataGroup group : groups) {
			List<ConfigurationMetadataProperty> properties = MetadataUtils
				.sortProperties(group.getProperties().values());
			for (ConfigurationMetadataProperty property : properties) {
				if (property.isDeprecated() && property.getDeprecation().getLevel() == Level.ERROR) {
					String replacement = property.getDeprecation().getReplacement();
					if (replacement != null && repo.getAllProperties().get(replacement) != null) {
						invalidProperties.add(property);
					}
					else {
						validProperties.add(property);
					}

				}
			}
		}
		validProperties.sort(Comparator.comparing(ConfigurationMetadataProperty::getId));
		invalidProperties.sort(Comparator.comparing(ConfigurationMetadataProperty::getId));
		StringBuilder message = new StringBuilder();
		message.append(String.format("Found %d deprecated properties with error level?%n",
				(validProperties.size() + invalidProperties.size())));
		message.append(String.format("\t%d Have an existing replacement%n", invalidProperties.size()));
		message.append(String.format("\t%d seems legit%n", validProperties.size()));

		if (!invalidProperties.isEmpty()) {
			message.append(String.format("%n%n"));
			message.append(String.format("Error properties with a replacement. Should they be flagged with error%n"));
			invalidProperties.forEach(e -> message
				.append(String.format("\t%s (replacement: %s)%n", e.getId(), e.getDeprecation().getReplacement())));
		}
		if (!validProperties.isEmpty()) {
			message.append(String.format("%n%n"));
			message.append(String.format("Error properties that should be double checked if they are still needed%n"));
			validProperties.forEach(e -> message.append(String.format("\t%s%n", e.getId())));
		}
		System.out.println(message);
	}

}
