package club.sondge.code.parsing.beanpostprocessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.util.concurrent.atomic.AtomicInteger;

public class MyBeanPostProcessor implements BeanPostProcessor {

	private final static AtomicInteger count = new AtomicInteger();

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		System.out.println("Bean [" + beanName + "] 开始初始化");
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		count.getAndIncrement();
		System.out.println("Bean" + count.get() + " [" + beanName + "] 完成初始化");
		return bean;
	}

	public void display() {
//		System.out.println("Hello BeanPostProcessor,user define bean's count is ");
		System.out.println("Hello BeanPostProcessor,user define bean's count is " + count.get());
	}

	public static void main(String[] args) {
		ClassPathResource resource = new ClassPathResource("application.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		MyBeanPostProcessor myBeanPostProcessor = new MyBeanPostProcessor();
		factory.addBeanPostProcessor(myBeanPostProcessor);
//		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
//		reader.loadBeanDefinitions(resource);
//
//		MyBeanPostProcessor test = (MyBeanPostProcessor) factory.getBean("myBeanPostProcessor");
//		test.display();

		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("application.xml");
		MyBeanPostProcessor test = (MyBeanPostProcessor) applicationContext.getBean("myBeanPostProcessor");
		test.display();

//		Resource resource;
//		BeanFactory beanFactory =  new XmlBeanFactory(r);


	}
}
