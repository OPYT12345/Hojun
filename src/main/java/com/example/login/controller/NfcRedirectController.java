package com.example.login.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class NfcRedirectController {

    @GetMapping("/nfc/{lectureId}")
    public String redirect(@PathVariable Long lectureId) {
        return "redirect:/student-nfc.html?lectureId=" + lectureId;
    }
}
