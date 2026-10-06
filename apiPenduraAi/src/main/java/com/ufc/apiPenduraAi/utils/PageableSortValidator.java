package com.ufc.apiPenduraAi.utils;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public class PageableSortValidator {

    public static void validate(Pageable pageable, Set<String> allowedFields){

        for(Sort.Order order : pageable.getSort()){
            String property = order.getProperty();

            if(!allowedFields.contains(property)){
                throw new IllegalArgumentException("Campo de ordenação não permitido: " + property);
            }
        }
    }
}
