package club.sondge.code.parsing.lifecycle;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

public class LifeCycleTest implements BeanNameAware, BeanClassLoaderAware, BeanFactoryAware, BeanPostProcessor, InitializingBean, DisposableBean {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		System.out.print("执行属性方法 ->");
		this.name = name;
	}

	public LifeCycleTest() {
		System.out.print("执行构造方法 ->");
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		System.out.print("执行 BeanClassLoaderAware ->");
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		System.out.print("执行 BeanFactoryAware ->");
	}

	@Override
	public void setBeanName(String name) {
		System.out.print("执行 BeanNameAware ->");
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		System.out.print("执行前置处理器 ->");
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		System.out.print("执行后置处理器 -> ");
		return bean;
	}

	public void initMethod() {
		System.out.print("执行初始化方法 -> ");
	}

	public void destroyMethod() {
		System.out.print("执行销毁方法");
	}

	@Override
	public void destroy() throws Exception {
		System.out.print("DisposableBean destroy 被调动... -> ");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.print("InitializingBean afterPropertiesSet 被调动... -> ");
	}

	public void display() {
		System.out.print("方法调用... -> ");
	}

	public static void main(String[] args) {
		ClassPathResource resource = new ClassPathResource("application.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		reader.loadBeanDefinitions(resource);
		// BeanFactory 容器一定要调用该方法进行 BeanPostProcessor 注册
		factory.addBeanPostProcessor(new LifeCycleTest()); // <1>

		LifeCycleTest lifeCycleBean = (LifeCycleTest) factory.getBean("lifeCycleTest");
		lifeCycleBean.display();

		System.out.print("方法调用完成，容器开始关闭.... ->");
// 关闭容器
		factory.destroySingletons();
	}
}
