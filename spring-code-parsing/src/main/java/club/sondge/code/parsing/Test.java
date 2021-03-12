package club.sondge.code.parsing;

import org.springframework.util.StringUtils;

public class Test {
	public static void main(String[] args) {
		String path = "fill:core\\club\\sondge\\sondge.java";
		String s = StringUtils.cleanPath(path);
		System.out.println(s);
	}
}
