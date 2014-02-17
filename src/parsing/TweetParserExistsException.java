package parsing;

public class TweetParserExistsException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TweetParserExistsException(){
		super("An instance of TweetParser has been created.");
	}
}
