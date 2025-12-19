package llc.berserkr.cache.converter;


import llc.berserkr.cache.exception.ResourceException;

public class LongStringConverter implements Converter<Long, String> {

	@Override
	public String convert(Long old) throws ResourceException {
		return old.toString();
	}

	@Override
	public Long restore(String newVal) throws ResourceException {
		try {
			return Long.parseLong(newVal);
		}
		catch (NumberFormatException nfe) {
			throw new ResourceException("invalid number " + newVal);
		}
	}

}
