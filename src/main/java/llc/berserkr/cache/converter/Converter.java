package llc.berserkr.cache.converter;


import llc.berserkr.cache.exception.ResourceException;

public interface Converter<Old, New> {

    /**
     * 
     * @param old
     * @return
     */
    New convert(Old old) throws ResourceException;
    
    /**
     * 
     * @param newVal
     * @return
     */
    Old restore(New newVal) throws ResourceException;
    
}
