/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.beans;

import org.springframework.core.AttributeAccessorSupport;
import org.springframework.lang.Nullable;

/**
 * Extension of {@link org.springframework.core.AttributeAccessorSupport},
 * holding attributes as {@link BeanMetadataAttribute} objects in order
 * to keep track of the definition source.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
@SuppressWarnings("serial")
public class BeanMetadataAttributeAccessor extends AttributeAccessorSupport implements BeanMetadataElement {

	@Nullable
	// 定义元资源
	private Object source;


	/**
	 * Set the configuration source {@code Object} for this metadata element.
	 * <p>The exact type of the object will depend on the configuration mechanism used.
	 *  设置实例
	 */
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	@Override
	@Nullable
	// 获取实例
	public Object getSource() {
		return this.source;
	}


	/**
	 * Add the given BeanMetadataAttribute to this accessor's set of attributes.
	 * @param attribute the BeanMetadataAttribute object to register
	 * 将制定的属性放置 BeanMetadataAttribute 器中
	 */
	public void addMetadataAttribute(BeanMetadataAttribute attribute) {
		super.setAttribute(attribute.getName(), attribute);
	}

	/**
	 * Look up the given BeanMetadataAttribute in this accessor's set of attributes.
	 * @param name the name of the attribute
	 * @return the corresponding BeanMetadataAttribute object,
	 * or {@code null} if no such attribute defined
	 * 根据名称获取用户自定义的属性值
	 */
	@Nullable
	public BeanMetadataAttribute getMetadataAttribute(String name) {
		return (BeanMetadataAttribute) super.getAttribute(name);
	}

	@Override
	// 设置属性
	public void setAttribute(String name, @Nullable Object value) {
		super.setAttribute(name, new BeanMetadataAttribute(name, value));
	}

	@Override
	@Nullable
	// 根据属性名称获取属性值
	public Object getAttribute(String name) {
		BeanMetadataAttribute attribute = (BeanMetadataAttribute) super.getAttribute(name);
		return (attribute != null ? attribute.getValue() : null);
	}

	@Override
	@Nullable
	// 根据属性名称删除属性值
	public Object removeAttribute(String name) {
		BeanMetadataAttribute attribute = (BeanMetadataAttribute) super.removeAttribute(name);
		return (attribute != null ? attribute.getValue() : null);
	}

}
