package ru.nand.registryservice.configs;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.nand.registryservice.entities.DTO.NotificationDTO;
import ru.nand.registryservice.entities.Notification;

@Configuration
public class ModelMapperConfig {
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // Маппинг сущности в DTO
        modelMapper.addMappings(new PropertyMap<Notification, NotificationDTO>() {
            @Override
            protected void configure() {
                // Маппинг поля user.username -> ownerUsername
                map(source.getUser().getUsername(), destination.getOwnerUsername());
                // Маппинг поля text -> message
                map(source.getText(), destination.getMessage());
            }
        });

        return modelMapper;
    }
}
