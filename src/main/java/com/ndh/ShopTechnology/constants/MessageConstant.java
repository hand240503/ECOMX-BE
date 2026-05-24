package com.ndh.ShopTechnology.constants;

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

    /** Dùng khi chưa gửi hoặc chưa chấp nhận token (bảo vệ trừ vùng permitAll). */
    public static final String AUTHENTICATION_REQUIRED =
            "Authentication required. Please provide a valid Bearer access token in the Authorization header.";

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

    /** PC (product price change): đã vào thời gian áp dụng — chỉ dừng (enabled=false), không xóa bản ghi. */
    public static final String PRICE_CHANGE_CANNOT_DELETE_AFTER_START =
            "Đợt giá đã đến hoặc đang trong thời gian áp dụng; không thể xóa. Hãy tắt chương trình (enabled=false) để dừng.";

    /** PWP đang bật — chỉ dừng bằng cập nhật, không xóa. */
    public static final String PWP_CANNOT_DELETE_WHILE_ACTIVE =
            "Chương trình purchase-with-purchase đang bật; không thể xóa. Hãy tắt (enabled=false) để dừng.";

    /** Mix-and-match (volume tier): không gỡ bậc đang bật khỏi cấu hình; phải gửi enabled=false cùng minQuantity trước. */
    public static final String VOLUME_TIER_CANNOT_DROP_WHILE_ACTIVE =
            "Không thể bỏ bậc giá theo số lượng đang bật. Hãy gửi lại cùng minQuantity với enabled=false để dừng, sau đó có thể cập nhật danh sách.";
}
