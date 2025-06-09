package org.example.controllers;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")  // Base path
public class ApiController {
    @GetMapping("/indexed-words")
    public String sayHello() {
        return "Hello from Spring!";
    }

    @PostMapping("/analyse")
    public String echoMessage(@RequestBody String message) {
        return "You said: " + message;
    }
}