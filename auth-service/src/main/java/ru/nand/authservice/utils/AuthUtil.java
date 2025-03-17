package ru.nand.authservice.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import ru.nand.authservice.entities.DTO.TokenResponse;

@Slf4j
@Component
public class AuthUtil {

    /// Ошибки валидации
    public ResponseEntity<TokenResponse> handleValidationErrors(BindingResult bindingResult) {
        StringBuilder errorMessage = new StringBuilder("Ошибки валидации:\n");
        for (FieldError error : bindingResult.getFieldErrors()) {
            errorMessage.append(error.getField())
                    .append(": ")
                    .append(error.getDefaultMessage())
                    .append("\n");
        }
        return ResponseEntity.status(400).body(new TokenResponse(null, null, errorMessage.toString()));
    }
}
