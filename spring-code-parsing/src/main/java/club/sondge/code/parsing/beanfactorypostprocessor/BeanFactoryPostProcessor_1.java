package club.sondge.code.parsing.beanfactorypostprocessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.annotation.Order;

import java.lang.annotation.Annotation;

public class BeanFactoryPostProcessor_1 implements BeanFactoryPostProcessor, Order {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		System.out.println("调用 BeanFactoryPostProcessor_1 ");
		System.out.println("容器中 BeanDefinition 中的个数：" + beanFactory.getBeanDefinitionCount());

		// 获取指定的 BeanDefinition
		BeanDefinition bd = beanFactory.getBeanDefinition("studentService");
		MutablePropertyValues mutablePropertyValues = bd.getPropertyValues();
		mutablePropertyValues.addPropertyValue("name", "feng");
		mutablePropertyValues.add("age", 19);
	}

	@Override
	public int value() {
		return 1;
	}


	@Override
	public Class<? extends Annotation> annotationType() {
		return null;
	}

	public static void main(String[] args) {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("application.xml");
		StudentService studentService = (StudentService) applicationContext.getBean("studentService");

		System.out.println(studentService.getName() + " : " + studentService.getAge());
	}
}
