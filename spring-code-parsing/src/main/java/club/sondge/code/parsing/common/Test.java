package club.sondge.code.parsing.common;

import club.sondge.code.parsing.beanfactorypostprocessor.StudentService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Test {
	public static void main(String[] args) {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("application.xml");
		StudentService student = (StudentService) applicationContext.getBean("studentService1");

		System.out.println(student.getName() + " : " + student.getAge());

	}
}
