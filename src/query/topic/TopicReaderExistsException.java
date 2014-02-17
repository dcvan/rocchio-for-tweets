package query.topic;

public class TopicReaderExistsException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TopicReaderExistsException(){
		super("An instance of TopicReader has been created");
	}
}
