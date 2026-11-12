package com.example.ticketbox.enums;

public enum UserPermission {
    // Legacy stored permission, expanded into module views when reading old accounts.
    VIEW_OPERATIONS,
    VIEW_MOVIES, MANAGE_MOVIES,
    VIEW_SHOWS, MANAGE_SHOWS,
    VIEW_ROOMS, MANAGE_ROOMS,
    VIEW_BOOKINGS, MANAGE_BOOKINGS,
    VIEW_PAYMENTS, MANAGE_PAYMENTS,
    VIEW_CUSTOMERS;

    public UserPermission requiredView() {
        return name().startsWith("MANAGE_") ? valueOf(name().replace("MANAGE_", "VIEW_")) : this;
    }
}
