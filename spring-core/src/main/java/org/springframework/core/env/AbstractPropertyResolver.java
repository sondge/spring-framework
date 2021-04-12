/*
 * Copyright 2002-2020 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Abstract base class for resolving properties against any underlying source.
 *
 * 属性资源解析的 Abstract  实现
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public abstract class AbstractPropertyResolver implements ConfigurablePropertyResolver {

	// 设置对应的日志信息
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	// 定义 ConfigurableConversionService（类型转换器）
	private volatile ConfigurableConversionService conversionService;

	@Nullable
	// 属性占位符解析器(非严格的)
	private PropertyPlaceholderHelper nonStrictHelper;

	@Nullable
	// 属性占位符解析器（严格的）
	private PropertyPlaceholderHelper strictHelper;
	// 设置是否抛出异常
	private boolean ignoreUnresolvableNestedPlaceholders = false;
	// 占位符前缀
	private String placeholderPrefix = SystemPropertyUtils.PLACEHOLDER_PREFIX;
	// 占位符后缀
	private String placeholderSuffix = SystemPropertyUtils.PLACEHOLDER_SUFFIX;

	@Nullable
	// 与默认值的分割
	private String valueSeparator = SystemPropertyUtils.VALUE_SEPARATOR;
	// 必须要有的字段值
	private final Set<String> requiredProperties = new LinkedHashSet<>();


	@Override
	// 获取对应的类型转换器（单例模式，双重检查锁）
	public ConfigurableConversionService getConversionService() {
		// Need to provide an independent DefaultConversionService, not the
		// shared DefaultConversionService used by PropertySourcesPropertyResolver.
		// 获取对应的类型转换器
		ConfigurableConversionService cs = this.conversionService;
		// 如果对应的类型转换器不存在
		if (cs == null) {
			// 加锁
			synchronized (this) {
				// 获取对应的类型转换器
				cs = this.conversionService;
				// 如果类型转换器还是不存在
				if (cs == null) {
					// 创建一个新的类型转换器
					cs = new DefaultConversionService();
					this.conversionService = cs;
				}
			}
		}
		return cs;
	}

	@Override
	//  设置对应的类型转换器
	public void setConversionService(ConfigurableConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
	}

	/**
	 * Set the prefix that placeholders replaced by this resolver must begin with.
	 * <p>The default is "${".
	 *
	 * 设置对应的占位符前缀默认为（${}）
	 * @see org.springframework.util.SystemPropertyUtils#PLACEHOLDER_PREFIX
	 */
	@Override
	public void setPlaceholderPrefix(String placeholderPrefix) {
		Assert.notNull(placeholderPrefix, "'placeholderPrefix' must not be null");
		this.placeholderPrefix = placeholderPrefix;
	}

	/**
	 * Set the suffix that placeholders replaced by this resolver must end with.
	 * <p>The default is "}".
	 *
	 *  设置对应的占位符后缀，默认为:(})
	 * @see org.springframework.util.SystemPropertyUtils#PLACEHOLDER_SUFFIX
	 */
	@Override
	public void setPlaceholderSuffix(String placeholderSuffix) {
		Assert.notNull(placeholderSuffix, "'placeholderSuffix' must not be null");
		this.placeholderSuffix = placeholderSuffix;
	}

	/**
	 * Specify the separating character between the placeholders replaced by this
	 * resolver and their associated default value, or {@code null} if no such
	 * special character should be processed as a value separator.
	 *
	 * 设置默认的属性，key 和值的分隔器
	 * <p>The default is ":".
	 * @see org.springframework.util.SystemPropertyUtils#VALUE_SEPARATOR
	 */
	@Override
	public void setValueSeparator(@Nullable String valueSeparator) {
		this.valueSeparator = valueSeparator;
	}

	/**
	 * Set whether to throw an exception when encountering an unresolvable placeholder
	 * nested within the value of a given property. A {@code false} value indicates strict
	 * resolution, i.e. that an exception will be thrown. A {@code true} value indicates
	 * that unresolvable nested placeholders should be passed through in their unresolved
	 *
	 * 设置是否抛出异常
	 * ${...} form.
	 * <p>The default is {@code false}.
	 * @since 3.2
	 */
	@Override
	public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {
		this.ignoreUnresolvableNestedPlaceholders = ignoreUnresolvableNestedPlaceholders;
	}

	@Override
	// 设置必须要有的属性值
	public void setRequiredProperties(String... requiredProperties) {
		Collections.addAll(this.requiredProperties, requiredProperties);
	}

	@Override
	// 校验必要的属性值是否有空值
	public void validateRequiredProperties() {
		MissingRequiredPropertiesException ex = new MissingRequiredPropertiesException();
		for (String key : this.requiredProperties) {
			if (this.getProperty(key) == null) {
				ex.addMissingRequiredProperty(key);
			}
		}
		if (!ex.getMissingRequiredProperties().isEmpty()) {
			throw ex;
		}
	}

	@Override
	// 是否包含对应的属性值
	public boolean containsProperty(String key) {
		return (getProperty(key) != null);
	}

	@Override
	@Nullable
	// 获取对应的属性
	public String getProperty(String key) {
		return getProperty(key, String.class);
	}

	@Override
	// 获取对应的属性值
	public String getProperty(String key, String defaultValue) {
		String value = getProperty(key);
		// 不存在就返回默认的值
		return (value != null ? value : defaultValue);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
		// 获取对应类型的属性值
		T value = getProperty(key, targetType);
		return (value != null ? value : defaultValue);
	}

	@Override
	// 获取必要的属性值，如果不存在就抛出 IllegalStateException
	public String getRequiredProperty(String key) throws IllegalStateException {
		String value = getProperty(key);
		if (value == null) {
			throw new IllegalStateException("Required key '" + key + "' not found");
		}
		return value;
	}

	@Override
	// 获取指定类型的属性值，如果为空，则抛出 IllegalStateException
	public <T> T getRequiredProperty(String key, Class<T> valueType) throws IllegalStateException {
		T value = getProperty(key, valueType);
		if (value == null) {
			throw new IllegalStateException("Required key '" + key + "' not found");
		}
		return value;
	}

	@Override
	// 解析文本中的站位父
	public String resolvePlaceholders(String text) {
		if (this.nonStrictHelper == null) {
			this.nonStrictHelper = createPlaceholderHelper(true);
		}
		return doResolvePlaceholders(text, this.nonStrictHelper);
	}

	@Override
	// 解析文本中的必要属性的占位符
	public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
		if (this.strictHelper == null) {
			this.strictHelper = createPlaceholderHelper(false);
		}
		return doResolvePlaceholders(text, this.strictHelper);
	}

	/**
	 * Resolve placeholders within the given string, deferring to the value of
	 * {@link #setIgnoreUnresolvableNestedPlaceholders} to determine whether any
	 * unresolvable placeholders should raise an exception or be ignored.
	 *
	 * // 解析对应的属性占位符的值，如果需要抛出异常，就使用resolveRequiredPlaceholders, 否则 resolvePlaceholders
	 * <p>Invoked from {@link #getProperty} and its variants, implicitly resolving
	 * nested placeholders. In contrast, {@link #resolvePlaceholders} and
	 * {@link #resolveRequiredPlaceholders} do <i>not</i> delegate
	 * to this method but rather perform their own handling of unresolvable
	 * placeholders, as specified by each of those methods.
	 * @since 3.2
	 * @see #setIgnoreUnresolvableNestedPlaceholders
	 */
	protected String resolveNestedPlaceholders(String value) {
		if (value.isEmpty()) {
			return value;
		}
		return (this.ignoreUnresolvableNestedPlaceholders ?
				resolvePlaceholders(value) : resolveRequiredPlaceholders(value));
	}

	// 创建对应的 占位符解析器
	private PropertyPlaceholderHelper createPlaceholderHelper(boolean ignoreUnresolvablePlaceholders) {
		return new PropertyPlaceholderHelper(this.placeholderPrefix, this.placeholderSuffix,
				this.valueSeparator, ignoreUnresolvablePlaceholders);
	}

	// 解析对应的占位符
	private String doResolvePlaceholders(String text, PropertyPlaceholderHelper helper) {
		return helper.replacePlaceholders(text, this::getPropertyAsRawString);
	}

	/**
	 * Convert the given value to the specified target type, if necessary.
	 *
	 * 如果有必要的话就转换对应的值
	 * @param value the original property value
	 * @param targetType the specified target type for property retrieval
	 * @return the converted value, or the original value if no conversion
	 * is necessary
	 * @since 4.3.5
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	protected <T> T convertValueIfNecessary(Object value, @Nullable Class<T> targetType) {
		// 如果类型为空，直接强转
		if (targetType == null) {
			return (T) value;
		}
		// 获取对应的转换器
		ConversionService conversionServiceToUse = this.conversionService;
		if (conversionServiceToUse == null) {
			// Avoid initialization of shared DefaultConversionService if
			// no standard type conversion is needed in the first place...
			if (ClassUtils.isAssignableValue(targetType, value)) {
				return (T) value;
			}
			//  获取共享的转换器实例
			conversionServiceToUse = DefaultConversionService.getSharedInstance();
		}
		// 转换对应的属性值
		return conversionServiceToUse.convert(value, targetType);
	}


	/**
	 * Retrieve the specified property as a raw String,
	 * i.e. without resolution of nested placeholders.
	 *
	 * 将指定的属性作为原始字符串检索，即不需要解析嵌套占位符。
	 *
	 *
	 * @param key the property name to resolve
	 * @return the property value or {@code null} if none found
	 */
	@Nullable
	protected abstract String getPropertyAsRawString(String key);

}
