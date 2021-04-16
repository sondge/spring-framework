package club.sondge.code.parsing.aop;

import org.springframework.aop.MethodBeforeAdvice;

import java.lang.reflect.Method;

public class AopBeforeMethod implements MethodBeforeAdvice {
	@Override
	public void before(Method method, Object[] args, Object target) throws Throwable {
		System.out.println(method.getName() + "执行开始");
	}
}
