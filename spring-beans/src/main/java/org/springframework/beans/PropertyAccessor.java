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

package org.springframework.beans;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * Common interface for classes that can access named properties
 * (such as bean properties of an object or fields in an object)
 * Serves as base interface for {@link BeanWrapper}.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see BeanWrapper
 * @see PropertyAccessorFactory#forBeanPropertyAccess
 * @see PropertyAccessorFactory#forDirectFieldAccess
 */
public interface PropertyAccessor {

	/**
	 * Path separator for nested properties.
	 * 嵌套属性的路径分隔符
	 * Follows normal Java conventions: getFoo().getBar() would be "foo.bar".
	 */
	String NESTED_PROPERTY_SEPARATOR = ".";

	/**
	 * Path separator for nested properties.
	 *
	 * 嵌套属性的分隔符
	 * Follows normal Java conventions: getFoo().getBar() would be "foo.bar".
	 */
	char NESTED_PROPERTY_SEPARATOR_CHAR = '.';

	/**
	 * Marker that indicates the start of a property key for an
	 * indexed or mapped property like "person.addresses[0]".
	 * 以索引或者已映射的属性的的开始标记
	 */
	String PROPERTY_KEY_PREFIX = "[";

	/**
	 * Marker that indicates the start of a property key for an
	 * indexed or mapped property like "person.addresses[0]".
	 *
	 * 以索引或者已映射的属性的的开始标记
	 */
	char PROPERTY_KEY_PREFIX_CHAR = '[';

	/**
	 * Marker that indicates the end of a property key for an
	 * indexed or mapped property like "person.addresses[0]".
	 *
	 * 以索引或者已映射的属性的的结束标记
	 */
	String PROPERTY_KEY_SUFFIX = "]";

	/**
	 * Marker that indicates the end of a property key for an
	 * indexed or mapped property like "person.addresses[0]".
	 *
	 * 以索引或者已映射的属性的的开始标记
	 */
	char PROPERTY_KEY_SUFFIX_CHAR = ']';


	/**
	 * Determine whether the specified property is readable.
	 *
	 * 表示给定的属性是否是可读属性
	 * <p>Returns {@code false} if the property doesn't exist.
	 * @param propertyName the property to check
	 * (may be a nested path and/or an indexed/mapped property)
	 * @return whether the property is readable
	 */
	boolean isReadableProperty(String propertyName);

	/**
	 * Determine whether the specified property is writable.
	 *
	 * 表示对应的属性是否是可写属性
	 * <p>Returns {@code false} if the property doesn't exist.
	 * @param propertyName the property to check
	 * (may be a nested path and/or an indexed/mapped property)
	 * @return whether the property is writable
	 */
	boolean isWritableProperty(String propertyName);

	/**
	 * Determine the property type for the specified property,
	 * either checking the property descriptor or checking the value
	 * in case of an indexed or mapped element.
	 *
	 * 根据属性名称获取属性的类型
	 * @param propertyName the property to check
	 * (may be a nested path and/or an indexed/mapped property)
	 * @return the property type for the particular property,
	 * or {@code null} if not determinable
	 * @throws PropertyAccessException if the property was valid but the
	 * accessor method failed
	 */
	@Nullable
	Class<?> getPropertyType(String propertyName) throws BeansException;

	/**
	 * Return a type descriptor for the specified property:
	 *
	 * 返回指定属性的类型描述器
	 * preferably from the read method, falling back to the write method.
	 *
	 * 最好从读取方法转回写入方法。
	 * @param propertyName the property to check
	 * (may be a nested path and/or an indexed/mapped property)
	 * @return the property type for the particular property,
	 * or {@code null} if not determinable
	 * @throws PropertyAccessException if the property was valid but the
	 * accessor method failed
	 */
	@Nullable
	TypeDescriptor getPropertyTypeDescriptor(String propertyName) throws BeansException;

	/**
	 * Get the current value of the specified property.
	 *
	 * 从给定的属性名称中获取当前的属性值
	 * @param propertyName the name of the property to get the value of
	 * (may be a nested path and/or an indexed/mapped property)
	 * @return the value of the property
	 * @throws InvalidPropertyException if there is no such property or
	 * if the property isn't readable
	 * @throws PropertyAccessException if the property was valid but the
	 * accessor method failed
	 */
	@Nullable
	Object getPropertyValue(String propertyName) throws BeansException;

	/**
	 * Set the specified value as current property value.
	 *
	 * 从给定的值和给定的名称设置对应的属性值
	 * @param propertyName the name of the property to set the value of
	 * (may be a nested path and/or an indexed/mapped property)
	 * @param value the new value
	 * @throws InvalidPropertyException if there is no such property or
	 * if the property isn't writable
	 * @throws PropertyAccessException if the property was valid but the
	 * accessor method failed or a type mismatch occurred
	 */
	void setPropertyValue(String propertyName, @Nullable Object value) throws BeansException;

	/**
	 * Set the specified value as current property value.
	 *
	 * 从给定的 PropertyValue 值，设置对应对应的属性值
	 * @param pv an object containing the new property value
	 * @throws InvalidPropertyException if there is no such property or
	 * if the property isn't writable
	 * @throws PropertyAccessException if the property was valid but the
	 * accessor method failed or a type mismatch occurred
	 */
	void setPropertyValue(PropertyValue pv) throws BeansException;

	/**
	 * Perform a batch update from a Map.
	 *
	 * 从给定的 map 中批量设置属性
	 * <p>Bulk updates from PropertyValues are more powerful: This method is
	 * provided for convenience. Behavior will be identical to that of
	 * the {@link #setPropertyValues(PropertyValues)} method.
	 * @param map a Map to take properties from. Contains property value objects,
	 * keyed by property name
	 * @throws InvalidPropertyException if there is no such property or
	 * if the property isn't writable
	 * @throws PropertyBatchUpdateException if one or more PropertyAccessExceptions
	 * occurred for specific properties during the batch update. This exception bundles
	 * all individual PropertyAccessExceptions. All other properties will have been
	 * successfully updated.
	 */
	void setPropertyValues(Map<?, ?> map) throws BeansException;

	/**
	 * The preferred way to perform a batch update.
	 * 从 PropertyValues 中批量设置属性值
	 * <p>Note that performing a batch update differs from performing a single update,
	 * in that an implementation of this class will continue to update properties
	 * if a <b>recoverable</b> error (such as a type mismatch, but <b>not</b> an
	 * invalid field name or the like) is encountered, throwing a
	 * {@link PropertyBatchUpdateException} containing all the individual errors.
	 * This exception can be examined later to see all binding errors.
	 * Properties that were successfully updated remain changed.
	 * <p>Does not allow unknown fields or invalid fields.
	 * @param pvs a PropertyValues to set on the target object
	 * @throws InvalidPropertyException if there is no such property or
	 * if the property isn't writable
	 * @throws PropertyBatchUpdateException if one or more PropertyAccessExceptions
	 * occurred for specific properties during the batch update. This exception bundles
	 * all individual PropertyAccessExceptions. All other properties will have been
	 * successfully updated.
	 * @see #setPropertyValues(PropertyValues, boolean, boolean)
	 */
	void setPropertyValues(PropertyValues pvs) throws BeansException;

	/**
	 * Perform a batch update with more control over behavior.
	 *
	 * 批量更新属性值（是否忽略我们未知的属性）
	 * <p>Note that performing a batch update differs from performing a single update,
	 * in that an implementation of this class will continue to update properties
	 * if a <b>recoverable</b> error (such as a type mismatch, but <b>not</b> an
	 * invalid field name or the like) is encountered, throwing a
	 * {@link PropertyBatchUpdateException} containing all the individual errors.
	 * This exception can be examined later to see all binding errors.
	 * Properties that were successfully updated remain changed.
	 * @param pvs a PropertyValues to set on the target object
	 * @param ignoreUnknown should we ignore unknown properties (not found in the bean)
	 * @throws InvalidPropertyException if there is no such property or
	 * if the property isn't writable
	 * @throws PropertyBatchUpdateException if one or more PropertyAccessExceptions
	 * occurred for specific properties during the batch update. This exception bundles
	 * all individual PropertyAccessExceptions. All other properties will have been
	 * successfully updated.
	 * @see #setPropertyValues(PropertyValues, boolean, boolean)
	 */
	void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown)
			throws BeansException;

	/**
	 * Perform a batch update with full control over behavior.
	 *
	 * 批量更新属性值（是否忽略我们未知的属性和无效的属性）
	 * <p>Note that performing a batch update differs from performing a single update,
	 * in that an implementation of this class will continue to update properties
	 * if a <b>recoverable</b> error (such as a type mismatch, but <b>not</b> an
	 * invalid field name or the like) is encountered, throwing a
	 * {@link PropertyBatchUpdateException} containing all the individual errors.
	 * This exception can be examined later to see all binding errors.
	 * Properties that were successfully updated remain changed.
	 * @param pvs a PropertyValues to set on the target object
	 * @param ignoreUnknown should we ignore unknown properties (not found in the bean)
	 * @param ignoreInvalid should we ignore invalid properties (found but not accessible)
	 * @throws InvalidPropertyException if there is no such property or
	 * if the property isn't writable
	 * @throws PropertyBatchUpdateException if one or more PropertyAccessExceptions
	 * occurred for specific properties during the batch update. This exception bundles
	 * all individual PropertyAccessExceptions. All other properties will have been
	 * successfully updated.
	 */
	void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid)
			throws BeansException;

}
