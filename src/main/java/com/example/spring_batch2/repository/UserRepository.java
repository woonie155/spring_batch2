package com.example.spring_batch2.repository;

import com.example.spring_batch2.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
