// Package for simple data classes ("models") that carry information around the app.
package com.tala.model;

/** An account shown in a combo box (e.g. on the Journal Entries form). */
public class AccountOption {
    // The three fields are "public final": they are set once in the constructor and can never change.
    // The database ID of the account (used when saving the journal entry).
    public final long id;
    // The account code, e.g. "1010".
    public final String code;
    // The account name, e.g. "Cash on Hand".
    public final String name;

    // Constructor: creates one AccountOption and stores the values passed in.
    public AccountOption(long id, String code, String name) {
        // "this.id" is the field; "id" is the parameter with the same name.
        this.id = id;
        this.code = code;
        this.name = name;
    }

    // A combo box (dropdown) displays each item by calling toString().
    // So the dropdown shows e.g. "1010 - Cash on Hand" while we still keep the id hidden inside.
    @Override
    public String toString() {
        return code + " - " + name;
    }
}
