package indexing;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.cybozu.labs.langdetect.LangDetectException;

import parsing.Tweet;
import parsing.TweetParser;
import parsing.TweetParserExistsException;
import parsing.WrongFileTypeException;

public class TweetIndexer {
	
	private final static String DOCNO = "docno";
	private final static String DATETIME = "datetime";
	private final static String USER = "user";
	private final static String TEXT = "text";
	
	private static TweetIndexer indexer;
	
	private TweetParser parser;
	private Directory indexDir;
	private IndexWriter writer;
	private Analyzer analyzer;
	
	
	/**
	 * Main method
	 * 
	 * @param args
	 * @throws LangDetectException 
	 * @throws WrongFileTypeException 
	 * @throws IOException 
	 * @throws TweetIndexerExistsException 
	 * @throws TweetsParserExistsException 
	 */
	public static void main(String[] args) 
			throws TweetParserExistsException, WrongFileTypeException, LangDetectException, TweetIndexerExistsException, IOException{
		if(args.length != 2){
			System.err.println("<Usage>: java TweetIndexer <tweet dir> <index dir>");
			System.exit(1);
		}
		
		TweetParser parser = TweetParser.create(args[0]);
		String indexDir = args[1];
		parser.setLanguage("en");
		
		TweetIndexer indexer = TweetIndexer.create(indexDir, parser);
		
		long start = System.currentTimeMillis();
		indexer.run();
		long end = System.currentTimeMillis();
		
		long timeElapse = end - start;
		System.out.println("time elapsed: " + timeElapse / (1000 * 60) + "m " 
				+ timeElapse / 1000 % 60 + "s " 
				+ timeElapse % 1000 + "ms");
		
		parser.close();
		indexer.close();
	}
	
	/**
	 * Factory method
	 * 
	 * @param dir - target directory of index
	 * @param parser - TweetParser instance
	 * @return - a TweetIndexer instance
	 * @throws TweetIndexerExistsException - when a TweetIndexer instance has been created
	 * @throws IOException - thrown by constructor
	 */
	public static TweetIndexer create(String dir, TweetParser parser) 
			throws TweetIndexerExistsException, IOException{
		if(indexer != null)
			throw new TweetIndexerExistsException();
		indexer = new TweetIndexer(dir, parser);
		return indexer;
	}
	
	/**
	 * Run the indexing
	 * 
	 * @throws IOException 
	 * @throws WrongFileTypeException 
	 */
	public void run() 
			throws WrongFileTypeException, IOException{
		while(parser.hasNext())
			addTweet(parser.next());
	}
	
	/**
	 * Clean up
	 * 
	 * @throws IOException 
	 * @throws CorruptIndexException 
	 */
	public void close() 
			throws CorruptIndexException, IOException{
		writer.close();
		indexDir.close();
	}
	
	/**
	 * Constructor
	 * 
	 * @param dir - target directory of index
	 * @param parser - TweetParser instance
	 * @throws IOException - when target directory doesn't exist
	 * 
	 */
	private TweetIndexer(String dir, TweetParser parser) 
			throws IOException{
		this.parser = parser;
		File tmpDir = new File(dir);
		
		//override existed dir/file at the destination
		if(tmpDir.exists())
			tmpDir.delete();
		tmpDir.mkdirs();
		
		indexDir = FSDirectory.open(tmpDir);
		analyzer = new StandardAnalyzer(Version.LUCENE_36);
		writer = new IndexWriter(indexDir, 
				new IndexWriterConfig(Version.LUCENE_36,analyzer));
	}
	
	/**
	 * Add a tweet into the index
	 * 
	 * @param t - a Tweet instance
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	private void addTweet(Tweet t) 
			throws CorruptIndexException, IOException{
		Document doc = new Document();
		//index DOCNO and DATETIME as numerics
		doc.add(new NumericField(DOCNO, Field.Store.YES, false).setLongValue(
					Long.parseLong(
							t.getDocNo())));
		doc.add(new NumericField(DATETIME, Field.Store.YES, false).setLongValue(
				t.getDateTime()));

		doc.add(new Field(USER,
				t.getUser(),
				Field.Store.YES,
				Field.Index.NOT_ANALYZED_NO_NORMS));
		doc.add(new Field(TEXT,
				t.getText(),
				Field.Store.YES,
				Field.Index.ANALYZED));
		writer.addDocument(doc);
	}
}
