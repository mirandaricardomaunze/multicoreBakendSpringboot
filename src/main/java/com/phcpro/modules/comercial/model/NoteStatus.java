package com.phcpro.modules.comercial.model;

/** Lifecycle states shared by Credit Notes and Debit Notes. */
public enum NoteStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    CANCELLED
}
