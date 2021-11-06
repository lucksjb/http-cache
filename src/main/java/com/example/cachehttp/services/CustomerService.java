package com.example.cachehttp.services;

import java.util.List;

import com.example.cachehttp.models.Customer;
import com.example.cachehttp.respositories.CustomerRepository;

import org.springframework.stereotype.Service;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> listAll() {
        return customerRepository.listAll();
    }
}
