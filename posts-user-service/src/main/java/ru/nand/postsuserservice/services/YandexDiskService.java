package ru.nand.postsuserservice.services;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class YandexDiskService {

    @Setter
    @Getter
    @Value("${yandex.token}")
    private String token;

    private final RestTemplate restTemplate;

    public YandexDiskService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /// Установка
    public String download(String path) throws IOException {
        final String baseUrl = "https://cloud-api.yandex.net/v1/disk/resources/download";
        RequestEntity<Void> requestEntity = RequestEntity.get(
                        UriComponentsBuilder.fromUriString(baseUrl)
                                .queryParam("path", path)
                                .build().toUri()
                )
                .header("Authorization", "OAuth " + token)
                .build();

        ResponseEntity<Link> response = restTemplate.exchange(requestEntity, Link.class);
        if (!response.getStatusCode().equals(HttpStatusCode.valueOf(200))) {
            log.warn("Ошибка при получении ссылки");
            throw new IOException("Ошибка при получении ссылки");
        }
        return response.getBody().href();
    }

    /// Загрузка на диск
    public void upload(InputStream is, String fullFileName) throws IOException {
        final String baseUrl = "https://cloud-api.yandex.net/v1/disk/resources/upload";

        RequestEntity<Void> requestEntity = RequestEntity.get(
                        UriComponentsBuilder.fromUriString(baseUrl)
                                .queryParam("path", fullFileName)
                                .build()
                                .toUri()
                ).header("Authorization", "OAuth " + token)
                .build();

        ResponseEntity<Link> linkResponseEntity = restTemplate.exchange(requestEntity, Link.class);

        String link = linkResponseEntity.getBody().href();

        RequestEntity<byte[]> requestToUpload = RequestEntity.put(
                UriComponentsBuilder.fromUriString(link)
                        .build()
                        .toUri()
        ).body(is.readAllBytes());

        ResponseEntity<String> responseToUpload = restTemplate.exchange(
                requestToUpload, String.class
        );

        if (responseToUpload.getStatusCode().is2xxSuccessful()) {
            log.debug("Файл успешно загружен");
        } else {
            System.out.println(responseToUpload.getBody() + " | " + responseToUpload.getStatusCode());
        }
    }

    /// Создание директории
    public void createDirectory(String path) {
        final String baseUrl = "https://cloud-api.yandex.net/v1/disk/resources";
        RequestEntity<Void> requestEntity = RequestEntity.put(
                        UriComponentsBuilder.fromUriString(baseUrl)
                                .queryParam("path", path)
                                .build().toUri()
                )
                .header("Authorization", "OAuth " + token)
                .build();
        try {
            ResponseEntity<String> exchange = restTemplate.exchange(requestEntity, String.class);
            HttpStatusCode statusCode = exchange.getStatusCode();
            if (statusCode.equals(HttpStatusCode.valueOf(201))) {
                log.debug("Успешное создание директории");
            }
        } catch (Exception e) {
            log.warn("Директория уже существует");
        }
    }

    /// Удаление файла с Яндекс.Диска
    public void deleteFile(String path) throws IOException {
        final String baseUrl = "https://cloud-api.yandex.net/v1/disk/resources";

        // Формируем запрос на удаление
        RequestEntity<Void> requestEntity = RequestEntity.delete(
                        UriComponentsBuilder.fromUriString(baseUrl)
                                .queryParam("path", path)
                                .build()
                                .toUri()
                )
                .header("Authorization", "OAuth " + token)
                .build();

        // Выполняем запрос
        ResponseEntity<String> response = restTemplate.exchange(requestEntity, String.class);

        // Проверяем статус ответа
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("Ошибка при удалении файла: " + response.getStatusCode());
        }
        log.debug("Файл успешно удален: {}", response.getBody());
    }

}

record Link(String href, String method, boolean templated) {
}