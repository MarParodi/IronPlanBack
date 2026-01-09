package com.example.ironplan.model;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dlqdsnuzs");
        config.put("api_key", "572745625386217");
        config.put("api_secret", "LarFuaxsCaoQKdmaBD3s8EHyvQo");
        return new Cloudinary(config);
    }
}