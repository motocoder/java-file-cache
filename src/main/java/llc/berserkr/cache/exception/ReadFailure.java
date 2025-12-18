package llc.berserkr.cache.exception;

import java.io.IOException;

public class ReadFailure extends Exception {
    public ReadFailure(String message) {
        super(message);
    }

    public ReadFailure(String message, IOException e) {
        super(message, e);
    }
}
