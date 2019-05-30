/*
 * Copyright 2016-2017 the original author or authors.
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

package org.joinfaces.autoconfigure.rewrite;


import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

import org.joinfaces.autoconfigure.servlet.WebFragmentRegistrationBean;
import org.ocpsoft.rewrite.servlet.RewriteFilter;
import org.ocpsoft.rewrite.servlet.impl.RewriteServletContextListener;
import org.ocpsoft.rewrite.servlet.impl.RewriteServletRequestListener;
import org.ocpsoft.rewrite.spring.SpringExpressionLanguageProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Spring Boot Auto Configuration of Rewrite.
 *
 * @author Marcelo Fernandes
 * @author Lars Grefer
 */
@Configuration
@EnableConfigurationProperties(RewriteProperties.class)
@ConditionalOnClass(RewriteFilter.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RewriteAutoConfiguration {

	@Autowired
	private RewriteProperties rewriteProperties;

	/**
	 * This {@link WebFragmentRegistrationBean} is equivalent to the
	 * {@code META-INF/web-fragment.xml} of the {@code rewrite-servlet.jar}.
	 *
	 * @return rewriteWebFragmentRegistrationBean
	 */
	@Bean
	@DependsOn("applicationContextProvider")
	public WebFragmentRegistrationBean rewriteWebFragmentRegistrationBean() {
		WebFragmentRegistrationBean bean = new WebFragmentRegistrationBean() {
			@Override
			public void customize(ConfigurableServletWebServerFactory factory) {
				super.customize(factory);
				factory.addInitializers(servletContext -> {

					FilterRegistration.Dynamic rewriteFilterRegistration = servletContext.addFilter("OCPsoft Rewrite Filter", RewriteFilter.class);
					rewriteFilterRegistration.setAsyncSupported(true);
					rewriteFilterRegistration.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
				});
			}
		};

		bean.getListeners().add(RewriteServletRequestListener.class);
		bean.getListeners().add(RewriteServletContextListener.class);

		return bean;
	}

	@Bean
	public ApplicationContextProvider applicationContextProvider() {
		return new ApplicationContextProvider();
	}

	@Bean
	public SpringExpressionLanguageProvider rewriteExpressionLanguageProvider() {
		return new SpringExpressionLanguageProvider();
	}

	@Bean
	public SpringBootBeanNameResolver rewriteBeanNameResolver(ApplicationContext applicationContext) {
		return new SpringBootBeanNameResolver(applicationContext);
	}

	/**
	 * This {@link SpringBootAnnotationConfigProvider} adds a
	 * {@link org.ocpsoft.rewrite.servlet.config.HttpConfigurationProvider} which scans for Rewrite annotations within
	 * the classpath.
	 *
	 * @return rewrite annotation scanner
	 */
	@Bean
	public SpringBootAnnotationConfigProvider rewriteAnnotationConfigProvider() {
		return new SpringBootAnnotationConfigProvider(this.rewriteProperties.getAnnotationConfigProvider());
	}
}
