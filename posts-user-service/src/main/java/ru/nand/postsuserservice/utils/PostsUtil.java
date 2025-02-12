package ru.nand.postsuserservice.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Component
public class PostsUtil {
    @Value("${images.path}")
    private String imagesDir;

    // Сохранение изображения
    public String saveImage(MultipartFile imageFile) {
        try {
            File directory = new File(imagesDir);
            if (!directory.exists() && !directory.mkdirs()) {
                log.error("Не удалось создать каталог для изображений: {}", directory.getAbsolutePath());
                throw new RuntimeException("Не удалось создать каталог для изображений");
            }

            String imageName = UUID.randomUUID().toString() + ".png";
            File destination = new File(directory, imageName);
            imageFile.transferTo(destination);
            log.debug("Изображение сохранено как: {}", destination.getAbsolutePath());

            return imageName;
        } catch (IOException e) {
            log.error("Ошибка при сохранении изображения: {}", e.getMessage());
            throw new RuntimeException("Ошибка при сохранении изображения");
        }
    }

    // Кодирование изображения в Base64
    public String encodeImageToBase64(String filename) {
        if (filename == null || filename.isEmpty()) return null;

        File imageFile = new File(imagesDir, filename);
        if (!imageFile.exists()) {
            log.warn("Изображение не найдено: {}", imageFile.getAbsolutePath());
            return null;
        }

        try {
            byte[] fileContent = Files.readAllBytes(imageFile.toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            log.error("Ошибка при чтении изображения: {}", e.getMessage());
            return null;
        }
    }

    // Удаление изображения из папки
    public boolean deleteImage(String filename){
        if(filename == null || filename.isEmpty()) return false;

        File imageFile = new File(imagesDir, filename);
        if(imageFile.exists()){
            boolean isDeleted = imageFile.delete();
            if(isDeleted){
                log.debug("Изображени удалено: {}", imageFile.getAbsolutePath());
            } else {
                log.warn("Не удалось удалить изображение: {}", imageFile.getAbsolutePath());
            }

            return isDeleted;
        } else {
            log.warn("Изображение для удаления не найдено: {}", imageFile.getAbsolutePath());
            return false;
        }
    }
}
