package com.fredfmelo.paymentservice.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fredfmelo.paymentservice.api.TestApi;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class TestController implements TestApi {

    @Override
    public ResponseEntity<Void> test() {
        log.info("test sucess");
        return ResponseEntity.ok().build();
    }
}