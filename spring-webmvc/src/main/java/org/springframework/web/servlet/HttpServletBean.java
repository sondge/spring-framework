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

package org.springframework.web.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.context.support.StandardServletEnvironment;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple extension of {@link javax.servlet.http.HttpServlet} which treats
 * its config parameters ({@code init-param} entries within the
 * {@code servlet} tag in {@code web.xml}) as bean properties.
 * <p>
 * HttpServlet 的简单扩展 对待配置参数实体包含 servlet 标签在 web.xml 作为 beanProperties
 *
 * <p>A handy superclass for any type of servlet. Type conversion of config
 * parameters is automatic, with the corresponding setter method getting
 * invoked with the converted value. It is also possible for subclasses to
 * specify required properties. Parameters without matching bean property
 * setter will simply be ignored.
 * <p>
 * 一个便利的父类对于任何类型的 servlet。类型转化的配置参数时自动化的，以及相应的设置方法和获取方法调用在这个转化值。他也是可能的对于子类指定必须的属性
 * 参数没有配 Bean 属性设置，将被简单的忽略
 *
 * <p>This servlet leaves request handling to subclasses, inheriting the default
 * behavior of HttpServlet ({@code doGet}, {@code doPost}, etc).
 *
 * <p>This generic servlet base class has no dependency on the Spring
 * {@link org.springframework.context.ApplicationContext} concept. Simple
 * servlets usually don't load their own context but rather access service
 * beans from the Spring root application context, accessible via the
 * filter's {@link #getServletContext() ServletContext} (see
 * {@link org.springframework.web.context.support.WebApplicationContextUtils}).
 *
 * <p>The {@link FrameworkServlet} class is a more specific servlet base
 * class which loads its own application context. FrameworkServlet serves
 * as direct base class of Spring's full-fledged {@link DispatcherServlet}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #addRequiredProperty
 * @see #initServletBean
 * @see #doGet
 * @see #doPost
 */
@SuppressWarnings("serial")
public abstract class HttpServletBean extends HttpServlet implements EnvironmentCapable, EnvironmentAware {

	/**
	 * Logger available to subclasses.
	 */
	/* 日志信息 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	/* 配置环境 */
	private ConfigurableEnvironment environment;

	/* 必要的配置属性 */
	private final Set<String> requiredProperties = new HashSet<>(4);


	/**
	 * Subclasses can invoke this method to specify that this property
	 * (which must match a JavaBean property they expose) is mandatory,
	 * and must be supplied as a config parameter. This should be called
	 * from the constructor of a subclass.
	 * <p>
	 * 子类可以调用这个方法目的是指定这个属性强制的
	 * <p>This method is only relevant in case of traditional initialization
	 * driven by a ServletConfig instance.
	 *
	 * @param property name of the required property
	 */
	protected final void addRequiredProperty(String property) {
		this.requiredProperties.add(property);
	}

	/**
	 * Set the {@code Environment} that this servlet runs in.
	 * <p>Any environment set here overrides the {@link StandardServletEnvironment}
	 * provided by default.
	 * <p>
	 * 设置 servlet 运行时的环境
	 *
	 * @throws IllegalArgumentException if environment is not assignable to
	 *                                  {@code ConfigurableEnvironment}
	 */
	@Override
	public void setEnvironment(Environment environment) {
		// 断言是否是对应的示例
		Assert.isInstanceOf(ConfigurableEnvironment.class, environment, "ConfigurableEnvironment required");
		this.environment = (ConfigurableEnvironment) environment;
	}

	/**
	 * Return the {@link Environment} associated with this servlet.
	 * <p>
	 * 返回和这个 Servlet 相互联系的环境
	 * <p>If none specified, a default environment will be initialized via
	 * {@link #createEnvironment()}.
	 */
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * Create and return a new {@link StandardServletEnvironment}.
	 * <p>
	 * 创建一个并且返回一个信息 StandardServletEnvironment
	 * <p>Subclasses may override this in order to configure the environment or
	 * specialize the environment type returned.
	 * <p>
	 * 子类可以覆盖方法为了配置环境或者指定环境类型返回
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * Map config parameters onto bean properties of this servlet, and
	 * invoke subclass initialization.
	 * <p>
	 * 可以配置参数在 BeanProperties 在这个 servlet 和调用子类初始化
	 *
	 * @throws ServletException if bean properties are invalid (or required
	 *                          properties are missing), or if subclass initialization fails.
	 */
	@Override
	public final void init() throws ServletException {

		// Set bean properties from init parameters.
		// 获取初始化参数的配置
		PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
		// 如果配置为空
		if (!pvs.isEmpty()) {
			try {
				// 获取包装类
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
				// 加载对应的 Servlet 资源加载器
				ResourceLoader resourceLoader = new ServletContextResourceLoader(getServletContext());
				// 注册用户自定义编辑器
				bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));
				// 初始化 Bean 包装类
				initBeanWrapper(bw);
				// 设置对应的属性
				bw.setPropertyValues(pvs, true);
			} catch (BeansException ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Failed to set bean properties on servlet '" + getServletName() + "'", ex);
				}
				throw ex;
			}
		}

		// Let subclasses do whatever initialization they like.
		initServletBean();
	}

	/**
	 * Initialize the BeanWrapper for this HttpServletBean,
	 * possibly with custom editors.
	 * <p>
	 * 初始化这个 Bean 包装类在 HttpServletBean 可能是用户自定编辑
	 * <p>This default implementation is empty.
	 *
	 * @param bw the BeanWrapper to initialize
	 * @throws BeansException if thrown by BeanWrapper methods
	 * @see org.springframework.beans.BeanWrapper#registerCustomEditor
	 */
	protected void initBeanWrapper(BeanWrapper bw) throws BeansException {
	}

	/**
	 * Subclasses may override this to perform custom initialization.
	 * All bean properties of this servlet will have been set before this
	 * method is invoked.
	 * <p>
	 * 这个 Servlet 将被设置在这个方法被调用之前的所有的 Bean 属性
	 * <p>This default implementation is empty.
	 *
	 * @throws ServletException if subclass initialization fails
	 */
	protected void initServletBean() throws ServletException {
	}

	/**
	 * Overridden method that simply returns {@code null} when no ServletConfig set yet.
	 * <p>
	 * 置ServletConfig时，仅返回{@code null}的重写方法
	 *
	 * @see #getServletConfig()
	 */
	@Override
	@Nullable
	public String getServletName() {
		return (getServletConfig() != null ? getServletConfig().getServletName() : null);
	}


	/**
	 * PropertyValues implementation created from ServletConfig init parameters.
	 * 属性值实现创建从这个 ServletConfig 初始化参数
	 */
	private static class ServletConfigPropertyValues extends MutablePropertyValues {

		/**
		 * Create new ServletConfigPropertyValues.
		 * <p>
		 * 创建新的 ServletConfigPropertyValues
		 *
		 * @param config             the ServletConfig we'll use to take PropertyValues from
		 * @param requiredProperties set of property names we need, where
		 *                           we can't accept default values
		 * @throws ServletException if any required properties are missing
		 */
		public ServletConfigPropertyValues(ServletConfig config, Set<String> requiredProperties)
				throws ServletException {
			// 获取对应的必要的 Properties
			Set<String> missingProps = (!CollectionUtils.isEmpty(requiredProperties) ?
					new HashSet<>(requiredProperties) : null);
			// 获取参数名称的集合
			Enumeration<String> paramNames = config.getInitParameterNames();
			// 如果还有参数
			while (paramNames.hasMoreElements()) {
				// 获取对应的属性
				String property = paramNames.nextElement();
				// 获取初始化值
				Object value = config.getInitParameter(property);
				// 加入对应的属性中
				addPropertyValue(new PropertyValue(property, value));
				// 如果 missingProps 不为空
				if (missingProps != null) {
					// 删除对应的属性信息
					missingProps.remove(property);
				}
			}

			// Fail if we are still missing properties.
			// 如果我们仍然缺少属性，则失败。
			if (!CollectionUtils.isEmpty(missingProps)) {
				throw new ServletException(
						"Initialization from ServletConfig for servlet '" + config.getServletName() +
								"' failed; the following required properties were missing: " +
								StringUtils.collectionToDelimitedString(missingProps, ", "));
			}
		}
	}

}
