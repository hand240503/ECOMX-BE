package com.ndh.ShopTechnology.constants;

/**
 * Phân nhánh quản trị user: nhân viên nội bộ (không CUSTOMER) vs chỉ role EMPLOYEE.
 */
public enum AdminManagedUserKind {
    /** Nội bộ: mọi role trừ CUSTOMER. */
    STAFF,
    /** Chỉ tài khoản role EMPLOYEE. */
    EMPLOYEE
}
