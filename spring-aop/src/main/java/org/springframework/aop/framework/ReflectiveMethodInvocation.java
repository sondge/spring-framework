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

package org.springframework.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.lang.Nullable;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring's implementation of the AOP Alliance
 * <p>
 * AOP 联盟 Spring 的实现
 * {@link org.aopalliance.intercept.MethodInvocation} interface,
 * implementing the extended
 * {@link org.springframework.aop.ProxyMethodInvocation} interface.
 *
 * <p>Invokes the target object using reflection. Subclasses can override the
 * {@link #invokeJoinpoint()} method to change this behavior, so this is also
 * a useful base class for more specialized MethodInvocation implementations.
 *
 * <p>It is possible to clone an invocation, to invoke {@link #proceed()}
 * repeatedly (once per clone), using the {@link #invocableClone()} method.
 * It is also possible to attach custom attributes to the invocation,
 * using the {@link #setUserAttribute} / {@link #getUserAttribute} methods.
 *
 * <p><b>NOTE:</b> This class is considered internal and should not be
 * directly accessed. The sole reason for it being public is compatibility
 * with existing framework integrations (e.g. Pitchfork). For any other
 * purposes, use the {@link ProxyMethodInvocation} interface instead.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @see #invokeJoinpoint
 * @see #proceed
 * @see #invocableClone
 * @see #setUserAttribute
 * @see #getUserAttribute
 */
public class ReflectiveMethodInvocation implements ProxyMethodInvocation, Cloneable {
	// 定义一个代理实例
	protected final Object proxy;

	@Nullable
	// 定义一个目标实例
	protected final Object target;
	// 定义方法
	protected final Method method;
	// 参数
	protected Object[] arguments;

	@Nullable
	// 目标类型
	private final Class<?> targetClass;

	/**
	 * Lazily initialized map of user-specific attributes for this invocation.
	 * 对于这个调用用户指定的属性的延迟初始化集合
	 */
	@Nullable
	private Map<String, Object> userAttributes;

	/**
	 * List of MethodInterceptor and InterceptorAndDynamicMethodMatcher
	 * that need dynamic checks.
	 * 方法拦截器和定太代理匹配器的列表，需要动态代理检查
	 */
	protected final List<?> interceptorsAndDynamicMethodMatchers;

	/**
	 * Index from 0 of the current interceptor we're invoking.
	 * 我们正在执行的拦截器数量
	 * -1 until we invoke: then the current interceptor.
	 */
	private int currentInterceptorIndex = -1;


	/**
	 * Construct a new ReflectiveMethodInvocation with the given arguments.
	 * <p>
	 * 构造一个新的 ReflectiveMethodInvocation 从给定的参数
	 *
	 * @param proxy                                the proxy object that the invocation was made on
	 * @param target                               the target object to invoke
	 * @param method                               the method to invoke
	 * @param arguments                            the arguments to invoke the method with
	 * @param targetClass                          the target class, for MethodMatcher invocations
	 * @param interceptorsAndDynamicMethodMatchers interceptors that should be applied,
	 *                                             along with any InterceptorAndDynamicMethodMatchers that need evaluation at runtime.
	 *                                             MethodMatchers included in this struct must already have been found to have matched
	 *                                             as far as was possibly statically. Passing an array might be about 10% faster,
	 *                                             but would complicate the code. And it would work only for static pointcuts.
	 */
	protected ReflectiveMethodInvocation(
			Object proxy, @Nullable Object target, Method method, @Nullable Object[] arguments,
			@Nullable Class<?> targetClass, List<Object> interceptorsAndDynamicMethodMatchers) {
		// 赋值代理类
		this.proxy = proxy;
		// 赋值目标类
		this.target = target;
		// 获取目标类类型
		this.targetClass = targetClass;
		// 赋值方法
		this.method = BridgeMethodResolver.findBridgedMethod(method);
		// 获取适配参数
		this.arguments = AopProxyUtils.adaptArgumentsIfNecessary(method, arguments);
		// 获取获取拦截器和匹配器
		this.interceptorsAndDynamicMethodMatchers = interceptorsAndDynamicMethodMatchers;
	}


	@Override
	// 获取代理对象
	public final Object getProxy() {
		return this.proxy;
	}

	@Override
	@Nullable
	// 获取目标对象
	public final Object getThis() {
		return this.target;
	}

	@Override
	// 获取静态部分的方法
	public final AccessibleObject getStaticPart() {
		return this.method;
	}

	/**
	 * Return the method invoked on the proxied interface.
	 * 返回代理接口的方法执行
	 * May or may not correspond with a method invoked on an underlying
	 * implementation of that interface.
	 * 可能或者可能不对应在一个潜在的接口的实现的方法执行
	 */
	@Override
	public final Method getMethod() {
		return this.method;
	}

	//  获取当前的参数信息
	@Override
	public final Object[] getArguments() {
		return this.arguments;
	}

	// 设置对应的参数信息
	@Override
	public void setArguments(Object... arguments) {
		this.arguments = arguments;
	}


	// 定义执行器
	@Override
	@Nullable
	public Object proceed() throws Throwable {
		// We start with an index of -1 and increment early.
		// 我们从索引 -1 开始并尽早增加
		if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
			// 执行切点
			return invokeJoinpoint();
		}
		// 根据当前正在拦截的索引的拦截器
		Object interceptorOrInterceptionAdvice =
				this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
		// 如果拦截器是 InterceptorAndDynamicMethodMatcher
		if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher) {
			// Evaluate dynamic method matcher here: static part will already have
			// been evaluated and found to match.
			// 在这里评估动态方法匹配器：静态部分将准备评估和发现匹配
			InterceptorAndDynamicMethodMatcher dm =
					(InterceptorAndDynamicMethodMatcher) interceptorOrInterceptionAdvice;
			// 获取目标类的类型
			Class<?> targetClass = (this.targetClass != null ? this.targetClass : this.method.getDeclaringClass());
			// 如果可以匹配对应的方法和参数
			if (dm.methodMatcher.matches(this.method, targetClass, this.arguments)) {
				// 通过拦截器直接执行他们
				return dm.interceptor.invoke(this);
			} else {
				// Dynamic matching failed.
				// 动态匹配失败
				// Skip this interceptor and invoke the next in the chain.
				// 忽略这个拦截器和调用下一个拦截器链
				return proceed();
			}
		} else {
			// It's an interceptor, so we just invoke it: The pointcut will have
			// been evaluated statically before this object was constructed.
			// 他是一个拦截器，因此我们仅仅调用它：这个切点将在这个对象被构造前静态评估
			return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
		}
	}

	/**
	 * Invoke the joinpoint using reflection.
	 * 发射执行这个切点
	 * Subclasses can override this to use custom invocation.
	 * 子类可以重写这个执行方法
	 *
	 * @return the return value of the joinpoint
	 * 返回值是切点
	 * @throws Throwable if invoking the joinpoint resulted in an exception
	 *                   执行切点之后可能会抛出异常
	 */
	@Nullable
	protected Object invokeJoinpoint() throws Throwable {
		return AopUtils.invokeJoinpointUsingReflection(this.target, this.method, this.arguments);
	}


	/**
	 * This implementation returns a shallow copy of this invocation object,
	 * including an independent copy of the original arguments array.
	 * <p>
	 * 这个实现返回一个浅拷贝的调用对象，包括一个原始参数数组依赖的拷贝
	 * <p>We want a shallow copy in this case: We want to use the same interceptor
	 * chain and other object references, but we want an independent value for the
	 * current interceptor index.
	 *
	 * @see java.lang.Object#clone()
	 */
	@Override
	// 可调用的拷贝
	public MethodInvocation invocableClone() {
		// 获取当前的参数信息
		Object[] cloneArguments = this.arguments;
		if (this.arguments.length > 0) {
			// Build an independent copy of the arguments array.
			// 构建一个参数数组的依赖拷贝
			cloneArguments = this.arguments.clone();
		}
		// 可调用拷贝参数信息
		return invocableClone(cloneArguments);
	}

	/**
	 * This implementation returns a shallow copy of this invocation object,
	 * using the given arguments array for the clone.
	 * <p>We want a shallow copy in this case: We want to use the same interceptor
	 * chain and other object references, but we want an independent value for the
	 * current interceptor index.
	 *
	 * @see java.lang.Object#clone()
	 */
	@Override
	public MethodInvocation invocableClone(Object... arguments) {
		// Force initialization of the user attributes Map,
		// for having a shared Map reference in the clone.
		// 如果用户属性为空，则创建一个 HashMap
		if (this.userAttributes == null) {
			this.userAttributes = new HashMap<>();
		}

		// Create the MethodInvocation clone.
		// 创建这个方法调用的克隆
		try {
			// 定义发射方法调用独享的拷贝
			ReflectiveMethodInvocation clone = (ReflectiveMethodInvocation) clone();
			// 克隆的参数为当前的参数
			clone.arguments = arguments;
			// 返回拷贝对象
			return clone;
		} catch (CloneNotSupportedException ex) {
			throw new IllegalStateException(
					"Should be able to clone object of type [" + getClass() + "]: " + ex);
		}
	}


	@Override
	public void setUserAttribute(String key, @Nullable Object value) {
		// 如果对应的值不为空
		if (value != null) {
			// 如果 userAttributes 还没有初始化
			if (this.userAttributes == null) {
				this.userAttributes = new HashMap<>();
			}
			// 将键值推入到对应的缓存中
			this.userAttributes.put(key, value);
		} else {
			// 如果为空，并且 userAttributes 则删除对应的 key
			if (this.userAttributes != null) {
				this.userAttributes.remove(key);
			}
		}
	}

	@Override
	@Nullable
	// 根据给定的 key 获取对应的属性值
	public Object getUserAttribute(String key) {
		return (this.userAttributes != null ? this.userAttributes.get(key) : null);
	}

	/**
	 * Return user attributes associated with this invocation.
	 * 获取这个调用的用户联系地属性
	 * This method provides an invocation-bound alternative to a ThreadLocal.
	 * <p>This map is initialized lazily and is not used in the AOP framework itself.
	 *
	 * @return any user attributes associated with this invocation
	 * (never {@code null})
	 */
	public Map<String, Object> getUserAttributes() {
		if (this.userAttributes == null) {
			this.userAttributes = new HashMap<>();
		}
		return this.userAttributes;
	}


	@Override
	public String toString() {
		// Don't do toString on target, it may be proxied.
		StringBuilder sb = new StringBuilder("ReflectiveMethodInvocation: ");
		sb.append(this.method).append("; ");
		if (this.target == null) {
			sb.append("target is null");
		} else {
			sb.append("target is of class [").append(this.target.getClass().getName()).append(']');
		}
		return sb.toString();
	}

}
