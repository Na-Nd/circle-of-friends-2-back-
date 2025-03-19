package ru.nand.registryservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nand.registryservice.entities.ENUMS.ROLE;
import ru.nand.registryservice.entities.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    void deleteByUsername(String username);
    Optional<User> findByUsername(String username);
    Optional<User> findById(Integer id);
    List<User> findByRegistrationDateBetween(LocalDateTime start, LocalDateTime end);
    List<User> findByRole(ROLE role);
}