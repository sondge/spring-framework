package club.sondge.code.parsing.intmethod;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

public class InitializingBeanTest implements InitializingBean {

	private String name;

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("InitializingBeanTest initializing...");
		this.name = "chenssy 2 号";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOtherName(){
		System.out.println("InitializingBeanTest setOtherName...");
		this.name = "chenssy 3 号";
	}

	public static void main(String[] args) {
		ClassPathResource resource = new ClassPathResource("application.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		reader.loadBeanDefinitions(resource);
		InitializingBeanTest initializingBeanTest = (InitializingBeanTest) factory.getBean("initializingBean1");
		System.out.println("name: " + initializingBeanTest.getName());
	}
}
