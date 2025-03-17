package ru.nand.registryservice.services;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.nand.registryservice.entities.ENUMS.STATUS;
import ru.nand.registryservice.entities.User;
import ru.nand.registryservice.entities.UserSession;
import ru.nand.registryservice.grpc.TokenServiceGrpc;
import ru.nand.registryservice.grpc.TokenServiceProto.TokenRequest;
import ru.nand.registryservice.grpc.TokenServiceProto.TokenResponse;
import ru.nand.registryservice.utils.JwtUtil;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class TokenRefreshGrpcService extends TokenServiceGrpc.TokenServiceImplBase {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final UserSessionService userSessionService;

    @Override
    public void refreshToken(TokenRequest request, StreamObserver<TokenResponse> responseObserver) {

        String accessToken = request.getExpiredToken();
        log.info("Принял запрос на обновление токена по gRPC");

        try{
            if (accessToken != null && accessToken.startsWith("Bearer ")) {
                accessToken = accessToken.substring(7);
            }

            // Поиск сессии по access токену
            UserSession session = userSessionService.getSessionByAccessToken(accessToken);

            // Проверяем на активность
            if (session.getStatus() != STATUS.ACTIVE){
                throw new RuntimeException("Сессия не активна");
            }

            // Обновляем старый access
            String newAccessToken = userSessionService.refreshAccessToken(session.getRefreshToken()).getAccessToken();

            // Формирование ответа
            TokenResponse response = TokenResponse.newBuilder()
                    .setRefreshedToken(newAccessToken)
                    .build();

            // Отправка ответа клиенту
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e){
            log.error("Ошибка обновления access токена: {}", e.getMessage());
            responseObserver.onError(
                    Status.UNAUTHENTICATED
                            .withDescription("Ошибка обновления токена: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }


    //    @Override
//    public void refreshToken(TokenRequest request, StreamObserver<TokenResponse> responseObserver) {
//        String expiredToken = request.getExpiredToken();
//        log.debug("Принял запрос на обновление токена: {}", expiredToken);
//
//        try {
//            if (expiredToken != null && expiredToken.startsWith("Bearer ")) {
//                expiredToken = expiredToken.substring(7);
//            }
//
//            // Извлекаем имя пользователя
//            String username = jwtUtil.extractUsername(expiredToken);
//            User user = userService.findByUsername(username);
//
//            // Если пользователь заблокирован
//            if (user.getIsBlocked()) {
//                log.warn("Пользователь {} заблокирован, обновление невозможно", username);
//                responseObserver.onError(
//                        Status.PERMISSION_DENIED
//                                .withDescription("Пользователь заблокирован")
//                                .asRuntimeException()
//                );
//                return;
//            }
//
//            String refreshedToken = jwtUtil.generateToken(user);
//
//            // Формируем ответ
//            TokenResponse response = TokenResponse.newBuilder()
//                    .setRefreshedToken(refreshedToken)
//                    .build();
//
//            // И отправляем ответ клиенту
//            responseObserver.onNext(response);
//            responseObserver.onCompleted();
//
//        } catch (Exception e) {
//            log.error("Ошибка обновления токена", e);
//            responseObserver.onError(
//                    Status.UNAUTHENTICATED
//                            .withDescription("Недействительный токен")
//                            .withCause(e)
//                            .asRuntimeException()
//            );
//        }
//    }
}
