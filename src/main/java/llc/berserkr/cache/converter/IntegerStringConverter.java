package llc.berserkr.cache.converter;


import llc.berserkr.cache.exception.ResourceException;

public class IntegerStringConverter implements Converter<Integer, String> {

	@Override
	public String convert(Integer old) throws ResourceException {
		return old.toString();
	}

	@Override
	public Integer restore(String newVal) throws ResourceException {
		return Integer.parseInt(newVal);
	}

}
