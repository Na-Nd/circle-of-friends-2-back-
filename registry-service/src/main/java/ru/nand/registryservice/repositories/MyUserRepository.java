package ru.nand.registryservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nand.registryservice.entities.User;

@Repository
public interface MyUserRepository extends JpaRepository<User, Integer> {
    User findByUsername(String username);
    User findByEmail(String email);
    void deleteByUsername(String username);
}


