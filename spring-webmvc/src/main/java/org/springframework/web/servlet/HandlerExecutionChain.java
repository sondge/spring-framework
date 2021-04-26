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

package org.springframework.web.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler execution chain, consisting of handler object and any handler interceptors.
 * <p>
 * 处理器执行链，有处理对象和处理拦截器组成
 * Returned by HandlerMapping's {@link HandlerMapping#getHandler} method.
 *
 * @author Juergen Hoeller
 * @see HandlerInterceptor
 * @since 20.06.2003
 */
public class HandlerExecutionChain {

	private static final Log logger = LogFactory.getLog(HandlerExecutionChain.class);
	// 处理器
	private final Object handler;

	@Nullable
	// 处理拦截器数组
	private HandlerInterceptor[] interceptors;

	@Nullable
	// 处理拦截器列表
	/**
	 *
	 * 在实际使用时，会调用 {@link #getInterceptors()}方法，初始化到 #{@link #interceptors}
	 */
	private List<HandlerInterceptor> interceptorList;
	/**
	 * 已执行 {@link HandlerInterceptor#preHandle(HttpServletRequest, HttpServletResponse, Object)} 的位置
	 * 主要用于实现 {@link #applyPostHandle(HttpServletRequest, HttpServletResponse, ModelAndView)} 的逻辑
	 */
	private int interceptorIndex = -1;


	/**
	 * Create a new HandlerExecutionChain.
	 * 创建一个新的 HandlerExecutionChain
	 *
	 * @param handler the handler object to execute
	 */
	public HandlerExecutionChain(Object handler) {
		this(handler, (HandlerInterceptor[]) null);
	}

	/**
	 * Create a new HandlerExecutionChain.
	 * 创建一个新的 HandlerExecutionChain
	 *
	 * @param handler      the handler object to execute
	 * @param interceptors the array of interceptors to apply
	 *                     (in the given order) before the handler itself executes
	 */
	public HandlerExecutionChain(Object handler, @Nullable HandlerInterceptor... interceptors) {

		if (handler instanceof HandlerExecutionChain) {
			HandlerExecutionChain originalChain = (HandlerExecutionChain) handler;
			this.handler = originalChain.getHandler();
			this.interceptorList = new ArrayList<>();
			CollectionUtils.mergeArrayIntoCollection(originalChain.getInterceptors(), this.interceptorList);
			CollectionUtils.mergeArrayIntoCollection(interceptors, this.interceptorList);
		} else {
			this.handler = handler;
			this.interceptors = interceptors;
		}
	}


	/**
	 * Return the handler object to execute.
	 * 返回执行器链的处理器
	 */
	public Object getHandler() {
		return this.handler;
	}

	/**
	 * Add the given interceptor to the end of this chain.
	 * <p>
	 * 根据给定的拦截器加入新的 chain
	 */
	public void addInterceptor(HandlerInterceptor interceptor) {
		initInterceptorList().add(interceptor);
	}

	/**
	 * Add the given interceptor at the specified index of this chain.
	 * <p>
	 * 将给定的拦截器按照索引位置加入到拦截器链中
	 *
	 * @since 5.2
	 */
	public void addInterceptor(int index, HandlerInterceptor interceptor) {
		initInterceptorList().add(index, interceptor);
	}

	/**
	 * Add the given interceptors to the end of this chain.
	 * 将给定的拦截器数组加入	到这个 链中
	 */
	public void addInterceptors(HandlerInterceptor... interceptors) {
		if (!ObjectUtils.isEmpty(interceptors)) {
			CollectionUtils.mergeArrayIntoCollection(interceptors, initInterceptorList());
		}
	}

	private List<HandlerInterceptor> initInterceptorList() {
		// 如果 拦截器列表为空，则初始化为 ArrayList
		if (this.interceptorList == null) {
			this.interceptorList = new ArrayList<>();
			// 如果 interceptors 不为空，则将 interceptors 中的元素加入 interceptorList 中
			if (this.interceptors != null) {
				// An interceptor array specified through the constructor
				CollectionUtils.mergeArrayIntoCollection(this.interceptors, this.interceptorList);
			}
		}
		// 置空 interceptors
		this.interceptors = null;
		// 返回 interceptorList
		return this.interceptorList;
	}

	/**
	 * Return the array of interceptors to apply (in the given order).
	 * <p>
	 * 获得拦截器数组列表
	 *
	 * @return the array of HandlerInterceptors instances (may be {@code null})
	 */
	@Nullable
	public HandlerInterceptor[] getInterceptors() {
		// 将 interceptors 初始化到 interceptors 中
		if (this.interceptors == null && this.interceptorList != null) {
			this.interceptors = this.interceptorList.toArray(new HandlerInterceptor[0]);
		}
		return this.interceptors;
	}


	/**
	 * Apply preHandle methods of registered interceptors.
	 * <p>
	 * 应用前置拦截器处理
	 *
	 * @return {@code true} if the execution chain should proceed with the
	 * next interceptor or the handler itself. Else, DispatcherServlet assumes
	 * that this interceptor has already dealt with the response itself.
	 */
	boolean applyPreHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
		// 获取当前的拦截器数组
		HandlerInterceptor[] interceptors = getInterceptors();
		// 如果拦截器不为空
		if (!ObjectUtils.isEmpty(interceptors)) {
			// 循环遍历拦截器数组
			for (int i = 0; i < interceptors.length; i++) {
				HandlerInterceptor interceptor = interceptors[i];
				// 是佛做了前置处理
				if (!interceptor.preHandle(request, response, this.handler)) {
					// 触发已完成处理
					triggerAfterCompletion(request, response, null);
					// 返回 false 前置处理失败
					return false;
				}
				// 标记 interceptor 拦截器的位置
				this.interceptorIndex = i;
			}
		}
		// 返回 true，前置处理成功
		return true;
	}

	/**
	 * Apply postHandle methods of registered interceptors.
	 *
	 * 应用拦截器的 postHandler
	 */
	void applyPostHandle(HttpServletRequest request, HttpServletResponse response, @Nullable ModelAndView mv)
			throws Exception {
		// 获取全部的 HandlerInterceotor 数组
		HandlerInterceptor[] interceptors = getInterceptors();
		if (!ObjectUtils.isEmpty(interceptors)) {
			for (int i = interceptors.length - 1; i >= 0; i--) {
				HandlerInterceptor interceptor = interceptors[i];
				// 后置处理
				interceptor.postHandle(request, response, this.handler, mv);
			}
		}
	}

	/**
	 * Trigger afterCompletion callbacks on the mapped HandlerInterceptors.
	 * Will just invoke afterCompletion for all interceptors whose preHandle invocation
	 * has successfully completed and returned true.
	 */
	void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response, @Nullable Exception ex)
			throws Exception {
		// 获取拦截器数组
		HandlerInterceptor[] interceptors = getInterceptors();
		if (!ObjectUtils.isEmpty(interceptors)) {
			// 遍历拦截器数组
			for (int i = this.interceptorIndex; i >= 0; i--) {
				HandlerInterceptor interceptor = interceptors[i];
				try {
					// 已完成处理
					interceptor.afterCompletion(request, response, this.handler, ex);
				} catch (Throwable ex2) {
					// 如果执行失败仅仅打印日志不会结束循环
					logger.error("HandlerInterceptor.afterCompletion threw exception", ex2);
				}
			}
		}
	}

	/**
	 * Apply afterConcurrentHandlerStarted callback on mapped AsyncHandlerInterceptors.
	 */
	void applyAfterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response) {
		HandlerInterceptor[] interceptors = getInterceptors();
		if (!ObjectUtils.isEmpty(interceptors)) {
			for (int i = interceptors.length - 1; i >= 0; i--) {
				HandlerInterceptor interceptor = interceptors[i];
				if (interceptor instanceof AsyncHandlerInterceptor) {
					try {
						AsyncHandlerInterceptor asyncInterceptor = (AsyncHandlerInterceptor) interceptor;
						asyncInterceptor.afterConcurrentHandlingStarted(request, response, this.handler);
					} catch (Throwable ex) {
						if (logger.isErrorEnabled()) {
							logger.error("Interceptor [" + interceptor + "] failed in afterConcurrentHandlingStarted", ex);
						}
					}
				}
			}
		}
	}


	/**
	 * Delegates to the handler's {@code toString()} implementation.
	 */
	@Override
	public String toString() {
		Object handler = getHandler();
		StringBuilder sb = new StringBuilder();
		sb.append("HandlerExecutionChain with [").append(handler).append("] and ");
		if (this.interceptorList != null) {
			sb.append(this.interceptorList.size());
		} else if (this.interceptors != null) {
			sb.append(this.interceptors.length);
		} else {
			sb.append(0);
		}
		return sb.append(" interceptors").toString();
	}

}
