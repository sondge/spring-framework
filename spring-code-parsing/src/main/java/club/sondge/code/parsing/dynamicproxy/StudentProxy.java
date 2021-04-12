package club.sondge.code.parsing.dynamicproxy;

public class StudentProxy implements Person {
	Student student;

	public StudentProxy(Person person) {
		if (person.getClass() == Student.class) {
			this.student = (Student) person;
		}
	}

	@Override
	public void giveMoney() {
		before();
		student.giveMoney();
	}

	private void before() {
		System.out.println(student.name + "最近学习有进步");
	}

	public static void main(String[] args) {
		Person person = new Student("李四");
		StudentProxy studentProxy = new StudentProxy(person);
		studentProxy.giveMoney();
	}
}
