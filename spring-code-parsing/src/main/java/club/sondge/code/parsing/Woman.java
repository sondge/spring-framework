package club.sondge.code.parsing;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Woman {
	private Lipstick lipstick;

	public Woman() {

	}

	public void setLipstick(Lipstick lipstick) {
		System.out.println("女人拥有口红");
		this.lipstick = lipstick;
	}

	//	public static void main(String[] args) {
//		Woman woman = new Woman();
//		Lipstick lipstick = new Lipstick();
//
//		lipstick.setColor("红色");
//		woman.setLipstick(lipstick);
//	}
	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("application.xml");
		Woman woman = (Woman) context.getBean("woman");
		System.out.println(woman.toString());
	}

}
