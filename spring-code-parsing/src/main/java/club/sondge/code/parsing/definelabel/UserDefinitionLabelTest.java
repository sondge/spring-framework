package club.sondge.code.parsing.definelabel;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class UserDefinitionLabelTest {
	public static void main(String[] args) {
		ApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext("classpath:application.xml");
		User user = (User) classPathXmlApplicationContext.getBean("user");
		System.out.println(user.getId() + "--" + user.getUserName() + "--" + user.getEmail());
	}
}
