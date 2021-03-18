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

package org.springframework.core;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Support class for {@link AttributeAccessor AttributeAccessors}, providing
 * a base implementation of all methods. To be extended by subclasses.
 *
 * <p>{@link Serializable} if subclasses and all attribute values are {@link Serializable}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
// 属性存储器
public abstract class AttributeAccessorSupport implements AttributeAccessor, Serializable {

	/** Map with String keys and Object values. */
	private final Map<String, Object> attributes = new LinkedHashMap<>();


	@Override
	// 设置属性名称和属性值
	public void setAttribute(String name, @Nullable Object value) {
		Assert.notNull(name, "Name must not be null");
		if (value != null) {
			this.attributes.put(name, value);
		}
		else {
			removeAttribute(name);
		}
	}

	@Override
	@Nullable
	// 根据属性名获取属性
	public Object getAttribute(String name) {
		Assert.notNull(name, "Name must not be null");
		return this.attributes.get(name);
	}

	@Override
	@Nullable
	// 删除属性
	public Object removeAttribute(String name) {
		Assert.notNull(name, "Name must not be null");
		return this.attributes.remove(name);
	}

	@Override
	// 是否有属性
	public boolean hasAttribute(String name) {
		Assert.notNull(name, "Name must not be null");
		return this.attributes.containsKey(name);
	}

	@Override
	// 获取全部的属性名称数组
	public String[] attributeNames() {
		return StringUtils.toStringArray(this.attributes.keySet());
	}


	/**
	 * Copy the attributes from the supplied AttributeAccessor to this accessor.
	 * @param source the AttributeAccessor to copy from
	 * 从属性中拷贝属性
	 */
	protected void copyAttributesFrom(AttributeAccessor source) {
		Assert.notNull(source, "Source must not be null");
		String[] attributeNames = source.attributeNames();
		for (String attributeName : attributeNames) {
			setAttribute(attributeName, source.getAttribute(attributeName));
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof AttributeAccessorSupport &&
				this.attributes.equals(((AttributeAccessorSupport) other).attributes)));
	}

	@Override
	public int hashCode() {
		return this.attributes.hashCode();
	}

}
