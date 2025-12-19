package llc.berserkr.cache.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


public final class StringUtilities {

    public static String join(Collection<?> values, String joinString) {
        StringBuilder build = new StringBuilder();        
        
        for(Object value : values ) {
            build.append(value.toString()).append(joinString);
        }
        
        return values.size() == 0 ? "" : build.substring(0, build.lastIndexOf(joinString) );
    }
    
    public static String toHex(final byte [] bytes) {
        
        StringBuilder sb = new StringBuilder();
        
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        
        return sb.toString();
        
    }
    
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    
    public static String join(Object [] values, String joinString) {
        StringBuilder build = new StringBuilder();        
        for(Object value : values ) {
            build.append(String.valueOf(value)).append(joinString);
        }
        
        return build.substring(0, build.lastIndexOf(joinString) );
    }

	public static String formatDuration(long millis) {
		
		long sec = millis / 1000;
		long min = sec /60;
		long hour = min / 60;
		
		return hour + "hrs " + (min % 60) + "mins " + (sec %60) + "secs";
		
	}
	
    public static Set<String> getAllSubstrings(final String original) {

        final Set<String> returnVals = new HashSet<String>();

        final int length = original.length();

        for (int c = 0; c < length; c++) {

            for (int i = 1; i <= length - c; i++) {
                returnVals.add(original.substring(c, c + i));
            }

        }

        return returnVals;

    }
	
	public static boolean isEmpty(String check) {

        final boolean returnVal;

        if (check == null) {
            returnVal = true;
        }
        else if (check.trim().length() == 0) {
            returnVal = true;
        }
        else {
            returnVal = false;
        }

        return returnVal;

    }


	public static String formatDayDuration(long millis) {
		long sec = millis / 1000;
		long min = sec /60;
		long hour = min / 60;
		long days = hour/ 24;
		
		return days + "days " + (hour % 24) + "hrs " + (min % 60) + "mins " + (sec %60) + "secs";
		
	}
	
	public static String formatSQLSafeString(String filteringStr) {
		String[] toxicCharacters = {"\'", ";", "\\"};
		String[] safeCharacters = {"\"", ".", "\\\\"};
		for (int i=0; i < toxicCharacters.length; i++) {
			filteringStr = filteringStr.replace(toxicCharacters[i], safeCharacters[i]);
		}
		return filteringStr.trim();
	}
	
	public static String formatMoney(long cents) {
	    StringBuilder money = new StringBuilder(String.format("%.2f",(cents) /100f));
	    
	    final int decimal = money.indexOf(".");

	    for(int skipped = 0, i = decimal; i > 0; i--, skipped++) {
	        if(skipped == 3) {
	            skipped = 0;
	            money.insert(i, ',');
	        }
	    }
	    
	    money.insert(0, "$");
	    
	    return money.toString();
	}
	
	public static String generateSpaces(int size) {
		StringBuilder returnVal = new StringBuilder();
		
		for(int i = 0; i < size; i++) {
			returnVal.append(" ");
		}
		
		return returnVal.toString();
	}

    public static String limitString(
        final String body,
        final String string,
        final int size
    ) {
        
        if(body.length() < size) {
            return body;
        }
        else {
            return body.substring(0, size - string.length()) + string;
        }
        
    }

    public static String repeat(String valueOrig, int random) {
        
        final StringBuilder builder = new StringBuilder();
        
        for(int i = 0; i < random; i++) {
            builder.append(valueOrig);
        }
        
        return builder.toString();
        
    }

    public static boolean containsOnlyDigits(String str) {
        return str.chars().allMatch(Character::isDigit);
    }

}
