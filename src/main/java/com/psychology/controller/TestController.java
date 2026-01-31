package com.psychology.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @GetMapping("/simple")
    public String simpleTest() {
        return "OK";
    }

    @GetMapping("/error-test")
    public String errorTest() {
        throw new RuntimeException("Test error");
    }
}