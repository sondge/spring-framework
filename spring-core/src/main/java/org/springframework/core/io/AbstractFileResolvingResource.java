/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.io;

import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;

/**
 * Abstract base class for resources which resolve URLs into File references,
 * such as {@link UrlResource} or {@link ClassPathResource}.
 *
 * <p>Detects the "file" protocol as well as the JBoss "vfs" protocol in URLs,
 * resolving file system references accordingly.
 *
 * @author Juergen Hoeller
 * @since 3.0
 *
 *  处理文件资源
 */
public abstract class AbstractFileResolvingResource extends AbstractResource {

	@Override
	// 判断文件是否存在
	public boolean exists() {
		try {
			// 获取文件的 URL
			URL url = getURL();
			// 如果是文件的 URL，包含三种文件协议：file vfs  vfsfile
			if (ResourceUtils.isFileURL(url)) {
				// Proceed with file system resolution
				// 直接返回在文件系统是否存在
				return getFile().exists();
			}
			else {
				// Try a URL connection content-length header
				// 从 url 中打开连接
				URLConnection con = url.openConnection();
				// 设置连接
				customizeConnection(con);
				// 获取 HTTP 连接
				HttpURLConnection httpCon =
						(con instanceof HttpURLConnection ? (HttpURLConnection) con : null);
				// 如果是 HTTP 连接
				if (httpCon != null) {
					// 获取 http 返回的 code
					int code = httpCon.getResponseCode();
					// 如果 连接成功，返回 true
					if (code == HttpURLConnection.HTTP_OK) {
						return true;
					}
					// 如果返回的是 404 则返回不存在
					else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
						return false;
					}
				}
				// 如果连接的上下文长度大于 0 返回 true
				if (con.getContentLengthLong() > 0) {
					return true;
				}
				// 如果是 http 连接，关闭该连接
				if (httpCon != null) {
					// No HTTP OK status, and no content-length header: give up
					httpCon.disconnect();
					return false;
				}
				else {
					// Fall back to stream existence: can we open the stream?
					// 关闭文件刘
					getInputStream().close();
					return true;
				}
			}
		}
		catch (IOException ex) {
			return false;
		}
	}

	@Override
	// 判断文件是否是可读的
	public boolean isReadable() {
		try {
			// 获取文件 URL 路径
			URL url = getURL();
			// 如果是文件，直接返回文件的可读权限
			if (ResourceUtils.isFileURL(url)) {
				// Proceed with file system resolution
				File file = getFile();
				return (file.canRead() && !file.isDirectory());
			}
			else {
				// Try InputStream resolution for jar resources
				// 尝试获取路径的连接
				URLConnection con = url.openConnection();
				// 自定义用户连接
				customizeConnection(con);
				// 如果链接是 http 链接
				if (con instanceof HttpURLConnection) {
					HttpURLConnection httpCon = (HttpURLConnection) con;
					// 获取 code
					int code = httpCon.getResponseCode();
					// 如果 code 的响应不为 OK， 则为不可读
					if (code != HttpURLConnection.HTTP_OK) {
						httpCon.disconnect();
						return false;
					}
				}
				// 获取连接的长度
				long contentLength = con.getContentLengthLong();
				// 如果长度 大于 0，返回可读
				if (contentLength > 0) {
					return true;
				}
				// 如果长度为 0，直接返回不可读
				else if (contentLength == 0) {
					// Empty file or directory -> not considered readable...
					return false;
				}
				else {
					// Fall back to stream existence: can we open the stream?
					// 关闭输入流
					getInputStream().close();
					return true;
				}
			}
		}
		catch (IOException ex) {
			return false;
		}
	}

	@Override
	public boolean isFile() {
		try {
			// 获取 url 路径，如果文件的协议为 vfs
			URL url = getURL();
			// 获取资源然后判断是否是文件
			if (url.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
				return VfsResourceDelegate.getResource(url).isFile();
			}
			// 获取文件的
			return ResourceUtils.URL_PROTOCOL_FILE.equals(url.getProtocol());
		}
		catch (IOException ex) {
			return false;
		}
	}

	/**
	 * This implementation returns a File reference for the underlying class path
	 * resource, provided that it refers to a file in the file system.
	 * @see org.springframework.util.ResourceUtils#getFile(java.net.URL, String)
	 */
	@Override
	public File getFile() throws IOException {
		// 获取 URL
		URL url = getURL();
		if (url.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
			return VfsResourceDelegate.getResource(url).getFile();
		}
		// 获取文件
		return ResourceUtils.getFile(url, getDescription());
	}

	/**
	 * This implementation determines the underlying File
	 * (or jar file, in case of a resource in a jar/zip).
	 *
	 * 获取上次修改的检查
	 */
	@Override
	protected File getFileForLastModifiedCheck() throws IOException {
		// 获取文件的 URL
		URL url = getURL();
		// 如果是 jar 的url
		if (ResourceUtils.isJarURL(url)) {
			// 解析 jar 包的 URL
			URL actualUrl = ResourceUtils.extractArchiveURL(url);
			if (actualUrl.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
				return VfsResourceDelegate.getResource(actualUrl).getFile();
			}
			return ResourceUtils.getFile(actualUrl, "Jar URL");
		}
		else {
			return getFile();
		}
	}

	/**
	 * This implementation returns a File reference for the given URI-identified
	 * resource, provided that it refers to a file in the file system.
	 * 根据 uri 信息判断是否是文件
	 * @since 5.0
	 * @see #getFile(URI)
	 */
	protected boolean isFile(URI uri) {
		try {
			if (uri.getScheme().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
				return VfsResourceDelegate.getResource(uri).isFile();
			}
			return ResourceUtils.URL_PROTOCOL_FILE.equals(uri.getScheme());
		}
		catch (IOException ex) {
			return false;
		}
	}

	/**
	 * This implementation returns a File reference for the given URI-identified
	 * resource, provided that it refers to a file in the file system.
	 * @see org.springframework.util.ResourceUtils#getFile(java.net.URI, String)
	 *
	 * 根据 URI 获取文件
	 */
	protected File getFile(URI uri) throws IOException {
		if (uri.getScheme().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
			return VfsResourceDelegate.getResource(uri).getFile();
		}
		return ResourceUtils.getFile(uri, getDescription());
	}

	/**
	 * This implementation returns a FileChannel for the given URI-identified
	 * resource, provided that it refers to a file in the file system.
	 * @since 5.0
	 * @see #getFile()
	 *
	 * 获取文件的 ReadableByteChannel 信息
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		try {
			// Try file system channel
			return FileChannel.open(getFile().toPath(), StandardOpenOption.READ);
		}
		catch (FileNotFoundException | NoSuchFileException ex) {
			// Fall back to InputStream adaptation in superclass
			return super.readableChannel();
		}
	}

	@Override
	// 获取上下文长度
	public long contentLength() throws IOException {
		// 获取 url 的长度
		URL url = getURL();
		if (ResourceUtils.isFileURL(url)) {
			// Proceed with file system resolution
			// 获取文件系统的文件
			File file = getFile();
			// 获取文件的长度
			long length = file.length();
			if (length == 0L && !file.exists()) {
				throw new FileNotFoundException(getDescription() +
						" cannot be resolved in the file system for checking its content length");
			}
			return length;
		}
		else {
			// Try a URL connection content-length header
			// 获取 url 的连接
			URLConnection con = url.openConnection();
			//用户自定义连接
			customizeConnection(con);
			// 获取上下文长度
			return con.getContentLengthLong();
		}
	}

	@Override
	// 获取上一次修改的时间
	public long lastModified() throws IOException {
		// 获取 URL 资源
		URL url = getURL();
		// 文件是否检查
		boolean fileCheck = false;
		if (ResourceUtils.isFileURL(url) || ResourceUtils.isJarURL(url)) {
			// Proceed with file system resolution
			// 设计已经检查
			fileCheck = true;
			try {
				// 获取上一次的检查文件
				File fileToCheck = getFileForLastModifiedCheck();
				// 获取上一次的检查时间
				long lastModified = fileToCheck.lastModified();
				// 如果文件存在或者修改时间不非法
				if (lastModified > 0L || fileToCheck.exists()) {
					return lastModified;
				}
			}
			catch (FileNotFoundException ex) {
				// Defensively fall back to URL connection check instead
			}
		}
		// Try a URL connection last-modified header
		// 获取 url 的连接
		URLConnection con = url.openConnection();
		// 用户自定义链接
		customizeConnection(con);
		// 获取上一次连接的时间
		long lastModified = con.getLastModified();
		if (fileCheck && lastModified == 0 && con.getContentLengthLong() <= 0) {
			throw new FileNotFoundException(getDescription() +
					" cannot be resolved in the file system for checking its last-modified timestamp");
		}
		return lastModified;
	}

	/**
	 * Customize the given {@link URLConnection}, obtained in the course of an
	 * {@link #exists()}, {@link #contentLength()} or {@link #lastModified()} call.
	 * <p>Calls {@link ResourceUtils#useCachesIfNecessary(URLConnection)} and
	 * delegates to {@link #customizeConnection(HttpURLConnection)} if possible.
	 * Can be overridden in subclasses.
	 * @param con the URLConnection to customize
	 * @throws IOException if thrown from URLConnection methods
	 *
	 *
	 * 获取用户自定义链接
	 */
	protected void customizeConnection(URLConnection con) throws IOException {
		ResourceUtils.useCachesIfNecessary(con);
		if (con instanceof HttpURLConnection) {
			customizeConnection((HttpURLConnection) con);
		}
	}

	/**
	 * Customize the given {@link HttpURLConnection}, obtained in the course of an
	 * {@link #exists()}, {@link #contentLength()} or {@link #lastModified()} call.
	 * <p>Sets request method "HEAD" by default. Can be overridden in subclasses.
	 * @param con the HttpURLConnection to customize
	 * @throws IOException if thrown from HttpURLConnection methods
	 * 如果是 HTTP 需要设置 HEAD
	 */
	protected void customizeConnection(HttpURLConnection con) throws IOException {
		con.setRequestMethod("HEAD");
	}


	/**
	 * Inner delegate class, avoiding a hard JBoss VFS API dependency at runtime.
	 */
	private static class VfsResourceDelegate {

		public static Resource getResource(URL url) throws IOException {
			return new VfsResource(VfsUtils.getRoot(url));
		}

		public static Resource getResource(URI uri) throws IOException {
			return new VfsResource(VfsUtils.getRoot(uri));
		}
	}

}
