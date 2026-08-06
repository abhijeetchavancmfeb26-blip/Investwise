package com.investwise.user.dto;

import com.investwise.user.model.Enums;
import com.investwise.user.validation.Patterns;
import com.investwise.user.validation.StrongPassword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

/**
 * Every inbound payload, as records in one file.
 * <p>
 * Records give immutability, equals/hashCode and a compact constructor for free,
 * which is what the original achieved with Lombok's {@code @Getter @Setter
 * @Builder @NoArgsConstructor @AllArgsConstructor} on ten separate classes.
 */
public final class Requests {

    private Requests() { }

    public record Register(
            @NotBlank @Size(min = 2, max = 50)
            @Pattern(regexp = Patterns.NAME, message = "First name may contain only letters, spaces, apostrophes and hyphens")
            String firstName,

            @NotBlank @Size(min = 2, max = 50)
            @Pattern(regexp = Patterns.NAME, message = "Last name may contain only letters, spaces, apostrophes and hyphens")
            String lastName,

            @NotBlank @Email(message = "Please provide a valid email address") @Size(max = 120)
            String email,

            @NotBlank @StrongPassword String password,
            @NotBlank(message = "Please confirm your password") String confirmPassword,

            @NotBlank @Pattern(regexp = Patterns.PHONE, message = "Enter a valid 10 digit mobile number starting 6-9")
            String phone,

            @Past(message = "Date of birth must be in the past") LocalDate dateOfBirth,
            Enums.Gender gender,

            @Pattern(regexp = "^$|" + Patterns.PAN, message = "PAN must look like ABCPE1234F")
            String panNumber,

            @DecimalMin(value = "0.0", message = "Annual income cannot be negative") BigDecimal annualIncome,
            @Size(max = 100) String occupation) {

        /** Cross-field checks as compact assertions, replacing a class-level validator. */
        @AssertTrue(message = "Passwords do not match")
        public boolean isPasswordConfirmed() {
            return password != null && password.equals(confirmPassword);
        }

        @AssertTrue(message = "You must be at least 18 years old")
        public boolean isAdult() {
            return dateOfBirth == null || Period.between(dateOfBirth, LocalDate.now()).getYears() >= 18;
        }

        /** Normalised so lookups and uniqueness checks behave predictably. */
        public String normalisedEmail() {
            return email == null ? null : email.trim().toLowerCase();
        }

        public String normalisedPan() {
            return panNumber == null || panNumber.isBlank() ? null : panNumber.trim().toUpperCase();
        }
    }

    public record Login(
            @NotBlank @Email(message = "Please provide a valid email address") String email,
            @NotBlank @Size(max = 64) String password,
            boolean rememberMe) {

        public String normalisedEmail() {
            return email == null ? null : email.trim().toLowerCase();
        }
    }

    public record ForgotPassword(
            @NotBlank @Email(message = "Please provide a valid email address") String email) { }

    public record ResetPassword(
            @NotBlank(message = "Reset token is required") String token,
            @NotBlank @StrongPassword String newPassword,
            @NotBlank(message = "Please confirm your new password") String confirmPassword) {

        @AssertTrue(message = "Passwords do not match")
        public boolean isPasswordConfirmed() {
            return newPassword != null && newPassword.equals(confirmPassword);
        }
    }

    public record ChangePassword(
            @NotBlank(message = "Your current password is required") String currentPassword,
            @NotBlank @StrongPassword String newPassword,
            @NotBlank(message = "Please confirm your new password") String confirmPassword) {

        @AssertTrue(message = "Passwords do not match")
        public boolean isPasswordConfirmed() {
            return newPassword != null && newPassword.equals(confirmPassword);
        }

        @AssertTrue(message = "The new password must differ from your current one")
        public boolean isDifferent() {
            return newPassword == null || !newPassword.equals(currentPassword);
        }
    }

    public record UpdateProfile(
            @NotBlank @Size(min = 2, max = 50) @Pattern(regexp = Patterns.NAME) String firstName,
            @NotBlank @Size(min = 2, max = 50) @Pattern(regexp = Patterns.NAME) String lastName,
            @NotBlank @Pattern(regexp = Patterns.PHONE, message = "Enter a valid 10 digit mobile number") String phone,
            @Past LocalDate dateOfBirth,
            Enums.Gender gender,
            @Pattern(regexp = "^$|" + Patterns.PAN, message = "PAN must look like ABCPE1234F") String panNumber,
            @DecimalMin("0.0") BigDecimal annualIncome,
            @Size(max = 100) String occupation,
            @Size(max = 300) String address,
            @Size(max = 60) String city,
            @Size(max = 60) String state,
            @Pattern(regexp = "^$|" + Patterns.PINCODE, message = "Enter a valid 6 digit PIN code") String pincode) {

        @AssertTrue(message = "You must be at least 18 years old")
        public boolean isAdult() {
            return dateOfBirth == null || Period.between(dateOfBirth, LocalDate.now()).getYears() >= 18;
        }

        public String normalisedPan() {
            return panNumber == null || panNumber.isBlank() ? null : panNumber.trim().toUpperCase();
        }
    }

    public record Contact(
            @NotBlank @Size(min = 2, max = 80) String name,
            @NotBlank @Email(message = "Please provide a valid email address") String email,
            @Pattern(regexp = Patterns.OPTIONAL_PHONE, message = "Enter a valid 10 digit mobile number") String phone,
            @NotBlank @Size(min = 3, max = 150) String subject,
            @NotBlank @Size(min = 10, max = 2000, message = "Message must be between 10 and 2000 characters") String message) { }

    public record AdminReply(
            @NotBlank @Size(min = 5, max = 2000) String reply,
            @NotNull Enums.ContactStatus status) { }

    public record UpdateStatus(
            @NotNull Enums.Status status,
            @Size(max = 300) String reason) { }
}
