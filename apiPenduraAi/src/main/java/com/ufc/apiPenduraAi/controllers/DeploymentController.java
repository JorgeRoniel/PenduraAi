package com.ufc.apiPenduraAi.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class DeploymentController {

    @GetMapping({"/login", "/register"})
    public String frontendRoute() {
        return "forward:/index.html";
    }

    @GetMapping("/health")
    @ResponseBody
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
