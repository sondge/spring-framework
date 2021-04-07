package club.sondge.code.parsing.override;

import club.sondge.code.parsing.beanfactorypostprocessor.StudentService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MainTest {
	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("application.xml");

		StudentService studentService = (StudentService) context.getBean("student");
		System.out.println("student name:" + studentService.getName());
	}
}
