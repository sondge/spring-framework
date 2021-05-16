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

package org.springframework.web.method.annotation;

import org.springframework.core.ExceptionDepthComparator;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Discovers {@linkplain ExceptionHandler @ExceptionHandler} methods in a given class,
 * including all of its superclasses, and helps to resolve a given {@link Exception}
 * to the exception types supported by a given {@link Method}.
 * <p>
 * 发现给定类（包括其所有超类）中的@ExceptionHandler方法，
 * 并帮助将给定Exception解析为给定Method支持的异常类型。发现@ExceptionHandler方法在给定的类
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ExceptionHandlerMethodResolver {

	/**
	 * A filter for selecting {@code @ExceptionHandler} methods.
	 *
	 * MethodFilter 对象，用于过滤带有 @ExceptionHandler 方法
	 */
	public static final MethodFilter EXCEPTION_HANDLER_METHODS = method ->
			AnnotatedElementUtils.hasAnnotation(method, ExceptionHandler.class);

	/**
	 * 已经映射方法的缓存
	 */
	private final Map<Class<? extends Throwable>, Method> mappedMethods = new HashMap<>(16);
	/**
	 * 已经匹配的方法
	 */
	private final Map<Class<? extends Throwable>, Method> exceptionLookupCache = new ConcurrentReferenceHashMap<>(16);


	/**
	 * A constructor that finds {@link ExceptionHandler} methods in the given type.
	 *
	 * @param handlerType the type to introspect
	 */
	public ExceptionHandlerMethodResolver(Class<?> handlerType) {
		// 遍历带有 @ExceptionHandler 注解的方法
		for (Method method : MethodIntrospector.selectMethods(handlerType, EXCEPTION_HANDLER_METHODS)) {
			// 遍历所有的异常集合
			for (Class<? extends Throwable> exceptionType : detectExceptionMappings(method)) {
				// 添加到 mappingMethod 中
				addExceptionMapping(exceptionType, method);
			}
		}
	}


	/**
	 * Extract exception mappings from the {@code @ExceptionHandler} annotation first,
	 * and then as a fallback from the method signature itself.
	 *
	 * 提炼异常映射从 @ExceptionHandler 注解，然后作为方法签名的后备
	 */
	@SuppressWarnings("unchecked")
	private List<Class<? extends Throwable>> detectExceptionMappings(Method method) {
		List<Class<? extends Throwable>> result = new ArrayList<>();
		// 首先，从方法上的 @ExceptionHandler 注解中，获得所处理的异常，添加到 result 中
		detectAnnotationExceptionMappings(method, result);
		// 如果获取不到，从方法参数中，获得所处理的异常，添加到 result 中
		if (result.isEmpty()) {
			for (Class<?> paramType : method.getParameterTypes()) {
				if (Throwable.class.isAssignableFrom(paramType)) {
					result.add((Class<? extends Throwable>) paramType);
				}
			}
		}
		// 如果获得不到，则抛出 IllegalStateException 异常
		if (result.isEmpty()) {
			throw new IllegalStateException("No exception types mapped to " + method);
		}
		return result;
	}

	private void detectAnnotationExceptionMappings(Method method, List<Class<? extends Throwable>> result) {
		ExceptionHandler ann = AnnotatedElementUtils.findMergedAnnotation(method, ExceptionHandler.class);
		Assert.state(ann != null, "No ExceptionHandler annotation");
		result.addAll(Arrays.asList(ann.value()));
	}

	private void addExceptionMapping(Class<? extends Throwable> exceptionType, Method method) {
		// 添加到 mappedMethods 中
		Method oldMethod = this.mappedMethods.put(exceptionType, method);
		// 如果已存在，说明冲突，所以抛出 IllegalStateException 异常
		if (oldMethod != null && !oldMethod.equals(method)) {
			throw new IllegalStateException("Ambiguous @ExceptionHandler method mapped for [" +
					exceptionType + "]: {" + oldMethod + ", " + method + "}");
		}
	}

	/**
	 * Whether the contained type has any exception mappings.
	 */
	public boolean hasExceptionMappings() {
		return !this.mappedMethods.isEmpty();
	}

	/**
	 * Find a {@link Method} to handle the given exception.
	 * Use {@link ExceptionDepthComparator} if more than one match is found.
	 *
	 * @param exception the exception
	 * @return a Method to handle the exception, or {@code null} if none found
	 *
	 * 查找一个方法处理异常
	 */
	@Nullable
	public Method resolveMethod(Exception exception) {
		return resolveMethodByThrowable(exception);
	}

	/**
	 * Find a {@link Method} to handle the given Throwable.
	 * Use {@link ExceptionDepthComparator} if more than one match is found.
	 *
	 * @param exception the exception
	 * @return a Method to handle the exception, or {@code null} if none found
	 * @since 5.0
	 */
	@Nullable
	public Method resolveMethodByThrowable(Throwable exception) {
		// 首先，获得异常对应的方法
		Method method = resolveMethodByExceptionType(exception.getClass());
		// 其次，获取不到，则使用异常 cause 对应的方法
		if (method == null) {
			Throwable cause = exception.getCause();
			if (cause != null) {
				method = resolveMethodByExceptionType(cause.getClass());
			}
		}
		return method;
	}

	/**
	 * Find a {@link Method} to handle the given exception type. This can be
	 * useful if an {@link Exception} instance is not available (e.g. for tools).
	 *
	 * @param exceptionType the exception type
	 * @return a Method to handle the exception, or {@code null} if none found
	 */
	@Nullable
	public Method resolveMethodByExceptionType(Class<? extends Throwable> exceptionType) {
		// 首先，先从 exceptionLookupCache 缓存中获得
		Method method = this.exceptionLookupCache.get(exceptionType);
		// 如果缓存中获取不到，则从 mappedMethods 中获得，并添加到 exceptionLookupCache 中
		if (method == null) {
			method = getMappedMethod(exceptionType);
			this.exceptionLookupCache.put(exceptionType, method);
		}
		// 返回对应的方法
		return method;
	}

	/**
	 * Return the {@link Method} mapped to the given exception type, or {@code null} if none.
	 */
	@Nullable
	private Method getMappedMethod(Class<? extends Throwable> exceptionType) {
		// 定义匹配器
		List<Class<? extends Throwable>> matches = new ArrayList<>();
		// 遍历 mappedMethod 缓存
		for (Class<? extends Throwable> mappedException : this.mappedMethods.keySet()) {
			// 如果匹配到结果
			if (mappedException.isAssignableFrom(exceptionType)) {
				// 加入列表中
				matches.add(mappedException);
			}
		}
		if (!matches.isEmpty()) {
			// 对异常进行排序
			matches.sort(new ExceptionDepthComparator(exceptionType));
			// 获取第一个
			return this.mappedMethods.get(matches.get(0));
		} else {
			// 返回 null
			return null;
		}
	}

}
