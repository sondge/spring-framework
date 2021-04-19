package club.sondge.code.parsing.aop;

import club.sondge.code.parsing.definelabel.User;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class JdkProxyCreator implements ProxyCreator, InvocationHandler {

	private Object target;

	public JdkProxyCreator(Object target) {
		assert target != null;
		Class<?>[] interfaces = target.getClass().getInterfaces();
		if (interfaces.length == 0) {
			throw new IllegalArgumentException("target class implement any interface");
		}
		this.target = target;
	}

	@Override
	public Object getProxy() {
		Class<?> clazz = target.getClass();
		return Proxy.newProxyInstance(clazz.getClassLoader(), target.getClass().getInterfaces(), this);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println(System.currentTimeMillis() + "-" + method.getName() + "method start");
		Object invoke = method.invoke(target, args);
		System.out.println(System.currentTimeMillis() + "-" + method.getName() + "method end");
		return invoke;
	}

	public static void main(String[] args) {
		ProxyCreator proxyCreator = new JdkProxyCreator(new UserServiceImpl());
		UserService userService = (UserService) proxyCreator.getProxy();
		System.out.println("proxy type = " + userService.getClass());
		System.out.println();
		userService.save(new User());
		System.out.println();
		userService.update(new User());
		System.out.println();
	}
}
