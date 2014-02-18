package common.exception;

public class FileExistsException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FileExistsException(String fname){
		super(fname + " already exists.");
	}
}
