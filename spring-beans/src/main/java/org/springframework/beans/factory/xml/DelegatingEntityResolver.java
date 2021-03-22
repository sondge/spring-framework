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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * {@link EntityResolver} implementation that delegates to a {@link BeansDtdResolver}
 * and a {@link PluggableSchemaResolver} for DTDs and XML schemas, respectively.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @see BeansDtdResolver
 * @see PluggableSchemaResolver
 * @since 2.0
 */
public class DelegatingEntityResolver implements EntityResolver {

	/**
	 * Suffix for DTD files.
	 */
	// DTD 文件后缀
	public static final String DTD_SUFFIX = ".dtd";

	/**
	 * Suffix for schema definition files.
	 */
	// XSD 文件后缀
	public static final String XSD_SUFFIX = ".xsd";

	//定义一个不可变的  dtdResolver
	private final EntityResolver dtdResolver;

	// 定义一个不可变的 schemaResolver
	private final EntityResolver schemaResolver;


	/**
	 * Create a new DelegatingEntityResolver that delegates to
	 * a default {@link BeansDtdResolver} and a default {@link PluggableSchemaResolver}.
	 * <p>Configures the {@link PluggableSchemaResolver} with the supplied
	 * {@link ClassLoader}.
	 * <p>
	 * 从 ClassLoader 中创建一个 BeanDtdResolver 和 PluggableSchemaResolver
	 *
	 * @param classLoader the ClassLoader to use for loading
	 *                    (can be {@code null}) to use the default ClassLoader)
	 */
	public DelegatingEntityResolver(@Nullable ClassLoader classLoader) {
		this.dtdResolver = new BeansDtdResolver();
		this.schemaResolver = new PluggableSchemaResolver(classLoader);
	}

	/**
	 * Create a new DelegatingEntityResolver that delegates to
	 * the given {@link EntityResolver EntityResolvers}.
	 * <p>
	 * 传入 dtdResolver 以及 schemaResolver 中创建一个 DelegatingEntityResolver
	 *
	 * @param dtdResolver    the EntityResolver to resolve DTDs with
	 * @param schemaResolver the EntityResolver to resolve XML schemas with
	 */
	public DelegatingEntityResolver(EntityResolver dtdResolver, EntityResolver schemaResolver) {
		Assert.notNull(dtdResolver, "'dtdResolver' is required");
		Assert.notNull(schemaResolver, "'schemaResolver' is required");
		this.dtdResolver = dtdResolver;
		this.schemaResolver = schemaResolver;
	}


	@Override
	@Nullable
	public InputSource resolveEntity(@Nullable String publicId, @Nullable String systemId)
			throws SAXException, IOException {
		// 判断 systemId 不为空
		if (systemId != null) {
			// 如果是 DTD 后缀，采用 DTD 模式解析
			if (systemId.endsWith(DTD_SUFFIX)) {
				// 返回 dtdResolver 模式的 resolveEntity
				return this.dtdResolver.resolveEntity(publicId, systemId);
			} else if (systemId.endsWith(XSD_SUFFIX)) {
				// 否则采用 XSD 模式解析
				return this.schemaResolver.resolveEntity(publicId, systemId);
			}
		}

		// Fall back to the parser's default behavior.
		// 返回解析器的默认行为
		return null;
	}


	@Override
	// 重写 toString() 方法
	public String toString() {
		return "EntityResolver delegating " + XSD_SUFFIX + " to " + this.schemaResolver +
				" and " + DTD_SUFFIX + " to " + this.dtdResolver;
	}

}
