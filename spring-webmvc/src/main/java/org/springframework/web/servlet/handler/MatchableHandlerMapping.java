/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet.handler;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * Additional interface that a {@link HandlerMapping} can implement to expose
 * a request matching API aligned with its internal request matching
 * configuration and implementation.
 * <p>
 * 额外的接口目的是 HandlerMapping 可以实现暴露一个请求匹配 API与 他的额外的请求匹配配置和实现
 *
 * @author Rossen Stoyanchev
 * @see HandlerMappingIntrospector
 * @since 4.3.1
 */
public interface MatchableHandlerMapping extends HandlerMapping {

	/**
	 * Determine whether the given request matches the request criteria.
	 * <p>
	 * 表示是否根据给定的请求匹配这个请求标准
	 *
	 * @param request the current request
	 * @param pattern the pattern to match
	 * @return the result from request matching, or {@code null} if none
	 */
	@Nullable
	RequestMatchResult match(HttpServletRequest request, String pattern);

}
