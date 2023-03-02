package com.example.spring_batch2.read.service;

import com.example.spring_batch2.entity.User;
import com.example.spring_batch2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExternalService<T> {

    private final UserRepository userRepository;

    private long id = 100;

    public T cntRead(){
        Optional<User> user = userRepository.findById(id--);
        if(user.isPresent()) return (T) user.get();
        else return null;
    }
}
