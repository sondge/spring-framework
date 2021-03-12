package club.sondge.code.parsing;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Man {
	private Courier courier;

	public Man() {

	}

	public void setCourier(Courier courier) {
		this.courier = courier;
	}

	public static void main(String[] args) {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("application.xml");
		Man man = (Man) applicationContext.getBean("man");
		System.out.println(man.courier);
	}
}
