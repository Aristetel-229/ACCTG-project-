// This class lives in the model package (simple data holders).
package com.tala.model;

/** An account shown in a combo box (e.g. on the Journal Entries form). */
public class AccountOption {
    // Database primary key of the account (used when saving journal lines).
    public final long id;
    // Account code from the Chart of Accounts, e.g. "101".
    public final String code;
    // Account name, e.g. "Cash".
    public final String name;

    // Constructor: stores the three values when the object is created.
    public AccountOption(long id, String code, String name) {
        // Save the id.
        this.id = id;
        // Save the code.
        this.code = code;
        // Save the name.
        this.name = name;
    }

    // A combo box shows whatever toString() returns, so we make it look like "code - name".
    @Override
    public String toString() {
        // Example result: "101 - Cash".
        return code + " - " + name;
    }
}
