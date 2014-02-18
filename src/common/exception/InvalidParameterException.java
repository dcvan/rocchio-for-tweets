package common.exception;

public class InvalidParameterException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InvalidParameterException(String params){
		super("Parameters " + params + " are invalid.");
	}
}
