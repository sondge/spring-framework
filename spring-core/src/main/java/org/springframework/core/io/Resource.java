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

import org.springframework.lang.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * 资源的抽象接口，比如文件和类路径的相关接口
 * Interface for a resource descriptor that abstracts from the actual
 * type of underlying resource, such as a file or class path resource.
 * 一种可以如果存在可以打开每种资源的输入流，包括物理磁盘，URL 地址货真文件处理， 但是特定的实现只能返回某些文件资源
 * <p>An InputStream can be opened for every resource if it exists in
 * physical form, but a URL or File handle can just be returned for
 * certain resources. The actual behavior is implementation-specific.
 *
 * @author Juergen Hoeller
 * @see #getInputStream()
 * @see #getURL()
 * @see #getURI()
 * @see #getFile()
 * @see WritableResource
 * @see ContextResource
 * @see UrlResource
 * @see FileUrlResource
 * @see FileSystemResource
 * @see ClassPathResource
 * @see ByteArrayResource
 * @see InputStreamResource
 * 继承自InputStreamResource
 * @since 28.12.2003
 */
public interface Resource extends InputStreamSource {

	/**
	 * Determine whether this resource actually exists in physical form.
	 * <p>This method performs a definitive existence check, whereas the
	 * existence of a {@code Resource} handle only guarantees a valid
	 * descriptor handle.
	 * 判断资源是否已以物理的形式存在
	 */

	boolean exists();

	/**
	 * Indicate whether non-empty contents of this resource can be read via
	 * {@link #getInputStream()}.
	 * <p>Will be {@code true} for typical resource descriptors that exist
	 * since it strictly implies {@link #exists()} semantics as of 5.1.
	 * Note that actual content reading may still fail when attempted.
	 * However, a value of {@code false} is a definitive indication
	 * that the resource content cannot be read.
	 * 只是是否可以通过 getInputStream 读取非空内容。对于存在的描述存在的典型资源将为 true。 因为从 5.1
	 *  版本开始他严格隐含了 exists 的含义
	 *
	 * @see #getInputStream()
	 * @see #exists()
	 */
	default boolean isReadable() {
		return exists();
	}

	/**
	 * Indicate whether this resource represents a handle with an open stream.
	 * If {@code true}, the InputStream cannot be read multiple times,
	 * and must be read and closed to avoid resource leaks.
	 * <p>Will be {@code false} for typical resource descriptors.
	 * 判断文件是否被打开，如果范围为 true 表明不能被打开
	 * 如果返回为 false 的话，表示是一个普通可以被打开的文件
	 */
	default boolean isOpen() {
		return false;
	}

	/**
	 * Determine whether this resource represents a file in a file system.
	 * A value of {@code true} strongly suggests (but does not guarantee)
	 * that a {@link #getFile()} call will succeed.
	 * <p>This is conservatively {@code false} by default.
	 *
	 * @see #getFile()
	 * 表明打开的资源是否是一个文件
	 * @since 5.0
	 */
	default boolean isFile() {
		return false;
	}

	/**
	 * Return a URL handle for this resource.
	 *
	 * @throws IOException if the resource cannot be resolved as URL,
	 *                     i.e. if the resource is not available as descriptor
	 *                     将资源返回成返回一个 URL，如果是一个不能被解析的 URL 和不可以的资源都会包 IOException
	 */
	URL getURL() throws IOException;

	/**
	 * Return a URI handle for this resource.
	 *
	 * @throws IOException if the resource cannot be resolved as URI,
	 *                     i.e. if the resource is not available as descriptor
	 * @since 2.5
	 * <p>
	 * 将资源返回成一个 URI 资源，如果是一个不能解析的 URI 资源或者不被新人的 资源将报 IOException
	 */
	URI getURI() throws IOException;

	/**
	 * Return a File handle for this resource.
	 *
	 * @throws java.io.FileNotFoundException if the resource cannot be resolved as
	 *                                       absolute file path, i.e. if the resource is not available in a file system
	 * @throws IOException                   in case of general resolution/reading failures
	 * @see #getInputStream()
	 * 从资源中返回一个文件对象，如果资源不能被解析则抛出 FileNotFoundException。 如果资源读取失败则抛出 IOException
	 */
	File getFile() throws IOException;

	/**
	 * Return a {@link ReadableByteChannel}.
	 * <p>It is expected that each call creates a <i>fresh</i> channel.
	 * <p>The default implementation returns {@link Channels#newChannel(InputStream)}
	 * with the result of {@link #getInputStream()}.
	 *
	 * @return the byte channel for the underlying resource (must not be {@code null})
	 * @throws java.io.FileNotFoundException if the underlying resource doesn't exist
	 * @throws IOException                   if the content channel could not be opened
	 * @see #getInputStream()
	 * 读取资源的字节流
	 * @since 5.0
	 */
	default ReadableByteChannel readableChannel() throws IOException {
		return Channels.newChannel(getInputStream());
	}

	/**
	 * Determine the content length for this resource.
	 *
	 * @throws IOException if the resource cannot be resolved
	 *                     (in the file system or as some other known physical resource type)
	 *                     <p>
	 *                     算出资源的内容长度（只能在文件系统或者其他一直的物理资源类型），
	 */
	long contentLength() throws IOException;

	/**
	 * Determine the last-modified timestamp for this resource.
	 *
	 * @throws IOException if the resource cannot be resolved
	 *                     (in the file system or as some other known physical resource type)
	 *                     返回上一次修改的时间戳
	 */
	long lastModified() throws IOException;

	/**
	 * Create a resource relative to this resource.
	 *
	 * @param relativePath the relative path (relative to this resource)
	 * @return the resource handle for the relative resource
	 * @throws IOException if the relative resource cannot be determined
	 *                     相对本资源创建一个新的资源
	 */
	Resource createRelative(String relativePath) throws IOException;

	/**
	 * Determine a filename for this resource, i.e. typically the last
	 * part of the path: for example, "myfile.txt".
	 * <p>Returns {@code null} if this type of resource does not
	 * have a filename.
	 * 返回这个资源的名称，通常只包含文件
	 */
	@Nullable
	String getFilename();

	/**
	 * Return a description for this resource,
	 * to be used for error output when working with the resource.
	 * <p>Implementations are also encouraged to return this value
	 * from their {@code toString} method.
	 *
	 * @see Object#toString()
	 * 返回资源的描述信息
	 */
	String getDescription();

}
