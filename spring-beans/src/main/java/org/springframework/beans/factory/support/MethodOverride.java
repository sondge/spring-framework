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

package org.springframework.beans.factory.support;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;

/**
 * Object representing the override of a method on a managed object by the IoC
 * container.
 *
 *  对象代表这个覆盖一个方法在管理对象通过 IOC 容器
 *
 * <p>Note that the override mechanism is <em>not</em> intended as a generic
 * means of inserting crosscutting code: use AOP for that.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.1
 */
public abstract class MethodOverride implements BeanMetadataElement {
	/**
	 *  定义一个不可变的方法名称
	 */
	private final String methodName;
	/**
	 * 是否覆盖
	 */
	private boolean overloaded = true;

	@Nullable
	/**
	 * 资源
	 */
	private Object source;


	/**
	 * Construct a new override for the given method.
	 *
	 * 从给定的方法名称创建一个新的覆盖构造函数
	 * @param methodName the name of the method to override
	 */
	protected MethodOverride(String methodName) {
		Assert.notNull(methodName, "Method name must not be null");
		this.methodName = methodName;
	}


	/**
	 * Return the name of the method to be overridden.
	 *
	 * 返回这个对应的方法名称
	 */
	public String getMethodName() {
		return this.methodName;
	}

	/**
	 * Set whether the overridden method is <em>overloaded</em> (i.e., whether argument
	 * type matching needs to occur to disambiguate methods of the same name).
	 *
	 * 设置是否重载
	 * <p>Default is {@code true}; can be switched to {@code false} to optimize
	 * runtime performance.
	 */
	protected void setOverloaded(boolean overloaded) {
		this.overloaded = overloaded;
	}

	/**
	 * Return whether the overridden method is <em>overloaded</em> (i.e., whether argument
	 * type matching needs to occur to disambiguate methods of the same name).
	 *
	 * 获取是否重载
	 */
	protected boolean isOverloaded() {
		return this.overloaded;
	}

	/**
	 * Set the configuration source {@code Object} for this metadata element.
	 *
	 * 从这个给定元素设置这个配置资源
	 * <p>The exact type of the object will depend on the configuration mechanism used.
	 */
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}

	/**
	 * Subclasses must override this to indicate whether they <em>match</em> the given method.
	 *
	 * 子类必须重载此类方法，从给定的方法名中匹配对应的方法
	 * This allows for argument list checking as well as method name checking.
	 * @param method the method to check
	 * @return whether this override matches the given method
	 */
	public abstract boolean matches(Method method);


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MethodOverride)) {
			return false;
		}
		MethodOverride that = (MethodOverride) other;
		return (ObjectUtils.nullSafeEquals(this.methodName, that.methodName) &&
				ObjectUtils.nullSafeEquals(this.source, that.source));
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(this.methodName);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.source);
		return hashCode;
	}

}
