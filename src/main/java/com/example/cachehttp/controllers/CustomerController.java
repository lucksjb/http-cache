package com.example.cachehttp.controllers;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.example.cachehttp.models.Customer;
import com.example.cachehttp.services.CustomerService;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class CustomerController {
    private final CustomerService customerService;
    
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/customer/")
    public ResponseEntity<List<Customer>> listAll() {
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(5, TimeUnit.SECONDS))
            .body(customerService.listAll());
    }
}
