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

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.AopInvocationException;
import org.springframework.aop.RawTargetAccess;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DecoratingProxy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * JDK-based {@link AopProxy} implementation for the Spring AOP framework,
 * based on JDK {@link java.lang.reflect.Proxy dynamic proxies}.
 *
 * <p>Creates a dynamic proxy, implementing the interfaces exposed by
 * the AopProxy. Dynamic proxies <i>cannot</i> be used to proxy methods
 * defined in classes, rather than interfaces.
 *
 * <p>Objects of this type should be obtained through proxy factories,
 * configured by an {@link AdvisedSupport} class. This class is internal
 * to Spring's AOP framework and need not be used directly by client code.
 *
 * <p>Proxies created using this class will be thread-safe if the
 * underlying (target) class is thread-safe.
 *
 * <p>Proxies are serializable so long as all Advisors (including Advices
 * and Pointcuts) and the TargetSource are serializable.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @see java.lang.reflect.Proxy
 * @see AdvisedSupport
 * @see ProxyFactory
 */
final class JdkDynamicAopProxy implements AopProxy, InvocationHandler, Serializable {

	/**
	 * use serialVersionUID from Spring 1.2 for interoperability.
	 */

	// 序列化
	private static final long serialVersionUID = 5531744639992436476L;


	/*
	 * NOTE: We could avoid the code duplication between this class and the CGLIB
	 * proxies by refactoring "invoke" into a template method. However, this approach
	 * adds at least 10% performance overhead versus a copy-paste solution, so we sacrifice
	 * elegance for performance. (We have a good test suite to ensure that the different
	 * proxies behave the same :-)
	 * This way, we can also more easily take advantage of minor optimizations in each class.
	 */

	/**
	 * We use a static Log to avoid serialization issues.
	 */
	private static final Log logger = LogFactory.getLog(JdkDynamicAopProxy.class);

	/**
	 * Config used to configure this proxy.
	 */
	private final AdvisedSupport advised;

	/**
	 * Is the {@link #equals} method defined on the proxied interfaces?
	 */
	private boolean equalsDefined;

	/**
	 * Is the {@link #hashCode} method defined on the proxied interfaces?
	 */
	private boolean hashCodeDefined;


	/**
	 * Construct a new JdkDynamicAopProxy for the given AOP configuration.
	 *
	 * @param config the AOP configuration as AdvisedSupport object
	 * @throws AopConfigException if the config is invalid. We try to throw an informative
	 *                            exception in this case, rather than let a mysterious failure happen later.
	 */
	public JdkDynamicAopProxy(AdvisedSupport config) throws AopConfigException {
		Assert.notNull(config, "AdvisedSupport must not be null");
		if (config.getAdvisors().length == 0 && config.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE) {
			throw new AopConfigException("No advisors and no TargetSource specified");
		}
		this.advised = config;
	}


	@Override
	public Object getProxy() {
		return getProxy(ClassUtils.getDefaultClassLoader());
	}

	@Override
	public Object getProxy(@Nullable ClassLoader classLoader) {
		if (logger.isTraceEnabled()) {
			logger.trace("Creating JDK dynamic proxy: " + this.advised.getTargetSource());
		}
		// 获取代理的接口
		Class<?>[] proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(this.advised, true);
		// 查找定义的相等的方法
		findDefinedEqualsAndHashCodeMethods(proxiedInterfaces);
		// 创建一个新的代理对象
		return Proxy.newProxyInstance(classLoader, proxiedInterfaces, this);
	}

	/**
	 * Finds any {@link #equals} or {@link #hashCode} method that may be defined
	 * on the supplied set of interfaces.
	 *
	 * @param proxiedInterfaces the interfaces to introspect
	 */
	private void findDefinedEqualsAndHashCodeMethods(Class<?>[] proxiedInterfaces) {
		// 循环遍历接口
		for (Class<?> proxiedInterface : proxiedInterfaces) {
			// 循环遍历方法
			Method[] methods = proxiedInterface.getDeclaredMethods();
			for (Method method : methods) {
				// 如果 equals 方法相等
				if (AopUtils.isEqualsMethod(method)) {
					// 设置 equals 相等
					this.equalsDefined = true;
				}
				// 如果是 method
				if (AopUtils.isHashCodeMethod(method)) {
					// hashCode 方法定义
					this.hashCodeDefined = true;
				}
				// 如果 equals 和 hashCode 都已经定义，直接返回
				if (this.equalsDefined && this.hashCodeDefined) {
					return;
				}
			}
		}
	}


	/**
	 * Implementation of {@code InvocationHandler.invoke}.
	 * <p>
	 * 实现 InvocationHandler
	 * <p>Callers will see exactly the exception thrown by the target,
	 * unless a hook method throws an exception.
	 */
	@Override
	@Nullable
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// 定义老代理对象
		Object oldProxy = null;
		// 设置代理上下文
		boolean setProxyContext = false;
		// 设置目标资源
		TargetSource targetSource = this.advised.targetSource;
		// 目标
		Object target = null;

		try {
			// 如果是 equal 方法，直接返回 equals
			if (!this.equalsDefined && AopUtils.isEqualsMethod(method)) {
				// The target does not implement the equals(Object) method itself.
				return equals(args[0]);
			}
			// 如果是 hashCode 方法直接返回 hashCode
			else if (!this.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
				// The target does not implement the hashCode() method itself.
				return hashCode();
			}
			// 如果是装饰类，返回最终的目标类
			else if (method.getDeclaringClass() == DecoratingProxy.class) {
				// There is only getDecoratedClass() declared -> dispatch to proxy config.
				// 仅仅是装饰器类的声明
				return AopProxyUtils.ultimateTargetClass(this.advised);
			}
			// 如果通知器是透明的，并器修饰类是接口且 Advised 是有权限的
			else if (!this.advised.opaque && method.getDeclaringClass().isInterface() &&
					method.getDeclaringClass().isAssignableFrom(Advised.class)) {
				// Service invocations on ProxyConfig with the proxy config...
				return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
			}
			// 定义返回值
			Object retVal;
			// 如果 advised 暴露了代理
			if (this.advised.exposeProxy) {
				// Make invocation available if necessary.
				// 老的代理对象为当前代理
				oldProxy = AopContext.setCurrentProxy(proxy);
				// 设置代理对象的上下文为 true
				setProxyContext = true;
			}

			// Get as late as possible to minimize the time we "own" the target,
			// in case it comes from a pool.
			// 获取目标对象
			target = targetSource.getTarget();
			// 如果目标对象为空直接为空，否则为目标对象的类型
			Class<?> targetClass = (target != null ? target.getClass() : null);

			// Get the interception chain for this method.
			// 获取拦截器链对这个方法
			List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

			// Check whether we have any advice. If we don't, we can fallback on direct
			// 检查我们是否有任何的通知，如果我们没有，我们可以倒退反向调用这个目标以及通知创建一个 MethodInvocation
			// reflective invocation of the target, and avoid creating a MethodInvocation.
			// 如果调用链是空的
			if (chain.isEmpty()) {
				// We can skip creating a MethodInvocation: just invoke the target directly
				// 我们可以忽略创建一个 MethodInvocation：仅仅直接代用目标
				// Note that the final invoker must be an InvokerInterceptor so we know it does
				// 记录这个最终调用器必须是一个调用拦截器因此我们知道它不处理任何事情，但是一个可以直接操作这个目标并且不是热交换和花式代理
				// nothing but a reflective operation on the target, and no hot swapping or fancy proxying.
				// 如果需要适配参数
				Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
				// 直接执行切点
				retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
			} else {
				// We need to create a method invocation...
				// 我们需要创建一个方法调用去
				MethodInvocation invocation =
						new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
				// Proceed to the joinpoint through the interceptor chain.
				// 执行这个 joinpoint 通过拦截器链
				retVal = invocation.proceed();
			}

			// Massage return value if necessary.
			// 获取方法返回值类型
			Class<?> returnType = method.getReturnType();
			// 如果返回值不为空、返回值为 target、返回值类型不是 Object 类、返回值类型是代理类的示例并且方法的声明了不是一个接口
			if (retVal != null && retVal == target &&
					returnType != Object.class && returnType.isInstance(proxy) &&
					!RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
				// Special case: it returned "this" and the return type of the method
				// is type-compatible. Note that we can't help if the target sets
				// a reference to itself in another returned object.
				// 直接令返回值设置为 proxy
				retVal = proxy;
			} else if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
				throw new AopInvocationException(
						"Null return value from advice does not match primitive return type for: " + method);
			}
			// 直接返回返回值
			return retVal;
		} finally {
			// 如果 target 不是 null 并且 targetSource 不是静态的
			if (target != null && !targetSource.isStatic()) {
				// Must have come from TargetSource.
				// 释放目标资源
				targetSource.releaseTarget(target);
			}
			// 设置上下文
			if (setProxyContext) {
				// Restore old proxy.
				// 重新设置上下文
				AopContext.setCurrentProxy(oldProxy);
			}
		}
	}


	/**
	 * Equality means interfaces, advisors and TargetSource are equal.
	 *
	 * <p>The compared object may be a JdkDynamicAopProxy instance itself
	 * or a dynamic proxy wrapping a JdkDynamicAopProxy instance.
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		if (other == this) {
			return true;
		}
		if (other == null) {
			return false;
		}

		JdkDynamicAopProxy otherProxy;
		if (other instanceof JdkDynamicAopProxy) {
			otherProxy = (JdkDynamicAopProxy) other;
		} else if (Proxy.isProxyClass(other.getClass())) {
			InvocationHandler ih = Proxy.getInvocationHandler(other);
			if (!(ih instanceof JdkDynamicAopProxy)) {
				return false;
			}
			otherProxy = (JdkDynamicAopProxy) ih;
		} else {
			// Not a valid comparison...
			return false;
		}

		// If we get here, otherProxy is the other AopProxy.
		return AopProxyUtils.equalsInProxy(this.advised, otherProxy.advised);
	}

	/**
	 * Proxy uses the hash code of the TargetSource.
	 */
	@Override
	public int hashCode() {
		return JdkDynamicAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
	}

}
