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

package org.springframework.aop.framework;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionAwareMethodMatcher;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple but definitive way of working out an advice chain for a Method,
 * given an {@link Advised} object. Always rebuilds each advice chain;
 * caching can be provided by subclasses.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0.3
 */
@SuppressWarnings("serial")
public class DefaultAdvisorChainFactory implements AdvisorChainFactory, Serializable {

	@Override
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
			Advised config, Method method, @Nullable Class<?> targetClass) {

		// This is somewhat tricky... We have to process introductions first,
		// 这个有点棘手，我们有执行简介，但是我们需要保存排序在最终的列表
		// but we need to preserve order in the ultimate list.
		// 定义一个 GlobalAdvisorAdapteRegistry 注册表
		AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();
		// 获取通知器数组
		Advisor[] advisors = config.getAdvisors();
		// 定义拦截器列表
		List<Object> interceptorList = new ArrayList<>(advisors.length);
		// 定义实际的类类型，如果目标类不为空，直接返回目标类，否则返回方法的声明类型
		Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
		// 是否有简介
		Boolean hasIntroductions = null;
		// 循环遍历通知器
		for (Advisor advisor : advisors) {
			// 如果通知器是 PointcutAdvisor
			if (advisor instanceof PointcutAdvisor) {
				// Add it conditionally.
				// 新增它的条件
				// 强转类型
				PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
				// 如果配置是前置过滤的或者 通知器的 pointcut 可以匹配到对应的类
				if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
					// 获取对应的方法匹配器
					MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
					// 是否匹配
					boolean match;
					// 如果对应的方法匹配器是简介 Aware 方法匹配器
					if (mm instanceof IntroductionAwareMethodMatcher) {
						// 如果简介为空
						if (hasIntroductions == null) {
							// 获取是否有匹配简介
							hasIntroductions = hasMatchingIntroductions(advisors, actualClass);
						}
						// 是否可以匹配成功对应的方法
						match = ((IntroductionAwareMethodMatcher) mm).matches(method, actualClass, hasIntroductions);
					} else {
						// 是否可以匹配成功对应的方法
						match = mm.matches(method, actualClass);
					}
					if (match) {
						// 获取对应的注册拦截表
						MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
						// 如果方法拦截器是运行时
						if (mm.isRuntime()) {
							// Creating a new object instance in the getInterceptors() method
							// 创建一个新的对象实例在这个 Interceptors 方法
							// isn't a problem as we normally cache created chains.
							// 作为我们常用的缓存创建链不是一个问题
							for (MethodInterceptor interceptor : interceptors) {
								interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
							}
						} else {
							// 获取对应的拦截器
							interceptorList.addAll(Arrays.asList(interceptors));
						}
					}
				}
			} else if (advisor instanceof IntroductionAdvisor) {
				// 获取对应的通知器
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				// 如果配置了前置过滤器，并且可以匹配上对应的过滤器
				if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
					// 获取对应的拦截器
					Interceptor[] interceptors = registry.getInterceptors(advisor);
					// 加入全部的拦截器
					interceptorList.addAll(Arrays.asList(interceptors));
				}
			} else {
				// 获取对应的拦截器数组
				Interceptor[] interceptors = registry.getInterceptors(advisor);
				// 加入全部的拦截器
				interceptorList.addAll(Arrays.asList(interceptors));
			}
		}

		return interceptorList;
	}

	/**
	 * Determine whether the Advisors contain matching introductions.
	 */
	private static boolean hasMatchingIntroductions(Advisor[] advisors, Class<?> actualClass) {
		for (Advisor advisor : advisors) {
			if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (ia.getClassFilter().matches(actualClass)) {
					return true;
				}
			}
		}
		return false;
	}

}
