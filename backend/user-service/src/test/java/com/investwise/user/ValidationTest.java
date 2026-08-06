package com.investwise.user;

import com.investwise.user.dto.Requests;
import com.investwise.user.model.Enums;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validation now uses two custom validators instead of five; these tests confirm
 * the plain {@code @Pattern} and {@code @AssertTrue} replacements behave identically.
 */
@DisplayName("Bean validation")
class ValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void open() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    private Requests.Register registration(String password, String confirm, String phone,
                                           String pan, LocalDate dob) {
        return new Requests.Register("Rahul", "Sharma", "rahul.sharma@example.com",
                password, confirm, phone, dob, Enums.Gender.MALE, pan, null, "Engineer");
    }

    private Requests.Register valid() {
        return registration("Str0ng@Pass1", "Str0ng@Pass1", "9876543210",
                "ABCPE1234F", LocalDate.of(1995, 9, 23));
    }

    private boolean fieldOk(Object payload, String field) {
        Set<ConstraintViolation<Object>> violations = validator.validate(payload);
        return violations.stream().noneMatch(v -> v.getPropertyPath().toString().equals(field));
    }

    @Test
    @DisplayName("accepts a fully valid registration payload")
    void acceptsValid() {
        assertThat(validator.validate(valid())).isEmpty();
    }

    @ParameterizedTest(name = "rejects weak password \"{0}\"")
    @ValueSource(strings = {
            "short1!", "alllowercase1!", "ALLUPPERCASE1!", "NoDigitsHere!",
            "NoSpecial123", "Password@123", "With Space1!"
    })
    void rejectsWeakPasswords(String password) {
        assertThat(fieldOk(registration(password, password, "9876543210",
                "ABCPE1234F", LocalDate.of(1995, 9, 23)), "password"))
                .as("password %s", password).isFalse();
    }

    @Test
    @DisplayName("flags a mismatched confirmation via the cross-field assertion")
    void rejectsMismatch() {
        var request = registration("Str0ng@Pass1", "Different@1", "9876543210",
                "ABCPE1234F", LocalDate.of(1995, 9, 23));
        assertThat(fieldOk(request, "passwordConfirmed")).isFalse();
    }

    @ParameterizedTest(name = "phone \"{0}\" valid = {1}")
    @CsvSource({
            "9876543210, true", "6123456789, true", "5123456789, false",
            "98765432, false", "98765432101, false", "98765abcde, false"
    })
    void validatesPhone(String phone, boolean expected) {
        assertThat(fieldOk(registration("Str0ng@Pass1", "Str0ng@Pass1", phone,
                "ABCPE1234F", LocalDate.of(1995, 9, 23)), "phone")).isEqualTo(expected);
    }

    @ParameterizedTest(name = "PAN \"{0}\" valid = {1}")
    @CsvSource({
            "ABCPE1234F, true",
            "'',         true",
            "ABCP1234FG, false",
            "ABCPE12345, false"
    })
    void validatesPan(String pan, boolean expected) {
        assertThat(fieldOk(registration("Str0ng@Pass1", "Str0ng@Pass1", "9876543210",
                pan, LocalDate.of(1995, 9, 23)), "panNumber")).isEqualTo(expected);
    }

    @Test
    @DisplayName("refuses a date of birth under 18 via @AssertTrue rather than a custom validator")
    void rejectsMinors() {
        var request = registration("Str0ng@Pass1", "Str0ng@Pass1", "9876543210",
                "ABCPE1234F", LocalDate.now().minusYears(15));
        assertThat(fieldOk(request, "adult")).isFalse();
    }

    @ParameterizedTest(name = "email \"{0}\" valid = {1}")
    @CsvSource({
            "rahul@example.com,         true",
            "rahul.sharma+tag@ex.co.in, true",
            "not-an-email,              false",
            "double@@example.com,       false"
    })
    void validatesEmail(String email, boolean expected) {
        var request = new Requests.Register("Rahul", "Sharma", email, "Str0ng@Pass1", "Str0ng@Pass1",
                "9876543210", LocalDate.of(1995, 9, 23), Enums.Gender.MALE, "ABCPE1234F", null, null);
        assertThat(fieldOk(request, "email")).isEqualTo(expected);
    }

    @Test
    @DisplayName("change-password refuses a new password equal to the current one")
    void rejectsUnchangedPassword() {
        var request = new Requests.ChangePassword("Str0ng@Pass1", "Str0ng@Pass1", "Str0ng@Pass1");
        assertThat(fieldOk(request, "different")).isFalse();
    }
}
