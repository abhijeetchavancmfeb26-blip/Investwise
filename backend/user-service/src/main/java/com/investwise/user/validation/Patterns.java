package com.investwise.user.validation;

/**
 * Regexes shared by the DTOs, mirrored exactly by the frontend Zod schemas.
 * <p>
 * Using {@code @Pattern} with these constants removed three custom validator
 * classes (PAN, phone, name) that added nothing a regex could not express.
 */
public final class Patterns {

    private Patterns() { }

    public static final String NAME = "^[A-Za-z][A-Za-z .'-]{1,49}$";
    public static final String PHONE = "^[6-9]\\d{9}$";
    /** Fourth character is the PAN holder type; P is an individual. */
    public static final String PAN = "^[A-Z]{5}[0-9]{4}[A-Z]$";
    public static final String PINCODE = "^[1-9][0-9]{5}$";
    public static final String OPTIONAL_PHONE = "^$|" + "[6-9]\\d{9}";
}
