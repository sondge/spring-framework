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

package org.springframework.beans.factory.support;

import org.springframework.beans.BeanMetadataAttributeAccessor;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Base class for concrete, full-fledged {@link BeanDefinition} classes,
 * factoring out common properties of {@link GenericBeanDefinition},
 * {@link RootBeanDefinition}, and {@link ChildBeanDefinition}.
 *
 * <p>The autowire constants match the ones defined in the
 * {@link org.springframework.beans.factory.config.AutowireCapableBeanFactory}
 * interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Mark Fisher
 * @see GenericBeanDefinition
 * @see RootBeanDefinition
 * @see ChildBeanDefinition
 */
@SuppressWarnings("serial")
public abstract class AbstractBeanDefinition extends BeanMetadataAttributeAccessor
		implements BeanDefinition, Cloneable {

	/**
	 * Constant for the default scope name: {@code ""}, equivalent to singleton
	 * status unless overridden from a parent bean definition (if applicable).
	 * 默认的作用方法为空
	 */
	public static final String SCOPE_DEFAULT = "";

	/**
	 * Constant that indicates no external autowiring at all.
	 *
	 * @see #setAutowireMode
	 * 设置是否自动注入的是编号
	 */
	public static final int AUTOWIRE_NO = AutowireCapableBeanFactory.AUTOWIRE_NO;

	/**
	 * Constant that indicates autowiring bean properties by name.
	 *
	 * @see #setAutowireMode
	 * 设置通过名称自动注入的编号
	 */
	public static final int AUTOWIRE_BY_NAME = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;

	/**
	 * Constant that indicates autowiring bean properties by type.
	 *
	 * @see #setAutowireMode
	 * <p>
	 *  设置通过类型自动注入的常量
	 */
	public static final int AUTOWIRE_BY_TYPE = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;

	/**
	 * Constant that indicates autowiring a constructor.
	 *
	 * @see #setAutowireMode
	 * 设置是否是通过构造方法自动注入
	 */
	public static final int AUTOWIRE_CONSTRUCTOR = AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;

	/**
	 * Constant that indicates determining an appropriate autowire strategy
	 * through introspection of the bean class.
	 *
	 * @see #setAutowireMode
	 * @deprecated as of Spring 3.0: If you are using mixed autowiring strategies,
	 * use annotation-based autowiring for clearer demarcation of autowiring needs.
	 * <p>
	 * 是否是自动扫描的注入方法
	 */
	@Deprecated
	public static final int AUTOWIRE_AUTODETECT = AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT;

	/**
	 * Constant that indicates no dependency check at all.
	 *
	 * @see #setDependencyCheck
	 * <p>
	 * 是否开启依赖检测全部
	 */
	public static final int DEPENDENCY_CHECK_NONE = 0;

	/**
	 * Constant that indicates dependency checking for object references.
	 *
	 * @see #setDependencyCheck
	 * <p>
	 * 表示是否开启依赖检测引用
	 */
	public static final int DEPENDENCY_CHECK_OBJECTS = 1;

	/**
	 * Constant that indicates dependency checking for "simple" properties.
	 *
	 * @see #setDependencyCheck
	 * @see org.springframework.beans.BeanUtils#isSimpleProperty
	 * <p>
	 * 是否检测依赖的简单的属性
	 */
	public static final int DEPENDENCY_CHECK_SIMPLE = 2;

	/**
	 * Constant that indicates dependency checking for all properties
	 * (object references as well as "simple" properties).
	 *
	 * @see #setDependencyCheck
	 * <p>
	 * 是否检测依赖的属性和引用
	 */
	public static final int DEPENDENCY_CHECK_ALL = 3;

	/**
	 * Constant that indicates the container should attempt to infer the
	 * {@link #setDestroyMethodName destroy method name} for a bean as opposed to
	 * explicit specification of a method name. The value {@value} is specifically
	 * designed to include characters otherwise illegal in a method name, ensuring
	 * no possibility of collisions with legitimately named methods having the same
	 * name.
	 * <p>Currently, the method names detected during destroy method inference
	 * are "close" and "shutdown", if present on the specific bean class.
	 * <p>
	 * 尝试推断 destroyMethod 名称，而不是显式的通过方法名称
	 */
	public static final String INFER_METHOD = "(inferred)";


	@Nullable
	// 定义一个不可变的 bean
	private volatile Object beanClass;

	@Nullable
	// 定义作用范围
	private String scope = SCOPE_DEFAULT;

	// 判断是否是抽象方法对应使用的
	private boolean abstractFlag = false;

	@Nullable
	// 是否是懒加载的
	private Boolean lazyInit;

	// 注入模式，默认是按照编号注入的
	private int autowireMode = AUTOWIRE_NO;

	// 依赖检查，默认检测全部
	private int dependencyCheck = DEPENDENCY_CHECK_NONE;

	@Nullable
	// 被依赖对象的名称
	private String[] dependsOn;

	// 是否是候选注入
	private boolean autowireCandidate = true;

	// 是否为主类
	private boolean primary = false;

	// 接口对应不同实例的解析
	private final Map<String, AutowireCandidateQualifier> qualifiers = new LinkedHashMap<>();

	@Nullable
	// 定义异步创建 bean 的回调
	private Supplier<?> instanceSupplier;

	// 是否允许非 public 权限的反射
	private boolean nonPublicAccessAllowed = true;
	// 是否是有参构造
	private boolean lenientConstructorResolution = true;

	@Nullable
	// factoryBeanName
	private String factoryBeanName;

	@Nullable
	// factoryMethodName
	private String factoryMethodName;

	@Nullable
	// 构造方法参数值
	private ConstructorArgumentValues constructorArgumentValues;

	@Nullable
	// 属性
	private MutablePropertyValues propertyValues;
	// 继承的方法集合
	private MethodOverrides methodOverrides = new MethodOverrides();

	@Nullable
	// 初始化方法名称
	private String initMethodName;

	@Nullable
	// 销毁方法名称
	private String destroyMethodName;
	// 是否强制初始化方法
	private boolean enforceInitMethod = true;
	// 是否强制销毁方法
	private boolean enforceDestroyMethod = true;
	// 是否是合成
	private boolean synthetic = false;
	// 默认是用户自定义的 bean
	private int role = BeanDefinition.ROLE_APPLICATION;

	@Nullable
	// bean 描述
	private String description;

	@Nullable
	// 对应的资源
	private Resource resource;


	/**
	 * Create a new AbstractBeanDefinition with default settings.
	 * 创建一个 AbstractBeanDefinition
	 */
	protected AbstractBeanDefinition() {
		this(null, null);
	}

	/**
	 * Create a new AbstractBeanDefinition with the given
	 * constructor argument values and property values.
	 * 根据构造方法和多个属性值初始化 AbstractBeanDefinition
	 */
	protected AbstractBeanDefinition(@Nullable ConstructorArgumentValues cargs, @Nullable MutablePropertyValues pvs) {
		this.constructorArgumentValues = cargs;
		this.propertyValues = pvs;
	}

	/**
	 * Create a new AbstractBeanDefinition as a deep copy of the given
	 * bean definition.
	 *
	 * @param original the original bean definition to copy from
	 *                 <p>
	 *                 根据 BeanDefinition 创建 BeanDefinition
	 */
	protected AbstractBeanDefinition(BeanDefinition original) {
		// 设置父名称类
		setParentName(original.getParentName());
		// 设置 Bean 的类名称（不是运行时类名称）
		setBeanClassName(original.getBeanClassName());
		//设置作用范围
		setScope(original.getScope());
		// 设置这个类是否可以单独创建实例
		setAbstract(original.isAbstract());
		// 设置 FactoryBeanName
		setFactoryBeanName(original.getFactoryBeanName());
		// 设置 FactoryMethodName
		setFactoryMethodName(original.getFactoryMethodName());
		// 设置启动规则
		setRole(original.getRole());
		// 设置源资源
		setSource(original.getSource());
		// 拷贝属性从 BeanDefinition
		copyAttributesFrom(original);
		// 如果 传过来的对象是 AbstractBeanDefinition 类型
		if (original instanceof AbstractBeanDefinition) {
			// 将对象转化为 AbstractBeanDefinition
			AbstractBeanDefinition originalAbd = (AbstractBeanDefinition) original;
			// 如果有对应的 BeanClass，则设置对应的 BeanClass
			if (originalAbd.hasBeanClass()) {
				setBeanClass(originalAbd.getBeanClass());
			}
			// 如果有对应的 ConstructorArgumentValues, 则设置对应的 ConstructArgumentValues
			if (originalAbd.hasConstructorArgumentValues()) {
				setConstructorArgumentValues(new ConstructorArgumentValues(original.getConstructorArgumentValues()));
			}
			// 设置对应的属性值
			if (originalAbd.hasPropertyValues()) {
				setPropertyValues(new MutablePropertyValues(original.getPropertyValues()));
			}
			// 设置对应的继承方法
			if (originalAbd.hasMethodOverrides()) {
				setMethodOverrides(new MethodOverrides(originalAbd.getMethodOverrides()));
			}
			// 设置对应的懒加载方法
			Boolean lazyInit = originalAbd.getLazyInit();
			if (lazyInit != null) {
				setLazyInit(lazyInit);
			}
			// 设置注入模式
			setAutowireMode(originalAbd.getAutowireMode());
			// 设置对应的依赖检查模式
			setDependencyCheck(originalAbd.getDependencyCheck());
			// 设置 DependsOn
			setDependsOn(originalAbd.getDependsOn());
			// 设置候选的注入
			setAutowireCandidate(originalAbd.isAutowireCandidate());
			// 设置是否为主要的类
			setPrimary(originalAbd.isPrimary());
			// 设置全部的对应配置
			copyQualifiersFrom(originalAbd);
			// 设置供应示例
			setInstanceSupplier(originalAbd.getInstanceSupplier());
			// 设置是否允许非公共方法的权限
			setNonPublicAccessAllowed(originalAbd.isNonPublicAccessAllowed());
			// TODO 作用
			setLenientConstructorResolution(originalAbd.isLenientConstructorResolution());
			// 设置初始化方法名称
			setInitMethodName(originalAbd.getInitMethodName());
			// 设置强制初始化方法
			setEnforceInitMethod(originalAbd.isEnforceInitMethod());
			// 设置销毁方法名称
			setDestroyMethodName(originalAbd.getDestroyMethodName());
			// 设置强制防范
			setEnforceDestroyMethod(originalAbd.isEnforceDestroyMethod());
			// 设置是否是合成类
			setSynthetic(originalAbd.isSynthetic());
			// 设置对应的资源
			setResource(originalAbd.getResource());
		} else {
			// 设置 ConstructorArgumentValues
			setConstructorArgumentValues(new ConstructorArgumentValues(original.getConstructorArgumentValues()));
			// 设置对应的属性值
			setPropertyValues(new MutablePropertyValues(original.getPropertyValues()));
			// 设置是否懒加载
			setLazyInit(original.isLazyInit());
			// 设置对应的资源描述
			setResourceDescription(original.getResourceDescription());
		}
	}


	/**
	 * Override settings in this bean definition (presumably a copied parent
	 * from a parent-child inheritance relationship) from the given bean
	 * definition (presumably the child).
	 * <ul>
	 * <li>Will override beanClass if specified in the given bean definition.
	 * <li>Will always take {@code abstract}, {@code scope},
	 * {@code lazyInit}, {@code autowireMode}, {@code dependencyCheck},
	 * and {@code dependsOn} from the given bean definition.
	 * <li>Will add {@code constructorArgumentValues}, {@code propertyValues},
	 * {@code methodOverrides} from the given bean definition to existing ones.
	 * <li>Will override {@code factoryBeanName}, {@code factoryMethodName},
	 * {@code initMethodName}, and {@code destroyMethodName} if specified
	 * in the given bean definition.
	 * </ul>
	 * <p>
	 * 重写父类的方法从其他的 beanDefinition
	 */
	public void overrideFrom(BeanDefinition other) {
		// 如果 beanClassName 不为空
		if (StringUtils.hasLength(other.getBeanClassName())) {
			// 设置 BeanClassName
			setBeanClassName(other.getBeanClassName());
		}
		// 如果有对应的作用域
		if (StringUtils.hasLength(other.getScope())) {
			// 设置对应的作用域
			setScope(other.getScope());
		}
		// 设置是否是抽象的
		setAbstract(other.isAbstract());
		if (StringUtils.hasLength(other.getFactoryBeanName())) {
			// 设置  FactoryBeanName
			setFactoryBeanName(other.getFactoryBeanName());
		}
		if (StringUtils.hasLength(other.getFactoryMethodName())) {
			// 设置  FactoryMethodName
			setFactoryMethodName(other.getFactoryMethodName());
		}
		// 设置对应的规则
		setRole(other.getRole());
		// 设置对应的实力
		setSource(other.getSource());
		// 从 other 中拷贝属性
		copyAttributesFrom(other);
		// 如果 other 表示的是 AbstractBeanDefinition
		if (other instanceof AbstractBeanDefinition) {
			AbstractBeanDefinition otherAbd = (AbstractBeanDefinition) other;
			if (otherAbd.hasBeanClass()) {
				// 设置 BeanClass 名称
				setBeanClass(otherAbd.getBeanClass());
			}
			if (otherAbd.hasConstructorArgumentValues()) {
				// 设置构造参数的值
				getConstructorArgumentValues().addArgumentValues(other.getConstructorArgumentValues());
			}
			if (otherAbd.hasPropertyValues()) {
				// 设置对应的属性
				getPropertyValues().addPropertyValues(other.getPropertyValues());
			}
			if (otherAbd.hasMethodOverrides()) {
				// 新增器覆盖方法
				getMethodOverrides().addOverrides(otherAbd.getMethodOverrides());
			}
			Boolean lazyInit = otherAbd.getLazyInit();
			if (lazyInit != null) {
				// 设置懒加载
				setLazyInit(lazyInit);
			}
			// 设置注入模式
			setAutowireMode(otherAbd.getAutowireMode());
			// 设置依赖检测
			setDependencyCheck(otherAbd.getDependencyCheck());
			setDependsOn(otherAbd.getDependsOn());
			// 设置是否为 候选注入
			setAutowireCandidate(otherAbd.isAutowireCandidate());
			setPrimary(otherAbd.isPrimary());
			// 从对象中拷贝 父类与子类的对应关系
			copyQualifiersFrom(otherAbd);
			// 设置示例供应
			setInstanceSupplier(otherAbd.getInstanceSupplier());
			// 设置是否允许没有公共权限的
			setNonPublicAccessAllowed(otherAbd.isNonPublicAccessAllowed());
			// TODO 作用域
			setLenientConstructorResolution(otherAbd.isLenientConstructorResolution());
			// 设置初始化方法
			if (otherAbd.getInitMethodName() != null) {
				setInitMethodName(otherAbd.getInitMethodName());
				setEnforceInitMethod(otherAbd.isEnforceInitMethod());
			}
			// 设置 销毁方法
			if (otherAbd.getDestroyMethodName() != null) {
				setDestroyMethodName(otherAbd.getDestroyMethodName());
				setEnforceDestroyMethod(otherAbd.isEnforceDestroyMethod());
			}
			// 设置是否合成
			setSynthetic(otherAbd.isSynthetic());
			// 设置对应的资源
			setResource(otherAbd.getResource());
		} else {
			// 设置参数构造
			getConstructorArgumentValues().addArgumentValues(other.getConstructorArgumentValues());
			// 设置对应的属性值
			getPropertyValues().addPropertyValues(other.getPropertyValues());
			// 设置是否懒加载
			setLazyInit(other.isLazyInit());
			// 设置资源描述
			setResourceDescription(other.getResourceDescription());
		}
	}

	/**
	 * Apply the provided default values to this bean.
	 * <p>
	 * 对 bean 提供默认值
	 *
	 * @param defaults the default settings to apply
	 * @since 2.5
	 */
	public void applyDefaults(BeanDefinitionDefaults defaults) {
		// 设置默认的 BeanDefinition
		// 设置是否懒加载
		Boolean lazyInit = defaults.getLazyInit();
		if (lazyInit != null) {
			setLazyInit(lazyInit);
		}
		// 设置自动注入模式
		setAutowireMode(defaults.getAutowireMode());
		// 设置依赖检查
		setDependencyCheck(defaults.getDependencyCheck());
		// 设置初始化方法名称
		setInitMethodName(defaults.getInitMethodName());
		// 设置强制的初始化方法
		setEnforceInitMethod(false);
		// 设置销毁的方法名称
		setDestroyMethodName(defaults.getDestroyMethodName());
		// 设置强制销毁的方法
		setEnforceDestroyMethod(false);
	}


	/**
	 * Specify the bean class name of this bean definition.
	 * 为 bean 设置对应的   BeanClassName
	 */
	@Override
	public void setBeanClassName(@Nullable String beanClassName) {
		this.beanClass = beanClassName;
	}

	/**
	 * Return the current bean class name of this bean definition.
	 * 获取当前的 beanClassName 的名称
	 */
	@Override
	@Nullable
	public String getBeanClassName() {
		Object beanClassObject = this.beanClass;
		if (beanClassObject instanceof Class) {
			return ((Class<?>) beanClassObject).getName();
		} else {
			return (String) beanClassObject;
		}
	}

	/**
	 * Specify the class for this bean.
	 * 设置对应的  beanClassName
	 *
	 * @see #setBeanClassName(String)
	 */
	public void setBeanClass(@Nullable Class<?> beanClass) {
		this.beanClass = beanClass;
	}

	/**
	 * Return the specified class of the bean definition (assuming it is resolved already).
	 * <p><b>NOTE:</b> This is an initial class reference as declared in the bean metadata
	 * definition, potentially combined with a declared factory method or a
	 * {@link org.springframework.beans.factory.FactoryBean} which may lead to a different
	 * runtime type of the bean, or not being set at all in case of an instance-level
	 * factory method (which is resolved via {@link #getFactoryBeanName()} instead).
	 * <b>Do not use this for runtime type introspection of arbitrary bean definitions.</b>
	 * The recommended way to find out about the actual runtime type of a particular bean
	 * is a {@link org.springframework.beans.factory.BeanFactory#getType} call for the
	 * specified bean name; this takes all of the above cases into account and returns the
	 * type of object that a {@link org.springframework.beans.factory.BeanFactory#getBean}
	 * call is going to return for the same bean name.
	 *
	 * @return the resolved bean class (never {@code null})
	 * @throws IllegalStateException if the bean definition does not define a bean class,
	 *                               or a specified bean class name has not been resolved into an actual Class yet
	 * @see #getBeanClassName()
	 * @see #hasBeanClass()
	 * @see #setBeanClass(Class)
	 * @see #resolveBeanClass(ClassLoader)
	 */

	// 获取当前的 Class 对象
	public Class<?> getBeanClass() throws IllegalStateException {
		Object beanClassObject = this.beanClass;
		if (beanClassObject == null) {
			throw new IllegalStateException("No bean class specified on bean definition");
		}
		if (!(beanClassObject instanceof Class)) {
			throw new IllegalStateException(
					"Bean class name [" + beanClassObject + "] has not been resolved into an actual Class");
		}
		return (Class<?>) beanClassObject;
	}

	/**
	 * Return whether this definition specifies a bean class.
	 * 判断对应的类是否是一个指定的beanClass
	 *
	 * @see #getBeanClass()
	 * @see #setBeanClass(Class)
	 * @see #resolveBeanClass(ClassLoader)
	 */
	public boolean hasBeanClass() {
		return (this.beanClass instanceof Class);
	}

	/**
	 * Determine the class of the wrapped bean, resolving it from a
	 * specified class name if necessary. Will also reload a specified
	 * Class from its name when called with the bean class already resolved.
	 *
	 * @param classLoader the ClassLoader to use for resolving a (potential) class name
	 * @return the resolved bean class
	 * @throws ClassNotFoundException if the class name could be resolved
	 *                                <p>
	 *                                确定包装的 Bean 的类，如果有必要会从对应的类名去使用它
	 */
	@Nullable
	public Class<?> resolveBeanClass(@Nullable ClassLoader classLoader) throws ClassNotFoundException {
		// 获取 Bean 的名称
		String className = getBeanClassName();
		if (className == null) {
			return null;
		}
		// 获取对应的 Class 对象
		Class<?> resolvedClass = ClassUtils.forName(className, classLoader);
		this.beanClass = resolvedClass;
		return resolvedClass;
	}

	/**
	 * Return a resolvable type for this bean definition.
	 * <p>This implementation delegates to {@link #getBeanClass()}.
	 *
	 * @since 5.2
	 * <p>
	 * 返回 Bean 对应的解析类型
	 */
	@Override
	public ResolvableType getResolvableType() {
		return (hasBeanClass() ? ResolvableType.forClass(getBeanClass()) : ResolvableType.NONE);
	}

	/**
	 * Set the name of the target scope for the bean.
	 * <p>The default is singleton status, although this is only applied once
	 * a bean definition becomes active in the containing factory. A bean
	 * definition may eventually inherit its scope from a parent bean definition.
	 * For this reason, the default scope name is an empty string (i.e., {@code ""}),
	 * with singleton status being assumed until a resolved scope is set.
	 *
	 * @see #SCOPE_SINGLETON
	 * @see #SCOPE_PROTOTYPE
	 * 设置 Bean 的作用域， 默认的作用域采用的 singleton 的方式
	 */
	@Override
	public void setScope(@Nullable String scope) {
		this.scope = scope;
	}

	/**
	 * Return the name of the target scope for the bean.
	 * 获取对应的作用域
	 */
	@Override
	@Nullable
	public String getScope() {
		return this.scope;
	}

	/**
	 * Return whether this a <b>Singleton</b>, with a single shared instance
	 * returned from all calls.
	 *
	 * @see #SCOPE_SINGLETON
	 * 判断这个类的作用域是否是单例的
	 */
	@Override
	public boolean isSingleton() {
		return SCOPE_SINGLETON.equals(this.scope) || SCOPE_DEFAULT.equals(this.scope);
	}

	/**
	 * Return whether this a <b>Prototype</b>, with an independent instance
	 * returned for each call.
	 *
	 * @see #SCOPE_PROTOTYPE
	 * 判断这个类的作用域是否是 PROTOTYPE
	 */
	@Override
	public boolean isPrototype() {
		return SCOPE_PROTOTYPE.equals(this.scope);
	}

	/**
	 * Set if this bean is "abstract", i.e. not meant to be instantiated itself but
	 * rather just serving as parent for concrete child bean definitions.
	 * <p>Default is "false". Specify true to tell the bean factory to not try to
	 * instantiate that particular bean in any case.
	 * <p>
	 * 判断对应的对象是否是 作用在子类上面的
	 */
	public void setAbstract(boolean abstractFlag) {
		this.abstractFlag = abstractFlag;
	}

	/**
	 * Return whether this bean is "abstract", i.e. not meant to be instantiated
	 * itself but rather just serving as parent for concrete child bean definitions.
	 * 判断对应的类是否是作用在子对象上的
	 */
	@Override
	public boolean isAbstract() {
		return this.abstractFlag;
	}

	/**
	 * Set whether this bean should be lazily initialized.
	 * <p>If {@code false}, the bean will get instantiated on startup by bean
	 * factories that perform eager initialization of singletons.
	 * <p>
	 * 设置是否是延迟加载
	 */
	@Override
	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * Return whether this bean should be lazily initialized, i.e. not
	 * eagerly instantiated on startup. Only applicable to a singleton bean.
	 *
	 * @return whether to apply lazy-init semantics ({@code false} by default)
	 * <p>
	 * 获取该对应方法时延迟加载
	 */
	@Override
	public boolean isLazyInit() {
		return (this.lazyInit != null && this.lazyInit.booleanValue());
	}

	/**
	 * Return whether this bean should be lazily initialized, i.e. not
	 * eagerly instantiated on startup. Only applicable to a singleton bean.
	 *
	 * @return the lazy-init flag if explicitly set, or {@code null} otherwise
	 * @since 5.2
	 * <p>
	 * 是否延迟获取当前 Bean 仅仅作用域  singleton
	 */
	@Nullable
	public Boolean getLazyInit() {
		return this.lazyInit;
	}

	/**
	 * Set the autowire mode. This determines whether any automagical detection
	 * and setting of bean references will happen. Default is AUTOWIRE_NO
	 * which means there won't be convention-based autowiring by name or type
	 * (however, there may still be explicit annotation-driven autowiring).
	 *
	 * @param autowireMode the autowire mode to set.
	 *                     Must be one of the constants defined in this class.
	 * @see #AUTOWIRE_NO
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_CONSTRUCTOR
	 * @see #AUTOWIRE_AUTODETECT
	 * <p>
	 * 设置对应的注入模式，默认值为 AUTOWIRE_NO
	 */
	public void setAutowireMode(int autowireMode) {
		this.autowireMode = autowireMode;
	}

	/**
	 * Return the autowire mode as specified in the bean definition.
	 * 返回对应的注入模式
	 */
	public int getAutowireMode() {
		return this.autowireMode;
	}

	/**
	 * Return the resolved autowire code,
	 * (resolving AUTOWIRE_AUTODETECT to AUTOWIRE_CONSTRUCTOR or AUTOWIRE_BY_TYPE).
	 *
	 * @see #AUTOWIRE_AUTODETECT
	 * @see #AUTOWIRE_CONSTRUCTOR
	 * @see #AUTOWIRE_BY_TYPE
	 * <p>
	 * 获取解决的注入模式
	 */
	public int getResolvedAutowireMode() {

		// 如果 注入模式为AUTOWIRE_AUTODETECT 并且参数注入为 0，则通过属性注入
		if (this.autowireMode == AUTOWIRE_AUTODETECT) {
			// Work out whether to apply setter autowiring or constructor autowiring.
			// If it has a no-arg constructor it's deemed to be setter autowiring,
			// otherwise we'll try constructor autowiring.
			// 获取改造方法
			Constructor<?>[] constructors = getBeanClass().getConstructors();
			for (Constructor<?> constructor : constructors) {
				if (constructor.getParameterCount() == 0) {
					return AUTOWIRE_BY_TYPE;
				}
			}
			// 返回构造方法注入
			return AUTOWIRE_CONSTRUCTOR;
		} else {
			// 返回注入模式
			return this.autowireMode;
		}
	}

	/**
	 * Set the dependency check code.
	 *
	 * @param dependencyCheck the code to set.
	 *                        Must be one of the four constants defined in this class.
	 * @see #DEPENDENCY_CHECK_NONE
	 * @see #DEPENDENCY_CHECK_OBJECTS
	 * @see #DEPENDENCY_CHECK_SIMPLE
	 * @see #DEPENDENCY_CHECK_ALL
	 * <p>
	 * 设置依赖检测的方式
	 */
	public void setDependencyCheck(int dependencyCheck) {
		this.dependencyCheck = dependencyCheck;
	}

	/**
	 * Return the dependency check code.
	 * <p>
	 * 获取依赖检测
	 */
	public int getDependencyCheck() {
		return this.dependencyCheck;
	}

	/**
	 * Set the names of the beans that this bean depends on being initialized.
	 * The bean factory will guarantee that these beans get initialized first.
	 * <p>Note that dependencies are normally expressed through bean properties or
	 * constructor arguments. This property should just be necessary for other kinds
	 * of dependencies like statics (*ugh*) or database preparation on startup.
	 * <p>
	 * 设置初始化依赖的 bean spring在操作的时候会优先将这个 bean 处理
	 */
	@Override
	public void setDependsOn(@Nullable String... dependsOn) {
		this.dependsOn = dependsOn;
	}

	/**
	 * Return the bean names that this bean depends on.
	 * <p>
	 * 返回被依赖额对象名称
	 */
	@Override
	@Nullable
	public String[] getDependsOn() {
		return this.dependsOn;
	}

	/**
	 * Set whether this bean is a candidate for getting autowired into some other bean.
	 * <p>Note that this flag is designed to only affect type-based autowiring.
	 * It does not affect explicit references by name, which will get resolved even
	 * if the specified bean is not marked as an autowire candidate. As a consequence,
	 * autowiring by name will nevertheless inject a bean if the name matches.
	 *
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_BY_NAME
	 * <p>
	 * 配置是否自动候选 TODO 候选的意思
	 */
	@Override
	public void setAutowireCandidate(boolean autowireCandidate) {
		this.autowireCandidate = autowireCandidate;
	}

	/**
	 * Return whether this  bean is a candidate for getting autowired into some other bean.
	 * <p>
	 * 返回这个类是否是自动候选的
	 */
	@Override
	public boolean isAutowireCandidate() {
		return this.autowireCandidate;
	}

	/**
	 * Set whether this bean is a primary autowire candidate.
	 * <p>If this value is {@code true} for exactly one bean among multiple
	 * matching candidates, it will serve as a tie-breaker.
	 * <p>
	 * 配置自动候选的类的主类
	 */
	@Override
	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	/**
	 * Return whether this bean is a primary autowire candidate.
	 * <p>
	 * 返回这个类是否是主类
	 */
	@Override
	public boolean isPrimary() {
		return this.primary;
	}

	/**
	 * Register a qualifier to be used for autowire candidate resolution,
	 * keyed by the qualifier's type name.
	 *
	 * @see AutowireCandidateQualifier#getTypeName()
	 * <p>
	 * 注册一新的相同的类型字段到容器中
	 */
	public void addQualifier(AutowireCandidateQualifier qualifier) {
		this.qualifiers.put(qualifier.getTypeName(), qualifier);
	}

	/**
	 * Return whether this bean has the specified qualifier.
	 * 判断这个类是否有特殊的类
	 */
	public boolean hasQualifier(String typeName) {
		return this.qualifiers.containsKey(typeName);
	}

	/**
	 * Return the qualifier mapped to the provided type name.
	 * 根据类型名称获取对应的注入类型
	 */
	@Nullable
	public AutowireCandidateQualifier getQualifier(String typeName) {
		return this.qualifiers.get(typeName);
	}

	/**
	 * Return all registered qualifiers.
	 *
	 * @return the Set of {@link AutowireCandidateQualifier} objects.
	 * 返回全部的注册的 AutowireCandidateQualifier
	 */
	public Set<AutowireCandidateQualifier> getQualifiers() {
		return new LinkedHashSet<>(this.qualifiers.values());
	}

	/**
	 * Copy the qualifiers from the supplied AbstractBeanDefinition to this bean definition.
	 *
	 * @param source the AbstractBeanDefinition to copy from
	 *               <p>
	 *               从另一个资源中拷贝对应 qualifiers
	 */
	public void copyQualifiersFrom(AbstractBeanDefinition source) {
		Assert.notNull(source, "Source must not be null");
		// 将对应的 qualifiers 给自己
		this.qualifiers.putAll(source.qualifiers);
	}

	/**
	 * Specify a callback for creating an instance of the bean,
	 * as an alternative to a declaratively specified factory method.
	 * <p>If such a callback is set, it will override any other constructor
	 * or factory method metadata. However, bean property population and
	 * potential annotation-driven injection will still apply as usual.
	 *
	 * @see #setConstructorArgumentValues(ConstructorArgumentValues)
	 * @see #setPropertyValues(MutablePropertyValues)
	 * @since 5.0
	 */
	public void setInstanceSupplier(@Nullable Supplier<?> instanceSupplier) {
		this.instanceSupplier = instanceSupplier;
	}

	/**
	 * Return a callback for creating an instance of the bean, if any.
	 * <p>
	 * 返回一个创建实例的回调
	 *
	 * @since 5.0
	 */
	@Nullable
	public Supplier<?> getInstanceSupplier() {
		return this.instanceSupplier;
	}

	/**
	 * Specify whether to allow access to non-public constructors and methods,
	 * for the case of externalized metadata pointing to those. The default is
	 * {@code true}; switch this to {@code false} for public access only.
	 * <p>This applies to constructor resolution, factory method resolution,
	 * and also init/destroy methods. Bean property accessors have to be public
	 * in any case and are not affected by this setting.
	 * <p>Note that annotation-driven configuration will still access non-public
	 * members as far as they have been annotated. This setting applies to
	 * externalized metadata in this bean definition only.
	 * <p>
	 * 设置允许设置非公共权限
	 */
	public void setNonPublicAccessAllowed(boolean nonPublicAccessAllowed) {
		this.nonPublicAccessAllowed = nonPublicAccessAllowed;
	}

	/**
	 * Return whether to allow access to non-public constructors and methods.
	 * <p>
	 * 返回是否允许访问公共方法
	 */
	public boolean isNonPublicAccessAllowed() {
		return this.nonPublicAccessAllowed;
	}

	/**
	 * Specify whether to resolve constructors in lenient mode ({@code true},
	 * which is the default) or to switch to strict resolution (throwing an exception
	 * in case of ambiguous constructors that all match when converting the arguments,
	 * whereas lenient mode would use the one with the 'closest' type matches).
	 * <p>
	 * 设置 构造函数是否是宽大模式（构造参数在模棱两可的时候也可以匹配）
	 */
	public void setLenientConstructorResolution(boolean lenientConstructorResolution) {
		this.lenientConstructorResolution = lenientConstructorResolution;
	}

	/**
	 * Return whether to resolve constructors in lenient mode or in strict mode.
	 * 返回 spring 中的构造方法中是否是宽大模式
	 */
	public boolean isLenientConstructorResolution() {
		return this.lenientConstructorResolution;
	}

	/**
	 * Specify the factory bean to use, if any.
	 * This the name of the bean to call the specified factory method on.
	 *
	 * @see #setFactoryMethodName
	 * <p>
	 * 设置指定的  FactoryBeanName
	 */
	@Override
	public void setFactoryBeanName(@Nullable String factoryBeanName) {
		this.factoryBeanName = factoryBeanName;
	}

	/**
	 * Return the factory bean name, if any.
	 * <p>
	 * 返回对应的 factory bean name
	 */
	@Override
	@Nullable
	public String getFactoryBeanName() {
		return this.factoryBeanName;
	}

	/**
	 * Specify a factory method, if any. This method will be invoked with
	 * constructor arguments, or with no arguments if none are specified.
	 * The method will be invoked on the specified factory bean, if any,
	 * or otherwise as a static method on the local bean class.
	 * <p>
	 * 设置对应的 Factory Method 名称
	 *
	 * @see #setFactoryBeanName
	 * @see #setBeanClassName
	 */
	@Override
	public void setFactoryMethodName(@Nullable String factoryMethodName) {
		this.factoryMethodName = factoryMethodName;
	}

	/**
	 * Return a factory method, if any.
	 * <p>
	 * 返回对应的工厂方法名称
	 */
	@Override
	@Nullable
	public String getFactoryMethodName() {
		return this.factoryMethodName;
	}

	/**
	 * Specify constructor argument values for this bean.
	 * <p>
	 * 特质设置这个类中的 构造参数
	 */
	public void setConstructorArgumentValues(ConstructorArgumentValues constructorArgumentValues) {
		this.constructorArgumentValues = constructorArgumentValues;
	}

	/**
	 * Return constructor argument values for this bean (never {@code null}).
	 * <p>
	 * 返回对应的构造参数值
	 */
	@Override
	public ConstructorArgumentValues getConstructorArgumentValues() {
		if (this.constructorArgumentValues == null) {
			this.constructorArgumentValues = new ConstructorArgumentValues();
		}
		return this.constructorArgumentValues;
	}

	/**
	 * Return if there are constructor argument values defined for this bean.
	 * <p>
	 * 返回这个构造参数是否有值
	 */
	@Override
	public boolean hasConstructorArgumentValues() {
		return (this.constructorArgumentValues != null && !this.constructorArgumentValues.isEmpty());
	}

	/**
	 * Specify property values for this bean, if any.
	 * <p>
	 * 对这个 bean 设置对应的属性值
	 */
	public void setPropertyValues(MutablePropertyValues propertyValues) {
		this.propertyValues = propertyValues;
	}

	/**
	 * Return property values for this bean (never {@code null}).
	 * <p>
	 * 对于这个 bean，返回对应的属性值
	 */
	@Override
	public MutablePropertyValues getPropertyValues() {
		if (this.propertyValues == null) {
			this.propertyValues = new MutablePropertyValues();
		}
		return this.propertyValues;
	}

	/**
	 * Return if there are property values values defined for this bean.
	 * <p>
	 * 返回这个 bean 中的属性是否有值
	 *
	 * @since 5.0.2
	 */
	@Override
	public boolean hasPropertyValues() {
		return (this.propertyValues != null && !this.propertyValues.isEmpty());
	}

	/**
	 * Specify method overrides for the bean, if any.
	 * <p>
	 * 设置特质重写的覆盖方法
	 */
	public void setMethodOverrides(MethodOverrides methodOverrides) {
		this.methodOverrides = methodOverrides;
	}

	/**
	 * Return information about methods to be overridden by the IoC
	 * container. This will be empty if there are no method overrides.
	 * <p>Never returns {@code null}.
	 * <p>
	 * 返回重写的方法信息，如果没有重写的方法，则返回空
	 */
	public MethodOverrides getMethodOverrides() {
		return this.methodOverrides;
	}

	/**
	 * Return if there are method overrides defined for this bean.
	 *
	 * @since 5.0.2
	 * <p>
	 * 返回这个方法中是否有重写方法
	 */
	public boolean hasMethodOverrides() {
		return !this.methodOverrides.isEmpty();
	}

	/**
	 * Set the name of the initializer method.
	 * <p>The default is {@code null} in which case there is no initializer method.
	 * <p>
	 * 设置初始化的方法名称
	 */
	@Override
	public void setInitMethodName(@Nullable String initMethodName) {
		this.initMethodName = initMethodName;
	}

	/**
	 * Return the name of the initializer method.
	 * 返回这个对应的初始化方法名称
	 */
	@Override
	@Nullable
	public String getInitMethodName() {
		return this.initMethodName;
	}

	/**
	 * Specify whether or not the configured initializer method is the default.
	 * <p>The default value is {@code true} for a locally specified init method
	 * but switched to {@code false} for a shared setting in a defaults section
	 * (e.g. {@code bean init-method} versus {@code beans default-init-method}
	 * level in XML) which might not apply to all contained bean definitions.
	 * 设置指定的方法是否为默认的初始化方法
	 *
	 * @see #setInitMethodName
	 * @see #applyDefaults
	 */
	public void setEnforceInitMethod(boolean enforceInitMethod) {
		this.enforceInitMethod = enforceInitMethod;
	}

	/**
	 * Indicate whether the configured initializer method is the default.
	 *
	 * @see #getInitMethodName()
	 * <p>
	 * 表示配置默认的初始化方法
	 */
	public boolean isEnforceInitMethod() {
		return this.enforceInitMethod;
	}

	/**
	 * Set the name of the destroy method.
	 * <p>The default is {@code null} in which case there is no destroy method.
	 * <p>
	 * 设置销毁方法名称
	 */
	@Override
	public void setDestroyMethodName(@Nullable String destroyMethodName) {
		this.destroyMethodName = destroyMethodName;
	}

	/**
	 * Return the name of the destroy method.
	 * <p>
	 * 返回销毁方法名称
	 */
	@Override
	@Nullable
	public String getDestroyMethodName() {
		return this.destroyMethodName;
	}

	/**
	 * Specify whether or not the configured destroy method is the default.
	 * <p>The default value is {@code true} for a locally specified destroy method
	 * but switched to {@code false} for a shared setting in a defaults section
	 * (e.g. {@code bean destroy-method} versus {@code beans default-destroy-method}
	 * level in XML) which might not apply to all contained bean definitions.
	 *
	 * @see #setDestroyMethodName
	 * @see #applyDefaults
	 * <p>
	 * 设置配置的默认销毁方法名
	 */
	public void setEnforceDestroyMethod(boolean enforceDestroyMethod) {
		this.enforceDestroyMethod = enforceDestroyMethod;
	}

	/**
	 * Indicate whether the configured destroy method is the default.
	 *
	 * @see #getDestroyMethodName()
	 * 是否配置了默认方法名
	 */
	public boolean isEnforceDestroyMethod() {
		return this.enforceDestroyMethod;
	}

	/**
	 * Set whether this bean definition is 'synthetic', that is, not defined
	 * by the application itself (for example, an infrastructure bean such
	 * as a helper for auto-proxying, created through {@code <aop:config>}).
	 * 设置是否是合成类
	 */
	public void setSynthetic(boolean synthetic) {
		this.synthetic = synthetic;
	}

	/**
	 * Return whether this bean definition is 'synthetic', that is,
	 * not defined by the application itself.
	 * <p>
	 * 返回是否是一个合成类
	 */
	public boolean isSynthetic() {
		return this.synthetic;
	}

	/**
	 * Set the role hint for this {@code BeanDefinition}.
	 * 设置访问规则
	 */
	@Override
	public void setRole(int role) {
		this.role = role;
	}

	/**
	 * Return the role hint for this {@code BeanDefinition}.
	 * <p>
	 * 获取访问规则
	 */
	@Override
	public int getRole() {
		return this.role;
	}

	/**
	 * Set a human-readable description of this bean definition.
	 * <p>
	 * 设置可读的描述
	 */
	@Override
	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	/**
	 * Return a human-readable description of this bean definition.
	 * 返回可读的描述
	 */
	@Override
	@Nullable
	public String getDescription() {
		return this.description;
	}

	/**
	 * Set the resource that this bean definition came from
	 * (for the purpose of showing context in case of errors).
	 * <p>
	 * 设置 beanDefinition 的资源
	 */
	public void setResource(@Nullable Resource resource) {
		this.resource = resource;
	}

	/**
	 * Return the resource that this bean definition came from.
	 * <p>
	 * 获取 beanDefinition 来自的资源
	 */
	@Nullable
	public Resource getResource() {
		return this.resource;
	}

	/**
	 * Set a description of the resource that this bean definition
	 * came from (for the purpose of showing context in case of errors).
	 * <p>
	 * 设置资源描述
	 */
	public void setResourceDescription(@Nullable String resourceDescription) {
		this.resource = (resourceDescription != null ? new DescriptiveResource(resourceDescription) : null);
	}

	/**
	 * Return a description of the resource that this bean definition
	 * came from (for the purpose of showing context in case of errors).
	 * <p>
	 * 获取对应的资源描述
	 */
	@Override
	@Nullable
	public String getResourceDescription() {
		return (this.resource != null ? this.resource.getDescription() : null);
	}

	/**
	 * Set the originating (e.g. decorated) BeanDefinition, if any.
	 */
	public void setOriginatingBeanDefinition(BeanDefinition originatingBd) {
		this.resource = new BeanDefinitionResource(originatingBd);
	}

	/**
	 * Return the originating BeanDefinition, or {@code null} if none.
	 * Allows for retrieving the decorated bean definition, if any.
	 * <p>Note that this method returns the immediate originator. Iterate through the
	 * originator chain to find the original BeanDefinition as defined by the user.
	 * <p>
	 * <p>
	 * 从 BeanDefinition 资源的描述
	 */
	@Override
	@Nullable
	public BeanDefinition getOriginatingBeanDefinition() {
		return (this.resource instanceof BeanDefinitionResource ?
				((BeanDefinitionResource) this.resource).getBeanDefinition() : null);
	}

	/**
	 * Validate this bean definition.
	 *
	 * @throws BeanDefinitionValidationException in case of validation failure
	 *                                           <p>
	 *                                           验证 BeanDefinition
	 */
	public void validate() throws BeanDefinitionValidationException {
		// 如果有重写方法，并且工厂方法名称不为空
		if (hasMethodOverrides() && getFactoryMethodName() != null) {
			throw new BeanDefinitionValidationException(
					"Cannot combine factory method with container-generated method overrides: " +
							"the factory method must create the concrete bean instance.");
		}
		// 判断是否是 BeanClass
		if (hasBeanClass()) {
			// 准备重写方法
			prepareMethodOverrides();
		}
	}

	/**
	 * Validate and prepare the method overrides defined for this bean.
	 * Checks for existence of a method with the specified name.
	 * <p>
	 * 检查是否存在指定的方法名称，指定之后使用 Bean 指定的方法代替
	 *
	 * @throws BeanDefinitionValidationException in case of validation failure
	 */
	public void prepareMethodOverrides() throws BeanDefinitionValidationException {
		// Check that lookup methods exist and determine their overloaded status.
		// 如果包含了重写机制
		if (hasMethodOverrides()) {
			getMethodOverrides().getOverrides().forEach(this::prepareMethodOverride);
		}
	}

	/**
	 * Validate and prepare the given method override.
	 * Checks for existence of a method with the specified name,
	 * marking it as not overloaded if none found.
	 *
	 * @param mo the MethodOverride object to validate
	 * @throws BeanDefinitionValidationException in case of validation failure
	 *                                           <p>
	 *                                           校验和准备重写方法名，如果存在对应的指定名称，如果找到了，将其标记为未重载
	 */
	protected void prepareMethodOverride(MethodOverride mo) throws BeanDefinitionValidationException {
		// 早找对应的方法数量
		int count = ClassUtils.getMethodCountForName(getBeanClass(), mo.getMethodName());
		if (count == 0) {
			throw new BeanDefinitionValidationException(
					"Invalid method override: no method with name '" + mo.getMethodName() +
							"' on class [" + getBeanClassName() + "]");
		} else if (count == 1) {
			// Mark override as not overloaded, to avoid the overhead of arg type checking.
			// 设置未重载
			mo.setOverloaded(false);
		}
	}


	/**
	 * Public declaration of Object's {@code clone()} method.
	 * Delegates to {@link #cloneBeanDefinition()}.
	 *
	 * @see Object#clone()
	 * <p>
	 * 设置克隆 definition
	 */
	@Override
	public Object clone() {
		return cloneBeanDefinition();
	}

	/**
	 * Clone this bean definition.
	 * To be implemented by concrete subclasses.
	 *
	 * @return the cloned bean definition object
	 */
	public abstract AbstractBeanDefinition cloneBeanDefinition();

	@Override
	// 判断和其他对象判断是否相等
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AbstractBeanDefinition)) {
			return false;
		}
		AbstractBeanDefinition that = (AbstractBeanDefinition) other;
		return (ObjectUtils.nullSafeEquals(getBeanClassName(), that.getBeanClassName()) &&
				ObjectUtils.nullSafeEquals(this.scope, that.scope) &&
				this.abstractFlag == that.abstractFlag &&
				this.lazyInit == that.lazyInit &&
				this.autowireMode == that.autowireMode &&
				this.dependencyCheck == that.dependencyCheck &&
				Arrays.equals(this.dependsOn, that.dependsOn) &&
				this.autowireCandidate == that.autowireCandidate &&
				ObjectUtils.nullSafeEquals(this.qualifiers, that.qualifiers) &&
				this.primary == that.primary &&
				this.nonPublicAccessAllowed == that.nonPublicAccessAllowed &&
				this.lenientConstructorResolution == that.lenientConstructorResolution &&
				ObjectUtils.nullSafeEquals(this.constructorArgumentValues, that.constructorArgumentValues) &&
				ObjectUtils.nullSafeEquals(this.propertyValues, that.propertyValues) &&
				ObjectUtils.nullSafeEquals(this.methodOverrides, that.methodOverrides) &&
				ObjectUtils.nullSafeEquals(this.factoryBeanName, that.factoryBeanName) &&
				ObjectUtils.nullSafeEquals(this.factoryMethodName, that.factoryMethodName) &&
				ObjectUtils.nullSafeEquals(this.initMethodName, that.initMethodName) &&
				this.enforceInitMethod == that.enforceInitMethod &&
				ObjectUtils.nullSafeEquals(this.destroyMethodName, that.destroyMethodName) &&
				this.enforceDestroyMethod == that.enforceDestroyMethod &&
				this.synthetic == that.synthetic &&
				this.role == that.role &&
				super.equals(other));
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(getBeanClassName());
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.scope);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.constructorArgumentValues);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.propertyValues);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.factoryBeanName);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.factoryMethodName);
		hashCode = 29 * hashCode + super.hashCode();
		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("class [");
		sb.append(getBeanClassName()).append("]");
		sb.append("; scope=").append(this.scope);
		sb.append("; abstract=").append(this.abstractFlag);
		sb.append("; lazyInit=").append(this.lazyInit);
		sb.append("; autowireMode=").append(this.autowireMode);
		sb.append("; dependencyCheck=").append(this.dependencyCheck);
		sb.append("; autowireCandidate=").append(this.autowireCandidate);
		sb.append("; primary=").append(this.primary);
		sb.append("; factoryBeanName=").append(this.factoryBeanName);
		sb.append("; factoryMethodName=").append(this.factoryMethodName);
		sb.append("; initMethodName=").append(this.initMethodName);
		sb.append("; destroyMethodName=").append(this.destroyMethodName);
		if (this.resource != null) {
			sb.append("; defined in ").append(this.resource.getDescription());
		}
		return sb.toString();
	}

}
