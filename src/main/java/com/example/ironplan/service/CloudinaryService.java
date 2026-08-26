package com.example.ironplan.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    public Map<?, ?> upload(MultipartFile multipartFile, String folder) throws IOException {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("El archivo de imagen está vacío.");
        }

        Map<?, ?> result = cloudinary.uploader().upload(
                multipartFile.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "image"
                )
        );

        if (result == null || result.get("secure_url") == null) {
            throw new IOException("Cloudinary no devolvió la URL de la imagen.");
        }
        return result;
    }
}
