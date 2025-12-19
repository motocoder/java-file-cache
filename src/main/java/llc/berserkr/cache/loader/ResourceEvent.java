package llc.berserkr.cache.loader;

/**
 *  
 *  ResourceEvent takes three parameters during construction: A Value,
 *  a Throwable, and an int. The class contains three getter methods, one for
 *  each of the variables.
 *      
 */

/**
 * 
 * 
 *
 * @param <Value>
 */

public class ResourceEvent<Value> {
    
	public static final int NEW_LOADED = 0;
	public static final int CACHED = 1;
	public static final int UNKNOWN = 2;
	
    private final Value val;
    private final Throwable throwable;
	private final int valueType;

	/**
	 * 
	 * @param val
	 * @param throwable
	 * @param valueType
	 */
	
    public ResourceEvent(
        final Value val,
        final Throwable throwable,
        final int valueType
    ) {
        
    	this.valueType = valueType;
        this.val = val;
        this.throwable = throwable;
        
    }
    
    /**
     * 
     * @return int
     */
    
    public int getValueType() {
		return valueType;
	}
    
    /**
     * 
     * @return Value
     */
    
	public Value getVal() {
        return val;
    }

	/**
	 * 
	 * @return Throwable
	 */
	
    public Throwable getThrowable() {
        return throwable;
    }

}
