package club.sondge.code.parsing.dynamicproxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class StuInvocationHandler<T> implements InvocationHandler {

	T target;

	public 	StuInvocationHandler(T target) {
		this.target = target;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println("代理执行" + method.getName() + "方法");
		// 代理检测方法
		MonitorUtil.start();
		Object result = method.invoke(target, args);
		MonitorUtil.finish(method.getName());
		return result;
	}

	public static void main(String[] args) {
		Person lisi = new Student("李四");
		InvocationHandler stuHandler = new StuInvocationHandler<Person>(lisi);
		Person person = (Person) Proxy.newProxyInstance(Person.class.getClassLoader(), new Class<?>[]{Person.class}, stuHandler);
		person.giveMoney();
	}
}
