package query;

public class TweetQueryLauncherExistsException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TweetQueryLauncherExistsException(){
		super("An instance of TweetQueryLauncher has been created");
	}
}
