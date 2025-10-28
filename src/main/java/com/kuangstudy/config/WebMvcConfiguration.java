package com.kuangstudy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Value("${file.staticPatternPath}")
    private String staticPatternPath;

    @Value("${file.uploadFolder}")
    private String uploadFolder;

    //这个就是springboot中springMVC让开发者去配置文件上传的额外静态资源服务配置
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //注册额外静态资源目录
        //registry.addResourceHandler("访问路径").addResourceLocations("file:服务器资源所在路径");
        //registry.addResourceHandler("/upload/file/**").addResourceLocations("file:D://02_ALL_WORKSPACE/99_tmp/");
        registry.addResourceHandler(staticPatternPath).addResourceLocations("file:"+uploadFolder);
    }
}
