package com.example.ticketbox.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = OneOfNotNullValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OneOfNotNull {

    String message() default "Either email or username must be provided";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
