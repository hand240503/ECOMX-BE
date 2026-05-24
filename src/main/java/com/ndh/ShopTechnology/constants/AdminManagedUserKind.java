package com.ndh.ShopTechnology.constants;

/**
 * Phân nhánh quản trị user: nhân viên nội bộ (không CUSTOMER) vs chỉ role EMPLOYEE vs mọi user.
 */
public enum AdminManagedUserKind {
    /** Nội bộ: mọi role trừ CUSTOMER. */
    STAFF,
    /** Chỉ tài khoản role EMPLOYEE. */
    EMPLOYEE,
    /** Mọi tài khoản trong hệ thống kể cả CUSTOMER — dùng API {@code /admin/users}. */
    ANY
}
