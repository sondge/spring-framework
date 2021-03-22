package club.sondge.code.parsing.lookup;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class LookUpMethodTest {
	public static void main(String[] args) {
		ApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext("classpath:application.xml");
		Display display = (Display) classPathXmlApplicationContext.getBean("display");
		display.display();
	}
}
