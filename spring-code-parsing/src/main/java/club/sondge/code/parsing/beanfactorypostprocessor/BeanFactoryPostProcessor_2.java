package club.sondge.code.parsing.beanfactorypostprocessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.Order;

import java.lang.annotation.Annotation;

public class BeanFactoryPostProcessor_2 implements BeanFactoryPostProcessor, Order {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		System.out.println("调用 BeanFactoryPostProcessor_2 ");

		// 获取指定的 BeanDefinition
		BeanDefinition bd = beanFactory.getBeanDefinition("studentService");
		MutablePropertyValues mutablePropertyValues = bd.getPropertyValues();
		mutablePropertyValues.addPropertyValue("name", "王");
		mutablePropertyValues.add("age", 20);
	}

	@Override
	public int value() {
		return 2;
	}


	@Override
	public Class<? extends Annotation> annotationType() {
		return null;
	}
}
