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
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * {@code EntityResolver} implementation that tries to resolve entity references
 * through a {@link org.springframework.core.io.ResourceLoader} (usually,
 * relative to the resource base of an {@code ApplicationContext}), if applicable.
 * Extends {@link DelegatingEntityResolver} to also provide DTD and XSD lookup.
 *
 * <p>Allows to use standard XML entities to include XML snippets into an
 * application context definition, for example to split a large XML file
 * into various modules. The include paths can be relative to the
 * application context's resource base as usual, instead of relative
 * to the JVM working directory (the XML parser's default).
 *
 * <p>Note: In addition to relative paths, every URL that specifies a
 * file in the current system root, i.e. the JVM working directory,
 * will be interpreted relative to the application context too.
 *
 * @author Juergen Hoeller
 * @since 31.07.2003
 * @see org.springframework.core.io.ResourceLoader
 * @see org.springframework.context.ApplicationContext
 */
public class ResourceEntityResolver extends DelegatingEntityResolver {

	// 设置日志信息
	private static final Log logger = LogFactory.getLog(ResourceEntityResolver.class);
	// 定义一个资源加载器
	private final ResourceLoader resourceLoader;


	/**
	 * Create a ResourceEntityResolver for the specified ResourceLoader
	 * (usually, an ApplicationContext).
	 *
	 * 从给定的 ResourceLoader(通常是一个 ApplicationContext) 中创建一个 ResourceEntityResolver
	 * @param resourceLoader the ResourceLoader (or ApplicationContext)
	 * to load XML entity includes with
	 */
	public ResourceEntityResolver(ResourceLoader resourceLoader) {
		super(resourceLoader.getClassLoader());
		this.resourceLoader = resourceLoader;
	}


	@Override
	@Nullable
	public InputSource resolveEntity(@Nullable String publicId, @Nullable String systemId)
			throws SAXException, IOException {
		// 从上述过程中获取 resolver
		InputSource source = super.resolveEntity(publicId, systemId);
		// 如果 source 为空
		if (source == null && systemId != null) {
			// 定义一个资源路径
			String resourcePath = null;
			try {
				// 编码 systemId
				String decodedSystemId = URLDecoder.decode(systemId, "UTF-8");
				// 获取给定的 url
				String givenUrl = new URL(decodedSystemId).toString();
				// 获取根 url
				String systemRootUrl = new File("").toURI().toURL().toString();
				// Try relative to resource base if currently in system root.
				// 如果给定的 url 是 systemRootUrl
				if (givenUrl.startsWith(systemRootUrl)) {
					resourcePath = givenUrl.substring(systemRootUrl.length());
				}
			}
			catch (Exception ex) {
				// Typically a MalformedURLException or AccessControlException.
				if (logger.isDebugEnabled()) {
					logger.debug("Could not resolve XML entity [" + systemId + "] against system root URL", ex);
				}
				// No URL (or no resolvable URL) -> try relative to resource base.
				// 获取资源路径
				resourcePath = systemId;
			}
			if (resourcePath != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Trying to locate XML entity [" + systemId + "] as resource [" + resourcePath + "]");
				}
				// 加载资源
				Resource resource = this.resourceLoader.getResource(resourcePath);
				// 获取一个 InputSource
				source = new InputSource(resource.getInputStream());
				// 获取一个 publiceId
				source.setPublicId(publicId);
				// 设置一个 systemId
				source.setSystemId(systemId);
				if (logger.isDebugEnabled()) {
					logger.debug("Found XML entity [" + systemId + "]: " + resource);
				}
			}
			// 如果结尾包含 DTD 或者 XSD
			else if (systemId.endsWith(DTD_SUFFIX) || systemId.endsWith(XSD_SUFFIX)) {
				// External dtd/xsd lookup via https even for canonical http declaration
				String url = systemId;
				// 将 http: 替换成 https
				if (url.startsWith("http:")) {
					url = "https:" + url.substring(5);
				}
				try {
					// 设置资源
					source = new InputSource(new URL(url).openStream());
					source.setPublicId(publicId);
					source.setSystemId(systemId);
				}
				catch (IOException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Could not resolve XML entity [" + systemId + "] through URL [" + url + "]", ex);
					}
					// Fall back to the parser's default behavior.
					source = null;
				}
			}
		}

		return source;
	}

}
