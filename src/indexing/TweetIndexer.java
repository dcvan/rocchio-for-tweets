package indexing;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import analyzer.TweetAnalyzer;

import com.cybozu.labs.langdetect.LangDetectException;

import common.exception.FileExistsException;
import common.exception.InstanceExistsException;
import common.exception.WrongFileTypeException;
import parsing.Tweet;
import parsing.TweetParser;

public class TweetIndexer {
	
	private final static String LANG_BASE = "lang-profile";
	private final static String DOCNO = "docno";
	private final static String DATETIME = "datetime";
	private final static String USER = "user";
	private final static String TEXT = "text";
	
	private TweetParser parser;
	private Directory indexDir;
	private IndexWriter writer;
	
	/**
	 * Main method
	 * 
	 * @param args
	 * @throws LangDetectException 
	 * @throws WrongFileTypeException 
	 * @throws IOException 
	 * @throws TweetIndexerExistsException 
	 * @throws FileExistsException 
	 * @throws TweetsParserExistsException 
	 */
	public static void main(String[] args) 
			throws InstanceExistsException, WrongFileTypeException, LangDetectException, IOException, FileExistsException{
		if(args.length != 2){
			System.err.println("<Usage>: java TweetIndexer <tweet dir> <index dir>");
			System.exit(1);
		}
		
		TweetParser parser = new TweetParser(args[0], LANG_BASE);
		String indexDir = args[1];
		parser.setLanguage("en");
		
		TweetIndexer indexer = new TweetIndexer(indexDir, parser, 
				new TweetAnalyzer());
		
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
	 * Constructor
	 * 
	 * @param dir - target directory of index
	 * @param parser - TweetParser instance
	 * @throws IOException - when target directory doesn't exist
	 * @throws FileExistsException 
	 * 
	 */
	public TweetIndexer(String dir, TweetParser parser, Analyzer analyzer) 
			throws IOException, FileExistsException{
		File in = new File(dir);
		if(in.exists())
			throw new FileExistsException(dir);
		in.mkdir();
		this.parser = parser;
		indexDir = FSDirectory.open(in);
		writer = new IndexWriter(indexDir, 
				new IndexWriterConfig(Version.LUCENE_36, analyzer));
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
