package com.investwise.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The password policy.
 * <p>
 * The annotation and its validator live in one file. Everything the original
 * expressed with separate {@code @ValidPan}, {@code @MinimumAge}, {@code @NoHtml}
 * and {@code @PasswordsMatch} classes is now handled by plain {@code @Pattern},
 * {@code @Past} and one cross-field check — this is the only rule complex enough
 * to justify a custom validator, because it must explain *which* rule failed.
 */
@Constraint(validatedBy = StrongPassword.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "Password does not meet the security policy";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<StrongPassword, String> {

        private static final Set<String> COMMON = Set.of(
                "password", "password1", "password@123", "admin@123", "welcome@123",
                "qwerty123", "12345678", "investwise", "invest@123", "india@123");

        @Override
        public boolean isValid(String password, ConstraintValidatorContext context) {
            if (password == null || password.isBlank()) {
                return false; // @NotBlank reports the empty case
            }

            List<String> missing = new ArrayList<>();
            if (password.length() < 8 || password.length() > 64) missing.add("8 to 64 characters");
            if (password.chars().noneMatch(Character::isUpperCase)) missing.add("an uppercase letter");
            if (password.chars().noneMatch(Character::isLowerCase)) missing.add("a lowercase letter");
            if (password.chars().noneMatch(Character::isDigit)) missing.add("a digit");
            if (password.chars().noneMatch(c -> "@$!%*?&#^()-_=+".indexOf(c) >= 0)) {
                missing.add("a special character");
            }
            if (password.chars().anyMatch(Character::isWhitespace)) missing.add("no spaces");
            if (COMMON.contains(password.toLowerCase())) missing.add("something less common");

            if (missing.isEmpty()) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Password needs " + String.join(", ", missing))
                    .addConstraintViolation();
            return false;
        }
    }
}
