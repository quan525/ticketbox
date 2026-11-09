package com.example.ticketbox.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OneOfNotNullValidator
        implements ConstraintValidator<OneOfNotNull, Object> {

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        try {
            String email = (String) obj.getClass()
                    .getDeclaredField("email")
                    .get(obj);

            String username = (String) obj.getClass()
                    .getDeclaredField("username")
                    .get(obj);

            return (email != null && !email.isBlank())
                    || (username != null && !username.isBlank());

        } catch (Exception e) {
            return false;
        }
    }
}
