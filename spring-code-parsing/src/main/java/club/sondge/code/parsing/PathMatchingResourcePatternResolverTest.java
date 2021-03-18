package club.sondge.code.parsing;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

public class PathMatchingResourcePatternResolverTest {
	public static void main(String[] args) throws IOException {
		PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
		Resource[] resources = pathMatchingResourcePatternResolver.getResources("classpath*:/");
		for (Resource resource : resources) {
			System.out.println(resource);
		}
	}
}
