package indexing;

public class TweetIndexerExistsException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TweetIndexerExistsException(){
		super("An instance of TweetIndexer has been created.");
	}
}
