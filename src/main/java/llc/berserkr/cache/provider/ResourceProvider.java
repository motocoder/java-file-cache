package llc.berserkr.cache.provider;


import llc.berserkr.cache.exception.ResourceException;

public interface ResourceProvider<TValue> {
	
	boolean exists() throws ResourceException;
	TValue provide() throws ResourceException; 
	
}
