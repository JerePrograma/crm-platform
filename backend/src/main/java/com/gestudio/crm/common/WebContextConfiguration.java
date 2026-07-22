package com.gestudio.crm.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class WebContextConfiguration implements WebMvcConfigurer {

  private final ActorLoggingInterceptor actorLoggingInterceptor;

  public WebContextConfiguration(ActorLoggingInterceptor actorLoggingInterceptor) {
    this.actorLoggingInterceptor = actorLoggingInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(actorLoggingInterceptor);
  }
}
