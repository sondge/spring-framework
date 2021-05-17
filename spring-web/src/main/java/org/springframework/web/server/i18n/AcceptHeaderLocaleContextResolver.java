/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.server.i18n;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@link LocaleContextResolver} implementation that simply uses the primary locale
 * specified in the "Accept-Language" header of the HTTP request (that is,
 * the locale sent by the client browser, normally that of the client's OS).
 * <p>
 * LocaleResolver 接口实现类
 * 简单地使用 Http 请求头中的 Accept-Language 来指定 Locale 对象（即客户浏览器发送的语言环境，通常是客户端的操作系统）
 * <p>
 * 注意：不支持 setLocale 方法，因为只能通过更改客户端的区域设置来改 Accept-Language 请求头
 * <p>Note: Does not support {@link #setLocaleContext}, since the accept header
 * can only be changed through changing the client's locale settings.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @see HttpHeaders#getAcceptLanguageAsLocales()
 * @since 5.0
 */
public class AcceptHeaderLocaleContextResolver implements LocaleContextResolver {

	private final List<Locale> supportedLocales = new ArrayList<>(4);

	@Nullable
	private Locale defaultLocale;


	/**
	 * Configure supported locales to check against the requested locales
	 * determined via {@link HttpHeaders#getAcceptLanguageAsLocales()}.
	 *
	 * 配置支持的区域设置列表
	 * @param locales the supported locales
	 */
	public void setSupportedLocales(List<Locale> locales) {
		this.supportedLocales.clear();
		this.supportedLocales.addAll(locales);
	}

	/**
	 * Return the configured list of supported locales.
	 *
	 * 返回配置的支持的区域设置列表
	 */
	public List<Locale> getSupportedLocales() {
		return this.supportedLocales;
	}

	/**
	 * Configure a fixed default locale to fall back on if the request does not
	 * have an "Accept-Language" header (not set by default)
	 *
	 * 如果 HTTP 请求头没有 Accept-Language，则使用该默认的语言环境设置.
	 *
	 * @param defaultLocale the default locale to use
	 */
	public void setDefaultLocale(@Nullable Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	/**
	 * The configured default locale, if any.
	 *
	 * 返回默认配置的语言环境（如果有）
	 * <p>This method may be overridden in subclasses.
	 */
	@Nullable
	public Locale getDefaultLocale() {
		return this.defaultLocale;
	}

	@Override
	public LocaleContext resolveLocaleContext(ServerWebExchange exchange) {
		// 获取请求中的语言环境
		List<Locale> requestLocales = null;
		try {
			requestLocales = exchange.getRequest().getHeaders().getAcceptLanguageAsLocales();
		} catch (IllegalArgumentException ex) {
			// Invalid Accept-Language header: treat as empty for matching purposes
		}
		// 返回语言环境上下文
		return new SimpleLocaleContext(resolveSupportedLocale(requestLocales));
	}

	@Nullable
	private Locale resolveSupportedLocale(@Nullable List<Locale> requestLocales) {
		// 如果请求中的语言为空，直接返回默认本地语言
		if (CollectionUtils.isEmpty(requestLocales)) {
			return getDefaultLocale();  // may be null
		}
		// 获取支持的本地语言
		List<Locale> supportedLocales = getSupportedLocales();
		// 如果支持的区域列表为空，直接返回请求中的第一个语言
		if (supportedLocales.isEmpty()) {
			return requestLocales.get(0);  // never null
		}
		// 获取语言匹配器
		Locale languageMatch = null;
		// 遍历请求中的语言
		for (Locale locale : requestLocales) {
			if (supportedLocales.contains(locale)) {
				// 如果匹配语言为空或者可以匹配对应的语言
				if (languageMatch == null || languageMatch.getLanguage().equals(locale.getLanguage())) {
					// Full match: language + country, possibly narrowed from earlier language-only match
					return locale;
				}
			// 如果 语言匹配器为空
			} else if (languageMatch == null) {
				// Let's try to find a language-only match as a fallback
				// 循环遍历支持的语言
				for (Locale candidate : supportedLocales) {
					// 重新获选匹配器
					if (!StringUtils.hasLength(candidate.getCountry()) &&
							candidate.getLanguage().equals(locale.getLanguage())) {
						languageMatch = candidate;
						break;
					}
				}
			}
		}
		// 直接返回语言匹配器
		if (languageMatch != null) {
			return languageMatch;
		}

		Locale defaultLocale = getDefaultLocale();
		return (defaultLocale != null ? defaultLocale : requestLocales.get(0));
	}

	@Override
	public void setLocaleContext(ServerWebExchange exchange, @Nullable LocaleContext locale) {
		// 不支持重新设置上下文
		throw new UnsupportedOperationException(
				"Cannot change HTTP accept header - use a different locale context resolution strategy");
	}

}
