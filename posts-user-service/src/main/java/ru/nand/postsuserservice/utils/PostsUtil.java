package ru.nand.postsuserservice.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import ru.nand.postsuserservice.services.YandexDiskService;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class PostsUtil {
    private final YandexDiskService yandexDiskService;
    private final RestTemplate restTemplate;

    @Autowired
    public PostsUtil(YandexDiskService yandexDiskService, RestTemplate restTemplate) {
        this.yandexDiskService = yandexDiskService;
        this.restTemplate = restTemplate;
    }

    /// Выдача уникальных названий и загрузка на диск
    public List<String> uploadImages(List<MultipartFile> images){
        List<String> uploadedImagesUrls = new ArrayList<>();

        for(MultipartFile image : images){
            try{
                // Делаем уникальное имя файлу
                String uniqueFileName = generateUniqueFileName(image.getOriginalFilename());

                // Кидаем на диск
                yandexDiskService.upload(image.getInputStream(), "/imagesFolder/" + uniqueFileName);

                // Сохраняем уникальное имя
                uploadedImagesUrls.add(uniqueFileName);
            } catch (IOException e) {
                log.warn("Ошибка при загрузке файла: {}", e.getMessage());
                throw new RuntimeException("Ошибка при загрузке файла: " + e.getMessage());
            }
        }

        log.debug("Выданы уникальные названия: {}", uploadedImagesUrls);
        return uploadedImagesUrls;
    }

    /// Генерация уникальных названий
    public String generateUniqueFileName(String originalFilename) {
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));

        String uniqueFileName = UUID.randomUUID() + fileExtension;
        log.debug("Сгенерировано уникальное название файлу: {}", uniqueFileName);
        return uniqueFileName;
    }

    /// Получение ссылки на скачивание
    public String getDownloadLink(String path) throws IOException{
        return yandexDiskService.download(path);
    }

    /// Загрузка изображения с Яндекс.Диска и кодирование в Base64
    public String downloadAndEncodeImage(String imageName) throws IOException {
        // Формируем путь к файлу на Яндекс.Диске
        String path = "/imagesFolder/" + imageName;

        // Получаем ссылку для скачивания
        String downloadLink = getDownloadLink(path);

        // Декодируем ссылку
        String decodedLink = URLDecoder.decode(downloadLink, StandardCharsets.UTF_8);

        // Загружаем файл по ссылке
        byte[] imageBytes = downloadFile(decodedLink);

        // Кодируем изображение в Base64
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /// Скачивание файла
    public byte[] downloadFile(String downloadLink) throws IOException {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(downloadLink, byte[].class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.warn("Ошибка при скачивании файла по ссылке: {}", response.getStatusCode());
            throw new IOException("Ошибка скачивания файла: " + response.getStatusCode());
        }
        return response.getBody();
    }

    /// Удаление файла с Яндекс.Диска
    public void deleteFile(String path) throws IOException {
        yandexDiskService.deleteFile(path);
        log.debug("Файл {} удален с диска", path);
    }
}
