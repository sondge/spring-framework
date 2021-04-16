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

package org.springframework.aop.framework.autoproxy;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ProxyProcessorSupport;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that wraps each eligible bean with an AOP proxy, delegating to specified interceptors
 * before invoking the bean itself.
 * <p>
 * 包裹每一个有资格的 bean 的 AOP 代理，委托给一个特殊的拦截器，在执行这个 bean 之前的 BeanPostProcessor 实现
 *
 * <p>This class distinguishes between "common" interceptors: shared for all proxies it
 * creates, and "specific" interceptors: unique per bean instance. There need not be any
 * common interceptors. If there are, they are set using the interceptorNames property.
 * As with {@link org.springframework.aop.framework.ProxyFactoryBean}, interceptors names
 * in the current factory are used rather than bean references to allow correct handling
 * of prototype advisors and interceptors: for example, to support stateful mixins.
 * Any advice type is supported for {@link #setInterceptorNames "interceptorNames"} entries.
 * <p>
 * 这个类区分为常见的拦截器：对于他创建的所有的代理和特别的拦截器(每一个唯一的 bean 实例)都共享。
 * 这里不需要任何的拦截器。如果有拦截器，他们将使用拦截器名称属性。与 ProxyFactoryBean， 拦截器名称在当前工厂是被使用的。而不是 bean 引用。
 * 允许正确的处理 袁英 Advisors 和 拦截器：例如，为了支持有状态的混合，任何的通知类型是支持 interceptorNames 条目的
 *
 * <p>Such auto-proxying is particularly useful if there's a large number of beans that
 * need to be wrapped with similar proxies, i.e. delegating to the same interceptors.
 * Instead of x repetitive proxy definitions for x target beans, you can register
 * one single such post processor with the bean factory to achieve the same effect.
 * <p>
 * 例如自动代理是规则使用的，如果有大量的 bean 需要被包装和相似的代理。委托这个相同的拦截器。
 * 替代重复的目标代理 BeanDefinition，你可以注册一个单例，例如 postProcessor 和这个 BeanFactory 为了达到相同的影响
 *
 * <p>Subclasses can apply any strategy to decide if a bean is to be proxied, e.g. by type,
 * by name, by definition details, etc. They can also return additional interceptors that
 * should just be applied to the specific bean instance. A simple concrete implementation is
 * {@link BeanNameAutoProxyCreator}, identifying the beans to be proxied via given names.
 *
 * <p>Any number of {@link TargetSourceCreator} implementations can be used to create
 * a custom target source: for example, to pool prototype objects. Auto-proxying will
 * occur even if there is no advice, as long as a TargetSourceCreator specifies a custom
 * {@link org.springframework.aop.TargetSource}. If there are no TargetSourceCreators set,
 * or if none matches, a {@link org.springframework.aop.target.SingletonTargetSource}
 * will be used by default to wrap the target bean instance.
 * <p>
 * 为了创建一个目标资源任何数量的 TargetSourceCreator 实现能被使用：例如，原型池类型。自动代理将发生（及时没有通知）,
 * 只要一个 TargetSourceCreator 指定一个 TargetSource. 如果没有设置 TargetSourceCreators, 将会使用默认的包装目标 Bean 实例
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Rob Harrop
 * @see #setInterceptorNames
 * @see #getAdvicesAndAdvisorsForBean
 * @see BeanNameAutoProxyCreator
 * @see DefaultAdvisorAutoProxyCreator
 * @since 13.10.2003
 */
@SuppressWarnings("serial")
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
		implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {

	/**
	 * Convenience constant for subclasses: Return value for "do not proxy".
	 * <p>
	 * 方便子类的常量，返回值为没有代理
	 *
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	@Nullable
	protected static final Object[] DO_NOT_PROXY = null;

	/**
	 * Convenience constant for subclasses: Return value for
	 * "proxy without additional interceptors, just the common ones".
	 * <p>
	 * 方便子类的常量，返回值为代理没有额外的拦截器，仅仅使用一个共同的一些
	 *
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	protected static final Object[] PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS = new Object[0];


	/**
	 * Logger available to subclasses.
	 * <p>
	 * 子类使用的日志系统
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * Default is global AdvisorAdapterRegistry.
	 * <p>
	 * 全局默认的 Advisor 适配器注册表
	 */
	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	/**
	 * Indicates whether or not the proxy should be frozen. Overridden from super
	 * to prevent the configuration from becoming frozen too early.
	 * <p>
	 * <p>
	 * 表示是否应该冻结代理，防止覆盖父类配置提前被冻结
	 */
	private boolean freezeProxy = false;

	/**
	 * Default is no common interceptors.
	 * 默认是没有共同的拦截器
	 */
	private String[] interceptorNames = new String[0];
	// 是否首先应用共同的拦截器
	private boolean applyCommonInterceptorsFirst = true;

	@Nullable
	// 用户自定义 TargetSourceCreator 数组
	private TargetSourceCreator[] customTargetSourceCreators;

	@Nullable
	//  定义的 BeanFactory
	private BeanFactory beanFactory;
	// 目标资源 Bean
	private final Set<String> targetSourcedBeans = Collections.newSetFromMap(new ConcurrentHashMap<>(16));
	// 早期代理的引用
	private final Map<Object, Object> earlyProxyReferences = new ConcurrentHashMap<>(16);
	// 代理的类型
	private final Map<Object, Class<?>> proxyTypes = new ConcurrentHashMap<>(16);
	// 通知的 Bean 列表
	private final Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<>(256);


	/**
	 * Set whether or not the proxy should be frozen, preventing advice
	 * from being added to it once it is created.
	 * <p>
	 * 根据给定的值判定设置是否冻结代理
	 * <p>Overridden from the super class to prevent the proxy configuration
	 * from being frozen before the proxy is created.
	 */
	@Override
	public void setFrozen(boolean frozen) {
		this.freezeProxy = frozen;
	}

	@Override
	// 获取是否冻结代理
	public boolean isFrozen() {
		return this.freezeProxy;
	}

	/**
	 * Specify the {@link AdvisorAdapterRegistry} to use.
	 * <p>Default is the global {@link AdvisorAdapterRegistry}.
	 * <p>
	 * 指定 AdvisorAdapterRegistry 这个将使用
	 * AdvisorAdapterRegistry 默认是全局的
	 *
	 * @see org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	/**
	 * Set custom {@code TargetSourceCreators} to be applied in this order.
	 * If the list is empty, or they all return null, a {@link SingletonTargetSource}
	 * will be created for each bean.
	 * <p>
	 * 设置用户自定义被排序应用的 TargetSourceCreator 数组， 如果这个数组为空，或者他们都是空的，SingletonTargetSource 将被创建对于每一个 Bena
	 * <p>Note that TargetSourceCreators will kick in even for target beans
	 * where no advices or advisors have been found. If a {@code TargetSourceCreator}
	 * returns a {@link TargetSource} for a specific bean, that bean will be proxied
	 * in any case.
	 * <p>
	 * TargetSourceCreator 将起作用，即使目标的 Beans 没有设置通知。如果 TargetSourceCreator 返回一个指定的 Bean， 那么这个 bean 将被代理
	 * <p>{@code TargetSourceCreators} can only be invoked if this post processor is used
	 * in a {@link BeanFactory} and its {@link BeanFactoryAware} callback is triggered.
	 * <p>
	 * TargetSource 仅仅可以被执行如果这个 psoProcessor 在被使用。在 BeanFactory 和 它的 BeanFactoryAware 回调是已经触发的
	 *
	 * @param targetSourceCreators the list of {@code TargetSourceCreators}.
	 *                             Ordering is significant: The {@code TargetSource} returned from the first matching
	 *                             {@code TargetSourceCreator} (that is, the first that returns non-null) will be used.
	 */
	public void setCustomTargetSourceCreators(TargetSourceCreator... targetSourceCreators) {
		this.customTargetSourceCreators = targetSourceCreators;
	}

	/**
	 * Set the common interceptors. These must be bean names in the current factory.
	 * They can be of any advice or advisor type Spring supports.
	 * <p>
	 * 设置共同的拦截器。这些 Bean 必须要在当前工厂内有 bean 名称，他们可以被 Spring 支持的任何一款通知器类型
	 * <p>If this property isn't set, there will be zero common interceptors.
	 * This is perfectly valid, if "specific" interceptors such as matching
	 * Advisors are all we want.
	 * <p>
	 * 如果这个属性没有设置，这里将没有共同的拦截器. 这是完美的有效，对于所以我们需要的匹配的 Advisor
	 */
	public void setInterceptorNames(String... interceptorNames) {
		this.interceptorNames = interceptorNames;
	}

	/**
	 * Set whether the common interceptors should be applied before bean-specific ones.
	 * Default is "true"; else, bean-specific interceptors will get applied first.
	 * <p>
	 * 设置是否有共同的拦截器将被应用在指定 bean。默认的返回值是 true，所有，指定 Bean 拦截器将被首先应用
	 */
	public void setApplyCommonInterceptorsFirst(boolean applyCommonInterceptorsFirst) {
		this.applyCommonInterceptorsFirst = applyCommonInterceptorsFirst;
	}

	@Override
	// 设置对应的 BeanFactory
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * Return the owning {@link BeanFactory}.
	 * <p>
	 * 返回这个自定义 BeanFactory
	 * May be {@code null}, as this post-processor doesn't need to belong to a bean factory.
	 * <p>
	 * 可能返回为空，作为这个 post-processor 不需要属于这个  BeanFactory
	 */
	@Nullable
	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	@Override
	@Nullable
	// 预测 Bean 类型
	public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
		// 如果代理类型为空，返回 null
		if (this.proxyTypes.isEmpty()) {
			return null;
		}
		// 获取缓存的  key
		Object cacheKey = getCacheKey(beanClass, beanName);
		// 获取代理类型
		return this.proxyTypes.get(cacheKey);
	}

	@Override
	@Nullable
	// 决定待使用的构造方法
	public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) {
		return null;
	}

	@Override
	// 获取提前被创建的 Bean 引用
	public Object getEarlyBeanReference(Object bean, String beanName) {
		// 获取对应的 缓存 key
		Object cacheKey = getCacheKey(bean.getClass(), beanName);
		// 加入提前被创建的 Bean 对象
		this.earlyProxyReferences.put(cacheKey, bean);
		return wrapIfNecessary(bean, beanName, cacheKey);
	}

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
		// 获取对应的缓存 key
		Object cacheKey = getCacheKey(beanClass, beanName);
		// 如果 beanName 没有长度，并且列表中不包含 bean 名称
		if (!StringUtils.hasLength(beanName) || !this.targetSourcedBeans.contains(beanName)) {
			// 如果 adviseBeans 中不包含缓存，则返回为空
			if (this.advisedBeans.containsKey(cacheKey)) {
				return null;
			}
			// 是基础类型的类或者应该忽略
			if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
				// 设置 advisedBeans 中缓存 key 为否
				this.advisedBeans.put(cacheKey, Boolean.FALSE);
				return null;
			}
		}

		// Create proxy here if we have a custom TargetSource.
		// 如果我们有用户的 TargetSource
		// Suppresses unnecessary default instantiation of the target bean:
		// 抑制目标 bean 的不必要的默认实例
		// The TargetSource will handle target instances in a custom fashion.
		// 这个 TargetSource 将被处理目标实例在用户
		// 获取对应的目标实例
		TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
		// 如果 targetSource 不为空
		if (targetSource != null) {
			// bean 名称有长度
			if (StringUtils.hasLength(beanName)) {
				// 将名称加入 targetSourcedBeans
				this.targetSourcedBeans.add(beanName);
			}
			// 获取指定的拦截器
			Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
			// 床架代理对象
			Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
			// 推入代理类型
			this.proxyTypes.put(cacheKey, proxy.getClass());
			// 返回代理对象
			return proxy;
		}
		// 返回为空
		return null;
	}

	@Override
	public boolean postProcessAfterInstantiation(Object bean, String beanName) {
		return true;
	}

	@Override
	// 属性值执行
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
		return pvs;
	}

	@Override
	// 前置处理器
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	/**
	 * Create a proxy with the configured interceptors if the bean is
	 * identified as one to proxy by the subclass.
	 * <p>
	 * 创建一个代理在配置的拦截。如果这个 bean 是确定的，作为一个现代从子类
	 *
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	@Override
	public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) {
		// 如果 bean 不为空
		if (bean != null) {
			// 获取缓存的 key
			Object cacheKey = getCacheKey(bean.getClass(), beanName);
			// 如果提前创建的对象中没有 bean
			if (this.earlyProxyReferences.remove(cacheKey) != bean) {
				// 对 bean 进行包装
				return wrapIfNecessary(bean, beanName, cacheKey);
			}
		}
		// 返回对应的 bean
		return bean;
	}


	/**
	 * Build a cache key for the given bean class and bean name.
	 * <p>
	 * // 根据给定的类名和 bean 名称获取缓存的 key
	 * <p>Note: As of 4.2.3, this implementation does not return a concatenated
	 * class/name String anymore but rather the most efficient cache key possible:
	 * a plain bean name, prepended with {@link BeanFactory#FACTORY_BEAN_PREFIX}
	 * in case of a {@code FactoryBean}; or if no bean name specified, then the
	 * given bean {@code Class} as-is.
	 *
	 * @param beanClass the bean class
	 * @param beanName  the bean name
	 * @return the cache key for the given class and name
	 */
	protected Object getCacheKey(Class<?> beanClass, @Nullable String beanName) {
		// 如果 bean 名称有长度
		if (StringUtils.hasLength(beanName)) {
			// 如果是可分配的，则返回前缀 +  bean 名称，否则直接返回 beqn 名称
			return (FactoryBean.class.isAssignableFrom(beanClass) ?
					BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
		} else {
			// 否则返回 类名
			return beanClass;
		}
	}

	/**
	 * Wrap the given bean if necessary, i.e. if it is eligible for being proxied.
	 * <p>
	 * 如果有必要包装给定的 bean. 如果这个 bean 是有资格被代理的
	 *
	 * @param bean     the raw bean instance
	 * @param beanName the name of the bean
	 * @param cacheKey the cache key for metadata access
	 * @return a proxy wrapping the bean, or the raw bean instance as-is
	 */
	protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
		// 如果 bean 名称并且 目标资源包含 bean 名称
		if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
			// 直接返回 bean
			return bean;
		}
		// 如果是忽略的对象，直接返回 bean
		if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
			return bean;
		}

		// 如果是基础的类或者应该被忽略，则将 bean 传入没有通知的对象中。并且直接返回 bean
		if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
			this.advisedBeans.put(cacheKey, Boolean.FALSE);
			return bean;
		}

		// Create proxy if we have advice.
		// 如果我们设置了通知则创建代理
		// 获取通知特殊的拦截器
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
		// 如果拦截器不为 没有设置代理
		if (specificInterceptors != DO_NOT_PROXY) {
			// 设置 bean 设置了动态代理
			this.advisedBeans.put(cacheKey, Boolean.TRUE);
			// 创建对应的代理对象
			Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
			// 是指缓存代理的类型
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}
		// 设置为不设置带路，并且直接返回
		this.advisedBeans.put(cacheKey, Boolean.FALSE);
		return bean;
	}

	/**
	 * Return whether the given bean class represents an infrastructure class
	 * that should never be proxied.
	 * <p>
	 * 返回给定的类是否是基础类，则不需要被代理
	 * <p>The default implementation considers Advices, Advisors and
	 * AopInfrastructureBeans as infrastructure classes.
	 *
	 * @param beanClass the class of the bean
	 * @return whether the bean represents an infrastructure class
	 * @see org.aopalliance.aop.Advice
	 * @see org.springframework.aop.Advisor
	 * @see org.springframework.aop.framework.AopInfrastructureBean
	 * @see #shouldSkip
	 */
	protected boolean isInfrastructureClass(Class<?> beanClass) {
		// 根据 bean 判断是否是可分配的
		boolean retVal = Advice.class.isAssignableFrom(beanClass) ||
				Pointcut.class.isAssignableFrom(beanClass) ||
				Advisor.class.isAssignableFrom(beanClass) ||
				AopInfrastructureBean.class.isAssignableFrom(beanClass);
		if (retVal && logger.isTraceEnabled()) {
			logger.trace("Did not attempt to auto-proxy infrastructure class [" + beanClass.getName() + "]");
		}
		return retVal;
	}

	/**
	 * Subclasses should override this method to return {@code true} if the
	 * given bean should not be considered for auto-proxying by this post-processor.
	 * <p>
	 * 子类应该重写这个方法，如果给定的 bean 类不应该被考虑自动代理在这个 post-processor
	 * <p>Sometimes we need to be able to avoid this happening, e.g. if it will lead to
	 * a circular reference or if the existing target instance needs to be preserved.
	 * This implementation returns {@code false} unless the bean name indicates an
	 * "original instance" according to {@code AutowireCapableBeanFactory} conventions.
	 *
	 * @param beanClass the class of the bean
	 * @param beanName  the name of the bean
	 * @return whether to skip the given bean
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#ORIGINAL_INSTANCE_SUFFIX
	 */
	protected boolean shouldSkip(Class<?> beanClass, String beanName) {
		// 判断是否是原始的示例
		return AutoProxyUtils.isOriginalInstance(beanName, beanClass);
	}

	/**
	 * Create a target source for bean instances. Uses any TargetSourceCreators if set.
	 * Returns {@code null} if no custom TargetSource should be used.
	 * <p>
	 * 创建一个目标资源对于 bean 实例，如果设置了使用任何一个 TargetSourceCreators。如果没有用户自定义 TargetSource 应该被使用则返回 null
	 * <p>This implementation uses the "customTargetSourceCreators" property.
	 * Subclasses can override this method to use a different mechanism.
	 * <p>
	 * 这个实现使用这个 cuetomTargetSourceCreators 属性。子类可以重写这个方法适用不同的机制
	 *
	 * @param beanClass the class of the bean to create a TargetSource for
	 * @param beanName  the name of the bean
	 * @return a TargetSource for this bean
	 * @see #setCustomTargetSourceCreators
	 */
	@Nullable
	protected TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
		// We can't create fancy target sources for directly registered singletons.
		// 我们无法为直接注册的单例创建奇特的目标源。
		// 如果  customTargetSourceCreators 并且 beanFactory 不为空并且不包含 bean 名称。
		if (this.customTargetSourceCreators != null &&
				this.beanFactory != null && this.beanFactory.containsBean(beanName)) {
			// 循环 customTargetSourceCreators
			for (TargetSourceCreator tsc : this.customTargetSourceCreators) {
				// 获取对应的目标资源
				TargetSource ts = tsc.getTargetSource(beanClass, beanName);
				// 如果目标资源不为空
				if (ts != null) {
					// Found a matching TargetSource.
					if (logger.isTraceEnabled()) {
						logger.trace("TargetSourceCreator [" + tsc +
								"] found custom TargetSource for bean with name '" + beanName + "'");
					}
					// 直接返回
					return ts;
				}
			}
		}

		// No custom TargetSource found.
		// 没有发现 TargetSource， 则返回为空
		return null;
	}

	/**
	 * Create an AOP proxy for the given bean.
	 * <p>
	 * 从给定的 bean 创建 AOP 代理
	 *
	 * @param beanClass            the class of the bean
	 * @param beanName             the name of the bean
	 * @param specificInterceptors the set of interceptors that is
	 *                             specific to this bean (may be empty, but not null)
	 * @param targetSource         the TargetSource for the proxy,
	 *                             already pre-configured to access the bean
	 * @return the AOP proxy for the bean
	 * @see #buildAdvisors
	 */
	protected Object createProxy(Class<?> beanClass, @Nullable String beanName,
								 @Nullable Object[] specificInterceptors, TargetSource targetSource) {

		// 如果 beanFactory 是ConfigurableListableBeanFactory 实例
		if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
			// 暴露目标实例
			AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);
		}
		// 创建代理工厂
		ProxyFactory proxyFactory = new ProxyFactory();
		// 代理工厂从这个类拷贝
		proxyFactory.copyFrom(this);
		// 如果代理工厂不是代理的目标工程
		if (!proxyFactory.isProxyTargetClass()) {
			// 是否应该代理 bean
			if (shouldProxyTargetClass(beanClass, beanName)) {
				// 代理工厂应该设置代理 目标对象
				proxyFactory.setProxyTargetClass(true);
			} else {
				// 评估代理对象
				evaluateProxyInterfaces(beanClass, proxyFactory);
			}
		}
		// 构建通知器
		Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
		// 新增通知器
		proxyFactory.addAdvisors(advisors);
		// 设置目标资源
		proxyFactory.setTargetSource(targetSource);
		// 用户自定义代理工厂
		customizeProxyFactory(proxyFactory);
		// 设置是否冻结代理，防止覆盖父类配置被提前冻结
		proxyFactory.setFrozen(this.freezeProxy);
		// 如果通知器是否是预过滤的
		if (advisorsPreFiltered()) {
			// 设置工厂为预过滤
			proxyFactory.setPreFiltered(true);
		}
		// 获取动态代理对象
		return proxyFactory.getProxy(getProxyClassLoader());
	}

	/**
	 * Determine whether the given bean should be proxied with its target class rather than its interfaces.
	 * <p>Checks the {@link AutoProxyUtils#PRESERVE_TARGET_CLASS_ATTRIBUTE "preserveTargetClass" attribute}
	 * of the corresponding bean definition.
	 * <p>
	 * 表示给定的 bean 是否应该被代理他的目标类而不是他的接口
	 *
	 * @param beanClass the class of the bean
	 * @param beanName  the name of the bean
	 * @return whether the given bean should be proxied with its target class
	 * @see AutoProxyUtils#shouldProxyTargetClass
	 */
	protected boolean shouldProxyTargetClass(Class<?> beanClass, @Nullable String beanName) {
		// 如果 beanFactory 是 ConfigurableListableBeanFactory, 并且应该是代理的目标类
		return (this.beanFactory instanceof ConfigurableListableBeanFactory &&
				AutoProxyUtils.shouldProxyTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName));
	}

	/**
	 * Return whether the Advisors returned by the subclass are pre-filtered
	 * to match the bean's target class already, allowing the ClassFilter check
	 * to be skipped when building advisors chains for AOP invocations.
	 * <p>
	 * 返回子类是否应该预处理目的是匹配这个 bean 的目标类，允许这个 ClassFilter，检查是否忽略当构建通知器链对于 Aop 执行器
	 * <p>Default is {@code false}. Subclasses may override this if they
	 * will always return pre-filtered Advisors.
	 * <p>
	 * 默认是 false，子类可以重写这个方法
	 *
	 * @return whether the Advisors are pre-filtered
	 * @see #getAdvicesAndAdvisorsForBean
	 * @see org.springframework.aop.framework.Advised#setPreFiltered
	 */
	protected boolean advisorsPreFiltered() {
		return false;
	}

	/**
	 * Determine the advisors for the given bean, including the specific interceptors
	 * as well as the common interceptor, all adapted to the Advisor interface.
	 * <p>
	 * 从给定的 bean 表示这个通知器，包括这个指定的拦截器, 也可以是共同的拦截器。所有 Advisor 接口都适配
	 *
	 * @param beanName             the name of the bean
	 * @param specificInterceptors the set of interceptors that is
	 *                             specific to this bean (may be empty, but not null)
	 * @return the list of Advisors for the given bean
	 */
	protected Advisor[] buildAdvisors(@Nullable String beanName, @Nullable Object[] specificInterceptors) {
		// Handle prototypes correctly...
		// 处理正确的原型模式
		// 获取共同的拦截器
		Advisor[] commonInterceptors = resolveInterceptorNames();
		// 定义拦截器列表
		List<Object> allInterceptors = new ArrayList<>();
		// 指定拦截器不为空
		if (specificInterceptors != null) {
			// 加入指定拦截器
			allInterceptors.addAll(Arrays.asList(specificInterceptors));
			// 如果共同拦截器
			if (commonInterceptors.length > 0) {
				// 是否优先应用共同的拦截器
				if (this.applyCommonInterceptorsFirst) {
					// 加入拦截器列表（放置最前面）
					allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
				} else {
					// 添加全部的拦截器
					allInterceptors.addAll(Arrays.asList(commonInterceptors));
				}
			}
		}
		if (logger.isTraceEnabled()) {
			int nrOfCommonInterceptors = commonInterceptors.length;
			int nrOfSpecificInterceptors = (specificInterceptors != null ? specificInterceptors.length : 0);
			logger.trace("Creating implicit proxy for bean '" + beanName + "' with " + nrOfCommonInterceptors +
					" common interceptors and " + nrOfSpecificInterceptors + " specific interceptors");
		}
		// 定义拦截器数组
		Advisor[] advisors = new Advisor[allInterceptors.size()];
		// 注册拦截器
		for (int i = 0; i < allInterceptors.size(); i++) {
			advisors[i] = this.advisorAdapterRegistry.wrap(allInterceptors.get(i));
		}
		// 返回所有的拦截器
		return advisors;
	}

	/**
	 * Resolves the specified interceptor names to Advisor objects.
	 * <p>
	 * 解决指定拦截器名称，根据 Advisor 对象
	 *
	 * @see #setInterceptorNames
	 */
	private Advisor[] resolveInterceptorNames() {
		// 获取对应的 beanFactory
		BeanFactory bf = this.beanFactory;
		// 如果是 ConfigurableBeanFactory, 则强转类型，否则返回为空
		ConfigurableBeanFactory cbf = (bf instanceof ConfigurableBeanFactory ? (ConfigurableBeanFactory) bf : null);
		// 定义拦截器列表
		List<Advisor> advisors = new ArrayList<>();
		// 循环遍历拦截器名称
		for (String beanName : this.interceptorNames) {
			// 如果当前的拦截器为空，并且当前的拦截器不在创建
			if (cbf == null || !cbf.isCurrentlyInCreation(beanName)) {
				// 断言 beanFactory
				Assert.state(bf != null, "BeanFactory required for resolving interceptor names");
				// 获取 bean
				Object next = bf.getBean(beanName);
				// 注册拦截器，并且加入 Advisor 中
				advisors.add(this.advisorAdapterRegistry.wrap(next));
			}
		}
		// 将拦截器转为列表
		return advisors.toArray(new Advisor[0]);
	}

	/**
	 * Subclasses may choose to implement this: for example,
	 * to change the interfaces exposed.
	 * <p>
	 * 子类可以选择实现：例如 改变这个接口扩展
	 * <p>The default implementation is empty.
	 *
	 * @param proxyFactory a ProxyFactory that is already configured with
	 *                     TargetSource and interfaces and will be used to create the proxy
	 *                     immediately after this method returns
	 */
	protected void customizeProxyFactory(ProxyFactory proxyFactory) {
	}


	/**
	 * Return whether the given bean is to be proxied, what additional
	 * advices (e.g. AOP Alliance interceptors) and advisors to apply.
	 * <p>
	 * 根据给定的 bean 是否被代理，产生额外的通知和应用通知器
	 *
	 * @param beanClass          the class of the bean to advise
	 * @param beanName           the name of the bean
	 * @param customTargetSource the TargetSource returned by the
	 *                           {@link #getCustomTargetSource} method: may be ignored.
	 *                           Will be {@code null} if no custom target source is in use.
	 * @return an array of additional interceptors for the particular bean;
	 * or an empty array if no additional interceptors but just the common ones;
	 * or {@code null} if no proxy at all, not even with the common interceptors.
	 * See constants DO_NOT_PROXY and PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS.
	 * @throws BeansException in case of errors
	 * @see #DO_NOT_PROXY
	 * @see #PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
	 */
	@Nullable
	protected abstract Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName,
															 @Nullable TargetSource customTargetSource) throws BeansException;

}
