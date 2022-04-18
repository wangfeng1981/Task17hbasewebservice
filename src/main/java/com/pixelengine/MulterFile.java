package com.pixelengine ;

import javax.servlet.MultipartConfigElement;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;


@Configuration
public  class MulterFile {
    /**
     * File upload configuration
     * @return
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        // Maximum file size
        factory.setMaxFileSize( DataSize.ofMegabytes(2)); // 2MB
        // / Set the total upload data size
        factory.setMaxRequestSize(DataSize.ofMegabytes(2));
        return factory.createMultipartConfig() ;
    }
}