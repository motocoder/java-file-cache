package llc.berserkr.cache.exception;

public class LinearStreamException extends Exception {

	private static final long serialVersionUID = 5829482387553994166L;

	public LinearStreamException(Exception e) {
		super(e);
	}

	public LinearStreamException(String e) {
		super(e);
	}
	
}
