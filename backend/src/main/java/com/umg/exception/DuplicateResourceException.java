package com.umg.exception;

/**
 * Thrown when an attempt is made to create a resource that already exists.
 * Results in an HTTP 409 response.
 */
public class DuplicateResourceException extends RuntimeException {

    private final String resourceType;
    private final String identifier;

    public DuplicateResourceException(String resourceType, String identifier) {
        super(String.format("%s already exists with identifier: %s", resourceType, identifier));
        this.resourceType = resourceType;
        this.identifier = identifier;
    }

    public DuplicateResourceException(String message) {
        super(message);
        this.resourceType = null;
        this.identifier = null;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getIdentifier() {
        return identifier;
    }
}
