package llc.berserkr.cache.exception;

import java.util.ArrayList;
import java.util.Collection;

public class GetAllResourceException extends ResourceException {

    private static final long serialVersionUID = -318030003702635934L;
    
    private final Collection<KeyThrownException> throwns;

    public GetAllResourceException(Collection<KeyThrownException> throwns) {
        this.throwns = throwns;
    }

    public Collection<KeyThrownException> getThrowns() {
        return new ArrayList<KeyThrownException>(throwns);
    }
   
}
