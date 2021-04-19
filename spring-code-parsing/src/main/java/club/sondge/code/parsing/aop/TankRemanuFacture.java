package club.sondge.code.parsing.aop;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class TankRemanuFacture implements MethodInterceptor {
	@Override
	public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
		if (method.getName().equals("run")) {
			System.out.println("正在重造 59 坦克...");
			System.out.println("重造成功，以获取 59 改之超音速飞行版");
			System.out.println("已起飞，正在突破音障。");
			methodProxy.invokeSuper(o, objects);
			System.out.println("已击落黑鸟 SR-71，正在返航...");
			return null;
		}
		return methodProxy.invokeSuper(o, objects);
	}

	public static void main(String[] args) {
		ProxyCreator proxyCreator = new CglibProxyCreator(new Tank59(), new TankRemanuFacture());
		Tank59 tank59 = (Tank59) proxyCreator.getProxy();

		System.out.println("proxy class = " + tank59.getClass() + "\n");
		tank59.run();
		System.out.println();
		System.out.print("射击测试：");
		tank59.shoot();
	}
}
