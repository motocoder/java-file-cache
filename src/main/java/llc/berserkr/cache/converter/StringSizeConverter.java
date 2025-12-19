package llc.berserkr.cache.converter;


import llc.berserkr.cache.exception.ResourceException;

public class StringSizeConverter implements Converter<String, Integer> {

    @Override
    public Integer convert(String old) throws ResourceException {
        
        if(old == null) {
            return 0;
        }
        
        return 8 * (int) ((((old.length()) * 2) + 45) / 8);
    }

    @Override
    public String restore(Integer newVal) throws ResourceException {
        return null;
    }

}
