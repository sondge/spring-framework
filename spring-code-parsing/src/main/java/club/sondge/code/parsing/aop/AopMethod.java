package club.sondge.code.parsing.aop;

import club.sondge.code.parsing.beanfactorypostprocessor.StudentService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class AopMethod {

	public static void main(String[] args) {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("application.xml");
		StudentService studentService = (StudentService) applicationContext.getBean("studentService");
		System.out.println(studentService.getName() + ":" + studentService.getAge());

	}
}
