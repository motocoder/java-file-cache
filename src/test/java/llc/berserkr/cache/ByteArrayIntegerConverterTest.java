package llc.berserkr.cache;

import llc.berserkr.cache.converter.ByteArrayIntegerConverter;
import llc.berserkr.cache.exception.ResourceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ByteArrayIntegerConverterTest {
	
	@Test
	public void testConverter() {
		
		final ByteArrayIntegerConverter converter = new ByteArrayIntegerConverter();
		
		for(int number = -100000; number < 1000000; number ++) {
			
			
			try {
				
				byte[] bytes = converter.restore(number);
			
			
				int converted = converter.convert(bytes);
				
				assertEquals(number, converted);
				
			}
			catch (ResourceException e) {
				e.printStackTrace();
			}
			
			
		}
		
	}

}
