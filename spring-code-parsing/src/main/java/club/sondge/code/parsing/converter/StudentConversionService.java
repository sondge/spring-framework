package club.sondge.code.parsing.converter;

import club.sondge.code.parsing.beanfactorypostprocessor.StudentService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

public class StudentConversionService implements Converter<String, StudentService> {
	@Override
	public StudentService convert(String source) {
		if (StringUtils.hasLength(source)) {
			String[] sources = source.split("#");

			StudentService studentService = new StudentService();
			studentService.setAge(Integer.parseInt(sources[0]));
			studentService.setName(sources[1]);

			return studentService;
		}
		return null;
	}

	public static void main(String[] args) {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("application.xml");
		Student student = (Student) applicationContext.getBean("student111");
		System.out.println(student.getStudentService().getName() + " : " + student.getStudentService().getAge());
	}
}
