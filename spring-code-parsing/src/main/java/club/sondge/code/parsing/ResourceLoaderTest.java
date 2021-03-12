package club.sondge.code.parsing;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;

public class ResourceLoaderTest {
	public static void main(String[] args) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();

		Resource filResource1 = resourceLoader.getResource("/Users/sondge/x.c");
		System.out.println("fileResource1 is ClassPathResource:"+ (filResource1 instanceof ClassPathResource));
//		Resource fileResource2 = resourceLoader.getResource();
		Resource urlResource1 = resourceLoader.getResource("file:/Users/sondge/x.c");
		System.out.println("urlResource1 is URLResource:"+ (urlResource1 instanceof UrlResource));

		Resource urlResource2 = resourceLoader.getResource("https:");
		System.out.println("urlResource2 is URLResource:"+ (urlResource2 instanceof UrlResource));
	}

}
