/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util.xml;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Detects whether an XML stream is using DTD- or XSD-based validation.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.0
 * <p>
 * Xml 校验检测器
 */
public class XmlValidationModeDetector {

	/**
	 * Indicates that the validation should be disabled.
	 * <p>
	 * 表示禁用校验被禁用
	 */
	public static final int VALIDATION_NONE = 0;

	/**
	 * Indicates that the validation mode should be auto-guessed, since we cannot find
	 * a clear indication (probably choked on some special characters, or the like).
	 * <p>
	 * 自动检测校验模式
	 */
	public static final int VALIDATION_AUTO = 1;

	/**
	 * Indicates that DTD validation should be used (we found a "DOCTYPE" declaration).
	 * 使用 DTD 模式校验
	 */
	public static final int VALIDATION_DTD = 2;

	/**
	 * Indicates that XSD validation should be used (found no "DOCTYPE" declaration).
	 * <p>
	 * 使用 XSD 校验
	 */
	public static final int VALIDATION_XSD = 3;


	/**
	 * The token in a XML document that declares the DTD to use for validation
	 * and thus that DTD validation is being used.
	 * <p>
	 * DTD 中 模式使用 DOCTYPE，如果包含对应的就是 DTD 模式
	 */
	private static final String DOCTYPE = "DOCTYPE";

	/**
	 * The token that indicates the start of an XML comment.
	 * <p>
	 * XML 文档说明的知识
	 */
	private static final String START_COMMENT = "<!--";

	/**
	 * The token that indicates the end of an XML comment.
	 * <p>
	 * XML 文档注释的结束说明
	 */
	private static final String END_COMMENT = "-->";


	/**
	 * Indicates whether or not the current parse position is inside an XML comment.
	 * <p>
	 * 表示分析的内容是否在 XML 注释中
	 */
	private boolean inComment;


	/**
	 * Detect the validation mode for the XML document in the supplied {@link InputStream}.
	 * Note that the supplied {@link InputStream} is closed by this method before returning.
	 *
	 * @param inputStream the InputStream to parse
	 * @throws IOException in case of I/O failure
	 * @see #VALIDATION_DTD
	 * @see #VALIDATION_XSD
	 * <p>
	 * 从输入流中探测校验模式
	 */
	public int detectValidationMode(InputStream inputStream) throws IOException {
		// Peek into the file to look for DOCTYPE.
		// 获取二进制读取文件
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		try {
			// 是否是  Dtd 校验
			boolean isDtdValidated = false;
			String content;
			while ((content = reader.readLine()) != null) {
				// 获取注释包括的非注释内容
				content = consumeCommentTokens(content);
				// 如果在注释找那个，或者不是文本。则继续遍历
				if (this.inComment || !StringUtils.hasText(content)) {
					continue;
				}
				// 如果包含了 DOCTYPE 类型的 XML 文档，则是 DTD 样式的文档
				if (hasDoctype(content)) {
					isDtdValidated = true;
					break;
				}
				if (hasOpeningTag(content)) {
					// End of meaningful data...
					break;
				}
			}
			// 如果是 DTD 模式的内容，则返回 DTD, 否则返回 XSD 中的内容
			return (isDtdValidated ? VALIDATION_DTD : VALIDATION_XSD);
		} catch (CharConversionException ex) {
			// Choked on some character encoding...
			// Leave the decision up to the caller.
			// 返回自定自动检测校验模式
			return VALIDATION_AUTO;
		} finally {
			reader.close();
		}
	}


	/**
	 * Does the content contain the DTD DOCTYPE declaration?
	 * <p>
	 * 是否是 DTD 样式的 XML 文档
	 */
	private boolean hasDoctype(String content) {
		return content.contains(DOCTYPE);
	}

	/**
	 * Does the supplied content contain an XML opening tag. If the parse state is currently
	 * in an XML comment then this method always returns false. It is expected that all comment
	 * tokens will have consumed for the supplied content before passing the remainder to this method.
	 * <p>
	 * 如果找到了注释的标签，则表示立马是 false，会消耗完所有的内容
	 */
	private boolean hasOpeningTag(String content) {
		if (this.inComment) {
			return false;
		}
		int openTagIndex = content.indexOf('<');
		return (openTagIndex > -1 && (content.length() > openTagIndex + 1) &&
				Character.isLetter(content.charAt(openTagIndex + 1)));
	}

	/**
	 * Consume all leading and trailing comments in the given String and return
	 * the remaining content, which may be empty since the supplied content might
	 * be all comment data.
	 * <p>
	 * 将在所在行的注释标签剥离
	 */
	@Nullable
	private String consumeCommentTokens(String line) {
		// 获取注释开始的位置
		int indexOfStartComment = line.indexOf(START_COMMENT);
		// 如果没有包含注释
		if (indexOfStartComment == -1 && !line.contains(END_COMMENT)) {
			// 直接返回行
			return line;
		}

		String result = "";
		String currLine = line;
		if (indexOfStartComment >= 0) {
			result = line.substring(0, indexOfStartComment);
			currLine = line.substring(indexOfStartComment);
		}
		// 循环遍历行数
		while ((currLine = consume(currLine)) != null) {
			if (!this.inComment && !currLine.trim().startsWith(START_COMMENT)) {
				return result + currLine;
			}
		}
		return null;
	}

	/**
	 * Consume the next comment token, update the "inComment" flag
	 * and return the remaining content.
	 * <p>
	 * 获取下一个注释的内容
	 */
	@Nullable
	private String consume(String line) {
		// 如果在注释中，返回注释结束的位置。如果不在注释中，则返回注释开始的地方
		int index = (this.inComment ? endComment(line) : startComment(line));
		return (index == -1 ? null : line.substring(index));
	}

	/**
	 * Try to consume the {@link #START_COMMENT} token.
	 *
	 * @see #commentToken(String, String, boolean)
	 * <p>
	 * 获取注释开始的位置
	 */
	private int startComment(String line) {
		return commentToken(line, START_COMMENT, true);
	}

	// 获取注释结束的位置
	private int endComment(String line) {
		return commentToken(line, END_COMMENT, false);
	}

	/**
	 * Try to consume the supplied token against the supplied content and update the
	 * in comment parse state to the supplied value. Returns the index into the content
	 * which is after the token or -1 if the token is not found.
	 * <p>
	 * 虎丘注释的位置信息
	 */
	private int commentToken(String line, String token, boolean inCommentIfPresent) {
		int index = line.indexOf(token);
		if (index > -1) {
			this.inComment = inCommentIfPresent;
		}
		return (index == -1 ? index : index + token.length());
	}

}
