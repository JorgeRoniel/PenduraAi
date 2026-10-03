package com.ufc.apiPenduraAi.utils;

import java.util.Locale;

public class EmailNormalizer {

    public static String normalized(String email){
        if(email == null) return null;

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
