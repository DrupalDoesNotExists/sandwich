package ru.dldnex.bundle.exception;

import org.jetbrains.annotations.NotNull;

/**
 * Wrapper for exceptions that occur while stacking sandwich
 */
public class SandwichRuntimeException extends RuntimeException {

    public SandwichRuntimeException() {}
    public SandwichRuntimeException(@NotNull String message) { super(message); }
    public SandwichRuntimeException(@NotNull String message, @NotNull Throwable cause) { super(message, cause); }
    public SandwichRuntimeException(@NotNull Throwable cause) { super(cause); }

}
