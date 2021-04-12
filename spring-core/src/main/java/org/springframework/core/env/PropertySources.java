/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.env;

import org.springframework.lang.Nullable;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Holder containing one or more {@link PropertySource} objects.
 *
 * 容器，包含一个或多个属性源对象
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see PropertySource
 */
public interface PropertySources extends Iterable<PropertySource<?>> {

	/**
	 * Return a sequential {@link Stream} containing the property sources.
	 *
	 * 返回属性资源的流
	 * @since 5.1
	 */
	default Stream<PropertySource<?>> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	/**
	 * Return whether a property source with the given name is contained.
	 *
	 * 容器中是否包含某个属性
	 * @param name the {@linkplain PropertySource#getName() name of the property source} to find
	 */
	boolean contains(String name);

	/**
	 * Return the property source with the given name, {@code null} if not found.
	 *
	 * 根据给定的名称获取对应的属性资源
	 * @param name the {@linkplain PropertySource#getName() name of the property source} to find
	 */
	@Nullable
	PropertySource<?> get(String name);

}
