/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.joinfaces.autoconfigure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import org.springframework.util.StringUtils;

/**
 * Utility class for handling classpath scan results.
 *
 * @author Lars Grefer
 */
@Slf4j
@UtilityClass
public class ClasspathScanUtil {

	public static Set<Class<?>> readAnnotationClassSet(InputStream inputStream, ClassLoader classLoader) throws IOException {
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			return getClasses(bufferedReader.lines(), classLoader);
		}
	}

	public static Map<Class<? extends Annotation>, Set<Class<?>>> readAnnotationClassMap(InputStream inputStream, ClassLoader classLoader) throws IOException {

		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			Map<Class<? extends Annotation>, Set<Class<?>>> classes = new HashMap<>();

			bufferedReader.lines().forEach(line -> {
				String[] split = line.split("=", 2);
				String annotationName = split[0];
				String classNameList = split[1];

				Class<? extends Annotation> annotation;
				try {
					annotation = (Class<? extends Annotation>) classLoader.loadClass(annotationName);
				}
				catch (ClassNotFoundException | LinkageError e) {
					log.warn("Failed to load annotation class {}", annotationName, e);
					return;
				}
				Set<Class<?>> classSet;

				if (StringUtils.hasText(classNameList)) {
					classSet = getClasses(Arrays.stream(classNameList.split(",")), classLoader);
				}
				else {
					classSet = Collections.emptySet();
				}

				classes.put(annotation, classSet);
			});
			return classes;
		}
	}

	public static Set<Class<?>> getClasses(Stream<String> classNames, ClassLoader classLoader) {
		AtomicInteger missingClasses = new AtomicInteger();
		AtomicInteger missingDependentClasses = new AtomicInteger();

		Set<Class<?>> collect = classNames
				.map(className -> {
					try {
						return classLoader.loadClass(className);
					}
					catch (ClassNotFoundException e) {
						missingClasses.incrementAndGet();
						log.debug("Failed to load class {} although it's listed in the prepared scan result.", className);
						log.trace("Stacktrace", e);
					}
					catch (NoClassDefFoundError e) {
						missingDependentClasses.incrementAndGet();
						log.debug("Failed to load class {} because it's dependency {} is missing.", className, e.getMessage());
						log.trace("Stacktrace", e);
					}
					catch (LinkageError e) {
						log.warn("Failed to load class {} from prepared scan result", className, e);
					}
					return null;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (missingClasses.get() > 0) {
			log.warn("{} classes listed in the prepared scan result could not be found. Set the log-level to debug for more information.", missingClasses.get());
		}
		if (missingDependentClasses.get() > 0) {
			log.info("{} classes failed to load, because some of their dependencies are missing. Set the log-level to debug for more information.", missingDependentClasses.get());
		}
		return collect;
	}
}