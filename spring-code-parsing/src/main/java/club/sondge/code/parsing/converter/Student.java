package club.sondge.code.parsing.converter;

import club.sondge.code.parsing.beanfactorypostprocessor.StudentService;

public class Student {
	private StudentService studentService;

	public StudentService getStudentService() {
		return studentService;
	}

	public void setStudentService(StudentService studentService) {
		this.studentService = studentService;
	}
}
