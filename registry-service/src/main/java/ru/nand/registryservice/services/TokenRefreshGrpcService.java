package ru.nand.registryservice.services;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.nand.registryservice.entities.User;
import ru.nand.registryservice.grpc.TokenServiceGrpc;
import ru.nand.registryservice.grpc.TokenServiceProto.TokenRequest;
import ru.nand.registryservice.grpc.TokenServiceProto.TokenResponse;
import ru.nand.registryservice.utils.JwtUtil;

@Slf4j
@GrpcService
public class TokenRefreshGrpcService extends TokenServiceGrpc.TokenServiceImplBase {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    public TokenRefreshGrpcService(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    public void refreshToken(TokenRequest request, StreamObserver<TokenResponse> responseObserver) {
        String expiredToken = request.getExpiredToken();
        log.debug("Принял запрос на обновление токена: {}", expiredToken);

        try {
            if (expiredToken != null && expiredToken.startsWith("Bearer ")) {
                expiredToken = expiredToken.substring(7);
            }

            // Извлекаем имя пользователя
            String username = jwtUtil.extractUsername(expiredToken);
            User user = userService.findByUsername(username);

            // Если пользователь заблокирован
            if (user.getIsBlocked()) {
                log.warn("Пользователь {} заблокирован, обновление невозможно", username);
                responseObserver.onError(
                        Status.PERMISSION_DENIED
                                .withDescription("Пользователь заблокирован")
                                .asRuntimeException()
                );
                return;
            }

            String refreshedToken = jwtUtil.generateToken(user);

            // Формируем ответ
            TokenResponse response = TokenResponse.newBuilder()
                    .setRefreshedToken(refreshedToken)
                    .build();

            // И отправляем ответ клиенту
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Ошибка обновления токена", e);
            responseObserver.onError(
                    Status.UNAUTHENTICATED
                            .withDescription("Недействительный токен")
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }
}
