package llc.berserkr.cache.provider;

import llc.berserkr.cache.exception.ResourceException;

public class SequentialIDProvider implements ResourceProvider<Long> {

	private long id;
	
	public SequentialIDProvider() {
	    
	}
	
	public SequentialIDProvider(final long starting) {
	    this.id = starting;
	}
	
	@Override
	public boolean exists() throws ResourceException {
		return true;
	}

	@Override
	public Long provide() throws ResourceException {
		return id++;
	}

}
