package com.example.spring_batch2.jms;

import lombok.Data;

import java.io.Serializable;

@Data
public class Account implements Serializable {
    private String name;
    private String age;
}
