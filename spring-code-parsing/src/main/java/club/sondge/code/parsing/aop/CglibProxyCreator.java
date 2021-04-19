package club.sondge.code.parsing.aop;


import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;

public class CglibProxyCreator implements ProxyCreator{

	private Object target;
	private MethodInterceptor methodInterceptor;

	public CglibProxyCreator(Object target, MethodInterceptor methodInterceptor) {
		assert (target != null && methodInterceptor != null);
		this.target = target;
		this.methodInterceptor = methodInterceptor;
	}

	@Override
	public Object getProxy() {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(target.getClass());
		enhancer.setCallback(methodInterceptor);
		return enhancer.create();
	}
}
