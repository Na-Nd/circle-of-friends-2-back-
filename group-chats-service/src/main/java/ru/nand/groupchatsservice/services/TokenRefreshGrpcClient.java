package ru.nand.groupchatsservice.services;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.nand.registryservice.grpc.TokenServiceGrpc;
import ru.nand.registryservice.grpc.TokenServiceProto;

@Slf4j
@Service
public class TokenRefreshGrpcClient {

    @GrpcClient("tokenRefreshRegistryService")
    private TokenServiceGrpc.TokenServiceBlockingStub tokenServiceBlockingStub;

    public String refreshToken(String expiredToken) throws RuntimeException{
        try{
            TokenServiceProto.TokenRequest request = TokenServiceProto.TokenRequest.newBuilder()
                    .setExpiredToken("Bearer " + expiredToken)
                    .build();

            TokenServiceProto.TokenResponse response = tokenServiceBlockingStub.refreshToken(request);
            return response.getRefreshedToken();
        } catch (StatusRuntimeException e){
            log.error("Ошибка при обновлении токена по gRPC: {}", e.getStatus());
            throw new RuntimeException("Ошибка при обновлении токена: " + e.getStatus());
        }
    }
}