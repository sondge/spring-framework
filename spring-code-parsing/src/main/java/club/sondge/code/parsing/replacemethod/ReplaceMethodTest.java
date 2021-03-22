package club.sondge.code.parsing.replacemethod;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ReplaceMethodTest {
	public static void main(String[] args) {
		ApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext("classpath:application.xml");
		Method method = (Method) classPathXmlApplicationContext.getBean("method");
		method.display();
	}
}
