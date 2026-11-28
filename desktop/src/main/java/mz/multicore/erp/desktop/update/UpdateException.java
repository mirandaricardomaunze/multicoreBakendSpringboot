package mz.multicore.erp.desktop.update;

/** A refused or failed update; the message is shown to the operator as is (PT-MZ). */
public class UpdateException extends RuntimeException {

    public UpdateException(String message) {
        super(message);
    }

    public UpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
