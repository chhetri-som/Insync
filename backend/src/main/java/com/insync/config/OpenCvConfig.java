package com.insync.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OpenCvConfig {

    @PostConstruct
    public void loadNativeLibrary() {
        OpenCV.loadLocally();
        log.info("OpenCV native library loaded: {}", Core.getVersionString());
    }
}
