/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.model;

public enum DuplicateDecisionType {

    WORKSPACE("WORKSPACE"), WORKFLOW("WORKFLOW"), ADMIN("ADMIN");

    private String text;

    DuplicateDecisionType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }

    public static DuplicateDecisionType fromString(String text) {
        if (text == null) {
            return null;
        }
        for (DuplicateDecisionType b : DuplicateDecisionType.values()) {
            if (b.text.equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new IllegalArgumentException("No Decision enum with type " + text + " found");
    }
}