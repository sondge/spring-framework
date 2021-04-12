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

import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;

/**
 * Interface that encapsulates configuration methods for a PropertyAccessor.
 * <p>
 * 封装 PropertyAccessor 的配置方法的接口。
 * Also extends the PropertyEditorRegistry interface, which defines methods for PropertyEditor management.
 * 还扩展了 PropertyEditorRegistry 接口，该接口定义了 PropertyEditor 管理的方法
 * <p>Serves as base interface for {@link BeanWrapper}.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see BeanWrapper
 * @since 2.0
 */
public interface ConfigurablePropertyAccessor extends PropertyAccessor, PropertyEditorRegistry, TypeConverter {

	/**
	 * Specify a Spring 3.0 ConversionService to use for converting
	 * property values, as an alternative to JavaBeans PropertyEditors.
	 * <p>
	 * 特别的  ConversionService 作为使用转化属性，作为一个选择的 JavaBeans PropertyEditors
	 */
	void setConversionService(@Nullable ConversionService conversionService);

	/**
	 * Return the associated ConversionService, if any.
	 * <p>
	 * 返回这个联系的 ConversionService
	 */
	@Nullable
	ConversionService getConversionService();

	/**
	 * Set whether to extract the old property value when applying a
	 * property editor to a new value for a property.
	 * <p>
	 * 设置是否提炼老的值从属性编译器向一个新的属性
	 */
	void setExtractOldValueForEditor(boolean extractOldValueForEditor);

	/**
	 * Return whether to extract the old property value when applying a
	 * property editor to a new value for a property.
	 * <p>
	 * 返回是否提炼老值对于属性编辑器
	 */
	boolean isExtractOldValueForEditor();

	/**
	 * Set whether this instance should attempt to "auto-grow" a
	 * nested path that contains a {@code null} value.
	 * <p>
	 * 设置是否自动解析嵌套路径
	 *
	 * <p>If {@code true}, a {@code null} path location will be populated
	 * with a default object value and traversed instead of resulting in a
	 * {@link NullValueInNestedPathException}.
	 * <p>Default is {@code false} on a plain PropertyAccessor instance.
	 */
	void setAutoGrowNestedPaths(boolean autoGrowNestedPaths);

	/**
	 * Return whether "auto-growing" of nested paths has been activated.
	 * <p>
	 * 返回是否自动解析嵌套路径
	 */
	boolean isAutoGrowNestedPaths();

}
