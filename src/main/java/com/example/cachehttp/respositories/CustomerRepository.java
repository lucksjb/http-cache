package com.example.cachehttp.respositories;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.example.cachehttp.models.Customer;

import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class CustomerRepository {
    
    public List<Customer> listAll() {
        
        var name  = "luck";
        int thisSecond = LocalDateTime.now().getSecond();
        
        /* Just to simulate changes on record every ten seconds */
        if(thisSecond < 10) {
            name = name + " 1" ;

        } else if(thisSecond >= 10 && thisSecond < 20) {
            name = name + " 2" ;

        } else if(thisSecond >= 20 && thisSecond < 30) {
            name = name + " 3" ;

        } else if(thisSecond >= 30 && thisSecond < 40) {
            name = name + " 4" ;

        } else if(thisSecond >= 40 && thisSecond < 50) {
            name = name + " 5" ;

        } else if(thisSecond >= 50 && thisSecond <= 60) {
            name = name + " 6" ;

        }


        Customer luck = new Customer(1L,name);
        log.info("Repository made "+LocalDateTime.now());

        return Arrays.asList(luck, new Customer(2L,"Marta"), new Customer(3L,"Yuri"), new Customer(4L,"Felipe"));
    }
}
