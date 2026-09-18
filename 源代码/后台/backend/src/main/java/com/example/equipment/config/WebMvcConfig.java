package com.example.equipment.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将相对路径转为绝对路径，确保 Windows/Linux 均可正确映射
        File uploadFolder = new File(uploadDir);
        String absolutePath = uploadFolder.getAbsolutePath();
        // 确保路径以 / 结尾
        if (!absolutePath.endsWith(File.separator)) {
            absolutePath += File.separator;
        }
        log.info("静态资源映射: /uploads/** -> file:{}", absolutePath);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath);
    }
}
