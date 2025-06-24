package com.example.gotatva.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AutoTester {
    private LocalDateTime timestamp;
    private String result;
}