package com.ndh.ShopTechnology.constant;

public class MessageConstant {

    // JWT / Authentication messages
    public static final String ERROR_GET_USERNAME = "An error occurred during getting username from token.";
    public static final String TOKEN_EXPIRED_LOG = "The token is expired and not valid anymore.";
    public static final String TOKEN_EXPIRED_RESPONSE = "JWT expired.";
    public static final String AUTH_FAILED = "Authentication Failed. Username or Password not valid.";
    public static final String BEARER_MISSING = "Couldn't find bearer string, will ignore the header.";
    public static final String AUTH_SUCCESS_PREFIX = "Authenticated user %s, setting security context.";

    // User validation messages
    public static final String USERNAME_REQUIRED = "Username is required.";
    public static final String PHONE_REQUIRED = "Phone number is required.";
    public static final String PASSWORD_REQUIRED = "Password is required.";
    public static final String ROLE_NOT_FOUND = "User role not found.";
    public static final String USER_ALREADY_EXISTS = "User already exists.";
    public static final String VALIDATION_FAILED = "Validation failed.";

    // Authorization messages
    public static final String ACCESS_DENIED = "Access denied. You do not have permission to perform this action.";
    public static final String NO_PERMISSION_ACTION = "You do not have permission to perform this action.";

    // Not Found messages
    public static final String USER_NOT_FOUND_BY_ID = "User with id %s not found.";
    public static final String USER_NOT_FOUND = "User not found.";

    // Success messages
    public static final String USER_REGISTER_SUCCESS = "User registered successfully.";
    public static final String LOGIN_SUCCESS = "Login successful.";
    public static final String USER_INFO_RETRIEVED = "User information retrieved successfully.";
    public static final String USER_PROFILE_UPDATE_SUCCESS = "User profile updated successfully.";

    // Error messages
    public static final String USER_PROFILE_UPDATE_FAILED = "Failed to update user profile.";

    // Message category
    public static final String CATEGORY_CREATE_SUCCESS = "Category created successfully!";
    public static final String CATEGORY_LIST_SUCCESS = "Category list retrieved successfully!";
    public static final String CATEGORY_UPDATE_SUCCESS = "Category updated successfully!";
    public static final String CATEGORY_DELETE_SUCCESS = "Category deleted successfully!";
    public static final String CATEGORY_NOT_FOUND = "Category not found.";

    // Message address
    public static final String ADDRESS_CREATE_SUCCESS = "Address created successfully!";
    public static final String ADDRESS_UPDATE_SUCCESS = "Address updated successfully!";
    public static final String ADDRESS_DELETE_SUCCESS = "Address deleted successfully!";
    public static final String ADDRESS_NOT_FOUND = "Address not found.";

    // Message product
    public static final String PRODUCT_CREATE_SUCCESS = "Product created successfully!";
    public static final String PRODUCT_UPDATE_SUCCESS = "Product updated successfully!";
    public static final String PRODUCT_DELETE_SUCCESS = "Product deleted successfully!";
    public static final String PRODUCT_NOT_FOUND = "Product not found.";
    public static final String PRODUCT_LIST_SUCCESS = "Products retrieved successfully!";
}
