/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link EntityResolver} implementation that attempts to resolve schema URLs into
 * local {@link ClassPathResource classpath resources} using a set of mappings files.
 * <p>
 * 设置映射文件的
 *
 * <p>By default, this class will look for mapping files in the classpath using the
 * pattern: {@code META-INF/spring.schemas} allowing for multiple files to exist on
 * the classpath at any one time.
 *
 * <p>The format of {@code META-INF/spring.schemas} is a properties file where each line
 * should be of the form {@code systemId=schema-location} where {@code schema-location}
 * should also be a schema file in the classpath. Since {@code systemId} is commonly a
 * URL, one must be careful to escape any ':' characters which are treated as delimiters
 * in properties files.
 *
 * <p>The pattern for the mapping files can be overridden using the
 * {@link #PluggableSchemaResolver(ClassLoader, String)} constructor.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class PluggableSchemaResolver implements EntityResolver {

	/**
	 * The location of the file that defines schema mappings.
	 * Can be present in multiple JAR files.
	 * <p>
	 * 默认的 schemas 位置
	 */
	public static final String DEFAULT_SCHEMA_MAPPINGS_LOCATION = "META-INF/spring.schemas";


	private static final Log logger = LogFactory.getLog(PluggableSchemaResolver.class);

	@Nullable
	// 定义一个 ClassLoader 处理器
	private final ClassLoader classLoader;
	// 处理一个 schemaMappingsLocation
	private final String schemaMappingsLocation;

	/**
	 * Stores the mapping of schema URL -> local schema path.
	 */
	@Nullable
	// 定义一个 schema 映射
	private volatile Map<String, String> schemaMappings;


	/**
	 * Loads the schema URL -> schema file location mappings using the default
	 * mapping file pattern "META-INF/spring.schemas".
	 *
	 * @param classLoader the ClassLoader to use for loading
	 *                    (can be {@code null}) to use the default ClassLoader)
	 * @see PropertiesLoaderUtils#loadAllProperties(String, ClassLoader)
	 * <p>
	 * 加载 Schema 的映射
	 */
	public PluggableSchemaResolver(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
		this.schemaMappingsLocation = DEFAULT_SCHEMA_MAPPINGS_LOCATION;
	}

	/**
	 * Loads the schema URL -> schema file location mappings using the given
	 * mapping file pattern.
	 *
	 * @param classLoader            the ClassLoader to use for loading
	 *                               (can be {@code null}) to use the default ClassLoader)
	 * @param schemaMappingsLocation the location of the file that defines schema mappings
	 *                               (must not be empty)
	 * @see PropertiesLoaderUtils#loadAllProperties(String, ClassLoader)
	 * <p>
	 * 从给定的映射加载 schemaUrl 和 类加载器构造一个 PluggableSchemaResolver
	 */
	public PluggableSchemaResolver(@Nullable ClassLoader classLoader, String schemaMappingsLocation) {
		Assert.hasText(schemaMappingsLocation, "'schemaMappingsLocation' must not be empty");
		this.classLoader = classLoader;
		this.schemaMappingsLocation = schemaMappingsLocation;
	}


	@Override
	@Nullable
	public InputSource resolveEntity(@Nullable String publicId, @Nullable String systemId) throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace("Trying to resolve XML entity with public id [" + publicId +
					"] and system id [" + systemId + "]");
		}

		if (systemId != null) {
			// 获取路径编号
			String resourceLocation = getSchemaMappings().get(systemId);
			// resource 并且 systemId 为 startswith 开头的
			if (resourceLocation == null && systemId.startsWith("https:")) {
				// Retrieve canonical http schema mapping even for https declaration
				// http: 替换成 https:
				resourceLocation = getSchemaMappings().get("http:" + systemId.substring(6));
			}
			if (resourceLocation != null) {
				// 创建一个新的 classPathResource
				Resource resource = new ClassPathResource(resourceLocation, this.classLoader);
				try {
					// 定义一个新的 source
					InputSource source = new InputSource(resource.getInputStream());
					// 设置 publicId 和 SystemId
					source.setPublicId(publicId);
					source.setSystemId(systemId);
					if (logger.isTraceEnabled()) {
						logger.trace("Found XML schema [" + systemId + "] in classpath: " + resourceLocation);
					}
					// 返回 source 文件流
					return source;
				} catch (FileNotFoundException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Could not find XML schema [" + systemId + "]: " + resource, ex);
					}
				}
			}
		}

		// Fall back to the parser's default behavior.
		return null;
	}

	/**
	 * Load the specified schema mappings lazily.
	 *
	 * 懒加载 schema mappings
	 */
	private Map<String, String> getSchemaMappings() {
		// 获取 map
		Map<String, String> schemaMappings = this.schemaMappings;
		// 如果为空
		if (schemaMappings == null) {
			synchronized (this) {
				// 重新获取 schemaMappings
				schemaMappings = this.schemaMappings;
				if (schemaMappings == null) {
					if (logger.isTraceEnabled()) {
						logger.trace("Loading schema mappings from [" + this.schemaMappingsLocation + "]");
					}
					try {
						// 定义一个新的属性，从 schemaMapping
						Properties mappings =
								PropertiesLoaderUtils.loadAllProperties(this.schemaMappingsLocation, this.classLoader);
						if (logger.isTraceEnabled()) {
							logger.trace("Loaded schema mappings: " + mappings);
						}
						schemaMappings = new ConcurrentHashMap<>(mappings.size());
						CollectionUtils.mergePropertiesIntoMap(mappings, schemaMappings);
						this.schemaMappings = schemaMappings;
					} catch (IOException ex) {
						throw new IllegalStateException(
								"Unable to load schema mappings from location [" + this.schemaMappingsLocation + "]", ex);
					}
				}
			}
		}
		return schemaMappings;
	}


	@Override
	public String toString() {
		return "EntityResolver using schema mappings " + getSchemaMappings();
	}

}
