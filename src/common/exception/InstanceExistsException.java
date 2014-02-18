package common.exception;

public class InstanceExistsException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InstanceExistsException(Class<?> c){
		super("An instance of " + c.getName() + " already exists.");
	}
}
