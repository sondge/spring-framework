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

package org.springframework.web.servlet.mvc.method;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy;

/**
 * A {@link org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy
 * HandlerMethodMappingNamingStrategy} for {@code RequestMappingInfo}-based handler
 * method mappings.
 * <p>
 * 针对 RequestMapping 的方法映射
 * <p>
 * If the {@code RequestMappingInfo} name attribute is set, its value is used.
 * Otherwise the name is based on the capital letters of the class name,
 * followed by "#" as a separator, and the method name. For example "TC#getFoo"
 * for a class named TestController with method getFoo.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class RequestMappingInfoHandlerMethodMappingNamingStrategy
		implements HandlerMethodMappingNamingStrategy<RequestMappingInfo> {

	/** Separator between the type and method-level parts of a HandlerMethod mapping name. */

	/**
	 * HandlerMethod映射名称的类型和方法级别部分之间的分隔符。
	 */
	public static final String SEPARATOR = "#";


	@Override
	public String getName(HandlerMethod handlerMethod, RequestMappingInfo mapping) {
		// 情况一：如果映射名称不为空，则使用 mapping 的名称
		if (mapping.getName() != null) {
			return mapping.getName();
		}
		// 构建 StringBuilder 对象的名称
		StringBuilder sb = new StringBuilder();
		// 获取简单的类型名称
		String simpleTypeName = handlerMethod.getBeanType().getSimpleName();
		for (int i = 0; i < simpleTypeName.length(); i++) {
			// 如果是大写的名称
			if (Character.isUpperCase(simpleTypeName.charAt(i))) {
				// 加入 append
				sb.append(simpleTypeName.charAt(i));
			}
		}
		// 加入方法名称
		sb.append(SEPARATOR).append(handlerMethod.getMethod().getName());
		return sb.toString();
	}

}
