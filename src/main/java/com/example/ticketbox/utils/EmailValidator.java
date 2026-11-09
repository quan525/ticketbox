package com.example.ticketbox.utils;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class EmailValidator {

    private static final String EMAIL_REGEX =
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

    private final Pattern pattern = Pattern.compile(EMAIL_REGEX);

    public boolean isValid(String email) {
        if (email == null) {
            return false;
        }
        return pattern.matcher(email).matches();
    }
}
