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

package org.springframework.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.context.ApplicationListener} decorator that filters
 * events from a specified event source, invoking its delegate listener for
 * matching {@link org.springframework.context.ApplicationEvent} objects only.
 * <p>
 * 实现将原始对象触发的事件，转发给指定监听器
 *
 * <p>Can also be used as base class, overriding the {@link #onApplicationEventInternal}
 * method instead of specifying a delegate listener.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.0.5
 */
public class SourceFilteringListener implements GenericApplicationListener, SmartApplicationListener {
	/**
	 * 原始类
	 */
	private final Object source;

	@Nullable
	/**
	 * 代理的监听器
	 */
	private GenericApplicationListener delegate;


	/**
	 * Create a SourceFilteringListener for the given event source.
	 * <p>
	 * 从给定的参数创建一个信息的 SourceFilteringListener
	 *
	 * @param source   the event source that this listener filters for,
	 *                 only processing events from this source
	 *                 监听器过滤器的事件资源，仅仅加工这个时间从这个资源
	 * @param delegate the delegate listener to invoke with event
	 *                 from the specified source
	 *                 <p>
	 *                 代理事件监听器目的是为了调用这个时间从指定的资源
	 */
	public SourceFilteringListener(Object source, ApplicationListener<?> delegate) {
		// 定义资源
		this.source = source;
		// 获取 GenericApplicationListener
		this.delegate = (delegate instanceof GenericApplicationListener ?
				(GenericApplicationListener) delegate : new GenericApplicationListenerAdapter(delegate));
	}

	/**
	 * Create a SourceFilteringListener for the given event source,
	 * expecting subclasses to override the {@link #onApplicationEventInternal}
	 * method (instead of specifying a delegate listener).
	 * <p>
	 * 根据给定的资源创建一个新的 SourceFilteringListener, 期待子类重写  {@link #onApplicationEventInternal} 方法，替代指定的替代的监听器
	 *
	 * @param source the event source that this listener filters for,
	 *               only processing events from this source
	 */
	protected SourceFilteringListener(Object source) {
		this.source = source;
	}


	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event.getSource() == this.source) { // 判定来源
			// 处理时间
			onApplicationEventInternal(event);
		}
	}

	@Override
	public boolean supportsEventType(ResolvableType eventType) {
		// 是否支持对应的事件类型
		return (this.delegate == null || this.delegate.supportsEventType(eventType));
	}

	@Override
	// 是否支持的时间类型
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return supportsEventType(ResolvableType.forType(eventType));
	}

	@Override
	// 是否自持资源类型
	public boolean supportsSourceType(@Nullable Class<?> sourceType) {
		return (sourceType != null && sourceType.isInstance(this.source));
	}

	@Override
	// 获取指定的排序
	public int getOrder() {
		return (this.delegate != null ? this.delegate.getOrder() : Ordered.LOWEST_PRECEDENCE);
	}


	/**
	 * Actually process the event, after having filtered according to the
	 * desired event source already.
	 * <p>
	 * 实际处理这个时间，在根据想要的时间资源处理之后
	 * <p>The default implementation invokes the specified delegate, if any.
	 *
	 * @param event the event to process (matching the specified source)
	 */
	protected void onApplicationEventInternal(ApplicationEvent event) {
		if (this.delegate == null) {
			throw new IllegalStateException(
					"Must specify a delegate object or override the onApplicationEventInternal method");
		}
		// 执行监听器
		this.delegate.onApplicationEvent(event);
	}

}
