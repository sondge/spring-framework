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

package org.springframework.beans.factory.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Default implementation of the {@link BeanDefinitionDocumentReader} interface that
 * reads bean definitions according to the "spring-beans" DTD and XSD format
 * (Spring's default XML bean definition format).
 * <p>
 * 从 XML 文件中根据 spring-beans读取 Bean 中默认实现的接口
 *
 * <p>The structure, elements, and attribute names of the required XML document
 * are hard-coded in this class. (Of course a transform could be run if necessary
 * to produce this format). {@code <beans>} does not need to be the root
 * element of the XML document: this class will parse all bean definition elements
 * in the XML file, regardless of the actual root element.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Erik Wiersma
 * @since 18.12.2003
 */
public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {

	// bean 元素
	public static final String BEAN_ELEMENT = BeanDefinitionParserDelegate.BEAN_ELEMENT;
	// 接近 bean 元素
	public static final String NESTED_BEANS_ELEMENT = "beans";
	// alias 标签
	public static final String ALIAS_ELEMENT = "alias";
	// name 标签
	public static final String NAME_ATTRIBUTE = "name";
	// alias 属性
	public static final String ALIAS_ATTRIBUTE = "alias";
	// import 标签
	public static final String IMPORT_ELEMENT = "import";
	// resource 属性
	public static final String RESOURCE_ATTRIBUTE = "resource";
	// profile 属性
	public static final String PROFILE_ATTRIBUTE = "profile";

	// 定义日志信息
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	// 获取 readerContext
	private XmlReaderContext readerContext;

	@Nullable
	// BeanDefinition 解析器
	private BeanDefinitionParserDelegate delegate;


	/**
	 * This implementation parses bean definitions according to the "spring-beans" XSD
	 * (or DTD, historically).
	 * <p>Opens a DOM Document; then initializes the default settings
	 * specified at the {@code <beans/>} level; then parses the contained bean definitions.
	 */
	@Override
	// 注册 BeanDefinition 到 容器中
	public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
		this.readerContext = readerContext;
		// 获得 Document 元素
		// 执行注册 BeanDefinition
		doRegisterBeanDefinitions(doc.getDocumentElement());
	}

	/**
	 * Return the descriptor for the XML resource that this parser works on.
	 * 获取 XmlReaderContext
	 */
	protected final XmlReaderContext getReaderContext() {
		Assert.state(this.readerContext != null, "No XmlReaderContext available");
		return this.readerContext;
	}

	/**
	 * Invoke the {@link org.springframework.beans.factory.parsing.SourceExtractor}
	 * to pull the source metadata from the supplied {@link Element}.
	 * <p>
	 * // 从指定元素中 提取元素据
	 */
	@Nullable
	protected Object extractSource(Element ele) {
		return getReaderContext().extractSource(ele);
	}


	/**
	 * Register each bean definition within the given root {@code <beans/>} element.
	 * 从  root 元素中注册 BeanDefinition 到 IOC  容器中
	 */
	@SuppressWarnings("deprecation")  // for Environment.acceptsProfiles(String...)
	protected void doRegisterBeanDefinitions(Element root) {
		// Any nested <beans> elements will cause recursion in this method. In
		// order to propagate and preserve <beans> default-* attributes correctly,
		// keep track of the current (parent) delegate, which may be null. Create
		// the new (child) delegate with a reference to the parent for fallback purposes,
		// then ultimately reset this.delegate back to its original (parent) reference.
		// this behavior emulates a stack of delegates without actually necessitating one.
		// 记录老的 BeanDefinitionParserDelegate
		BeanDefinitionParserDelegate parent = this.delegate;
		// 创建一个新的 BeanDefinitionParserDelegate 并且设置到 delegate
		this.delegate = createDelegate(getReaderContext(), root, parent);
		// 检查 <bean/> 根标签的命名空间是否为空 或者是 http://www.springframework.org/schema/beans
		if (this.delegate.isDefaultNamespace(root)) {
			// 处理 profileSpec 属性
			String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
			// 判断是否有内容
			if (StringUtils.hasText(profileSpec)) {
				// 使用特殊的分隔符（,;）分隔 ProfileSpec, 可能会有多个 profile
				String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
						profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
				// We cannot use Profiles.of(...) since profile expressions are not supported
				// in XML config. See SPR-12458 for details.
				// 如果所有的 profile 都无效，则不进行注册
				if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipped XML bean definition file due to specified profiles [" + profileSpec +
								"] not matching: " + getReaderContext().getResource());
					}
					return;
				}
			}
		}

		//  注册前夕处理
		preProcessXml(root);
		// 解析 BeanDefinition
		parseBeanDefinitions(root, this.delegate);
		// 解析后处理
		postProcessXml(root);

		// 将 delegate 重新设置为老的 BeanDefinitionParserDelegate
		this.delegate = parent;
	}

	// 创建一个 BeanDefinitionParseDelegate
	protected BeanDefinitionParserDelegate createDelegate(
			XmlReaderContext readerContext, Element root, @Nullable BeanDefinitionParserDelegate parentDelegate) {

		// 根据 readerContext 开始 new BeanDefinitionParserDelegate
		BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
		// 初始化默认
		delegate.initDefaults(root, parentDelegate);
		return delegate;
	}

	/**
	 * Parse the elements at the root level in the document:
	 * "import", "alias", "bean".
	 * <p>
	 * 解析 xml 的标签信息包含：import, alias bean
	 *
	 * @param root the DOM root element of the document
	 */
	protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
		// 如果是 <bean/> 默认的命名空间
		if (delegate.isDefaultNamespace(root)) {
			// 获取子节点
			NodeList nl = root.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (node instanceof Element) {
					Element ele = (Element) node;
					// 如果使用的是默认的命名空间，执行默认解析操作
					if (delegate.isDefaultNamespace(ele)) {
						parseDefaultElement(ele, delegate);
					} else {
						delegate.parseCustomElement(ele);
					}
				}
			}
		} else {
			delegate.parseCustomElement(root);
		}
	}

	private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
		// 解析import 标签，
		if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
			importBeanDefinitionResource(ele);
			// 解析 alias 标签
		} else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
			processAliasRegistration(ele);
			// 解析 bean 标签
		} else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
			processBeanDefinition(ele, delegate);
			// 解析 beans 标签
		} else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
			// recurse
			// 注册新的 bean
			doRegisterBeanDefinitions(ele);
		}
	}

	/**
	 * Parse an "import" element and load the bean definitions
	 * from the given resource into the bean factory.
	 * <p>
	 * 从给定的资源中解析 import 标签到 Bean 工厂中
	 */
	protected void importBeanDefinitionResource(Element ele) {
		// 获取 resource 中的目录
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		// 如果路径不存在，直接返回
		if (!StringUtils.hasText(location)) {
			getReaderContext().error("Resource location must not be empty", ele);
			return;
		}

		// Resolve system properties: e.g. "${user.dir}"
		// 解决系统配置属性
		location = getReaderContext().getEnvironment().resolveRequiredPlaceholders(location);
		// 定义实际资源集合
		Set<Resource> actualResources = new LinkedHashSet<>(4);

		// Discover whether the location is an absolute or relative URI
		// 从路径判断是否是绝对路径
		boolean absoluteLocation = false;
		try {
			// 判断是否是绝对路径, 或者转成 URI 判断是否是绝对路径
			absoluteLocation = ResourcePatternUtils.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
		} catch (URISyntaxException ex) {
			// cannot convert to an URI, considering the location relative
			// unless it is the well-known Spring prefix "classpath*:"
		}

		// Absolute or relative?
		// 如果是绝对路径
		if (absoluteLocation) {
			try {
				// 加载 BeanDefinition 中的标签
				int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
				if (logger.isTraceEnabled()) {
					logger.trace("Imported " + importCount + " bean definitions from URL location [" + location + "]");
				}
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from URL location [" + location + "]", ele, ex);
			}
		} else {
			// 如果是路径, 则Bean 标签在本地文件中
			// No URL -> considering resource location as relative to the current file.
			try {
				// 定义导入数量
				int importCount;
				// 创建相对路径中的 Resource
				Resource relativeResource = getReaderContext().getResource().createRelative(location);
				// 如果想对路径存在
				if (relativeResource.exists()) {
					// 加载 BeanDefinition
					importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
					// 并且将资源导入
					actualResources.add(relativeResource);
				} else {
					// 否则根据路径加载 BeanDefinition
					String baseLocation = getReaderContext().getResource().getURL().toString();
					importCount = getReaderContext().getReader().loadBeanDefinitions(
							StringUtils.applyRelativePath(baseLocation, location), actualResources);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Imported " + importCount + " bean definitions from relative location [" + location + "]");
				}
			} catch (IOException ex) {
				getReaderContext().error("Failed to resolve current resource location", ele, ex);
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from relative location [" + location + "]", ele, ex);
			}
		}
		// 获取只是的资源数组
		Resource[] actResArray = actualResources.toArray(new Resource[0]);
		// 使用监听器激活处理
		getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
	}

	/**
	 * Process the given alias element, registering the alias with the registry.
	 * 从给定的别名注册别名到类中
	 */
	protected void processAliasRegistration(Element ele) {
		// 获取指定标签的名称
		String name = ele.getAttribute(NAME_ATTRIBUTE);
		// 获取指定标签的别名
		String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
		// 判断是否验证
		boolean valid = true;
		// 如果没有文本
		if (!StringUtils.hasText(name)) {
			getReaderContext().error("Name must not be empty", ele);
			valid = false;
		}
		// 如果没有别名
		if (!StringUtils.hasText(alias)) {
			getReaderContext().error("Alias must not be empty", ele);
			valid = false;
		}
		// 如果既有比人命，又有名称
		if (valid) {
			try {
				// 将名称个别名注册到 ReaderContext 中
				getReaderContext().getRegistry().registerAlias(name, alias);
			} catch (Exception ex) {
				getReaderContext().error("Failed to register alias '" + alias +
						"' for bean with name '" + name + "'", ele, ex);
			}
			// 将名称和别名注册到监听器中
			getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
		}
	}

	/**
	 * Process the given bean element, parsing the bean definition
	 * and registering it with the registry.
	 * <p>
	 * 从给定的 bean 标签，解析 BeanDefinition 标签注册到注册表中
	 */
	protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
		//  解析 BeanDefinition 元素
		BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
		// 如果解析的 BeanDefinitionHolder =不为空
		if (bdHolder != null) {
			// 对 Bean 进行装饰
			bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
			try {
				// Register the final decorated instance.
				// 注册最终的 BeanDefinition 示例
				BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to register bean definition with name '" +
						bdHolder.getBeanName() + "'", ele, ex);
			}
			// Send registration event.
			// 监听 Bean 的信息
			getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
		}
	}


	/**
	 * Allow the XML to be extensible by processing any custom element types first,
	 * before we start to process the bean definitions. This method is a natural
	 * extension point for any other custom pre-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 *
	 * @see #getReaderContext()
	 */
	protected void preProcessXml(Element root) {
	}

	/**
	 * Allow the XML to be extensible by processing any custom element types last,
	 * after we finished processing the bean definitions. This method is a natural
	 * extension point for any other custom post-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 *
	 * @see #getReaderContext()
	 */
	protected void postProcessXml(Element root) {
	}

}
