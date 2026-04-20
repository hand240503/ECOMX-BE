package com.ndh.ShopTechnology.enums;

public enum UserState {
    NEW,        // chưa login (anonymous)
    COLD,       // đã có account nhưng 0 events
    ACTIVE      // đã có account + có events
}
