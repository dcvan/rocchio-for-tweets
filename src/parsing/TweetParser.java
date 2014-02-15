package parsing;
/**
 * TweetParser
 * 
 * @author dc
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

public class TweetParser {
	
	private final static String FILE_SUFFIX = ".trectext";
	private final static String DOC_START = "<DOC>";
	private final static String DOC_END = "</DOC>";
	private final static String DOCNO_REGEX= "<DOCNO>(.*)</DOCNO>";
	private final static String DATETIME_REGEX = "<DATETIME>(.*)</DATETIME>";
	private final static String DATE_FORMAT = "EEE MMM dd kk:mm:ss z yyyy";
	private final static String USER_REGEX = "<USER>(.*)</USER>";
	private final static String TEXT_REGEX = "<TEXT>(.*)</TEXT>";
	private final static String HTTP_REGEX = "http://[^ \t]*";
	
	private static TweetParser parser;
	private static File langProfiles = new File("profiles");

	private int curDir, curFile;
	private int endDir, endFile;
	private File rootDir;
	private Map<String, String> buf;
	private BufferedReader reader;
	private String lang;
	private boolean ignoreURL;
	
	private int total, hit;
	private String readingFile;
	
	
	/**
	 * create a single instance of TweetsParser
	 * 
	 * @param dirName - the tweets directory
	 * @return an instance of TweetsParser
	 * @throws TweetsParserExistsException - when a TweetsParser instance has been created
	 * @throws WrongFileTypeException 
	 * @throws LangDetectException 
	 * @throws FileNotFoundException 
	 */
	public static TweetParser create(String dirName) 
			throws TweetsParserExistsException, FileNotFoundException, WrongFileTypeException, LangDetectException{ 
		if(parser != null)
			throw new TweetsParserExistsException("An instance of TweetsParser has been created.");
		parser = new TweetParser(dirName);
		return parser;
	}
	
	/**
	 * Get the total of tweets for parsing
	 * 
	 * @return - total of tweets
	 */
	public int getTotal(){
		return total;
	}
	
	/**
	 * Get the number of tweets written in specified language
	 * 
	 * @return - number of tweets written in specified language
	 */
	public int getHits(){
		return hit;
	}
	
	/**
	 * Get currently reading file
	 * @return - file absolute path
	 */
	public String getCurrentFile(){
		return readingFile;
	}
	
	/**
	 * Set the starting file for parsing. Default to the first file
	 * 
	 * @param dirNo - dir number
	 * @param fileNo - file number
	 * @throws WrongFileTypeException 
	 * @throws FileNotFoundException 
	 */
	public void setStartFile(int dirNo, int fileNo) 
			throws FileNotFoundException, WrongFileTypeException{
		int dtotal = rootDir.listFiles().length - 1;
		curDir = (dirNo < dtotal) ? dirNo : dtotal;
		int ftotal = rootDir.listFiles()[curDir].listFiles().length - 1;
		curFile = (fileNo < ftotal) ? fileNo : ftotal;
		reader = getFileReader();
	}
	
	/**
	 * Set the ending file for parsing. Default to the last file
	 * 
	 * @param dirNo - dir number
	 * @param fileNo - file number
	 */
	public void setEndFile(int dirNo, int fileNo){
		int dtotal = rootDir.listFiles().length - 1;
		endDir = (dirNo < dtotal) ? dirNo : dtotal;
		int ftotal = rootDir.listFiles()[endDir].listFiles().length - 1;
		endFile = (fileNo < ftotal) ? fileNo : ftotal;
	}
	
	/**
	 * Set the preferred language. Default to "all"
	 * 
	 * @param - language encoding. "all" will skip language check in hasNext()
	 */
	public void setLanguage(String lang){
		this.lang = lang.toLowerCase();
	}
	
	/**
	 * Set if the parser tweets that contain only URLs.
	 * 
	 * @param - true if ignoring URLs
	 * 		  - false if not ignoring URLs
	 */
	public void setIgnoreURL(boolean ignoreURL){
		this.ignoreURL = ignoreURL;
	}
	
	/**
	 * 1. read from tweet files, return false if reader returns null
	 * 2. parse tweets' content
	 * 3. detect language, skip tweets not written in specified language
	 * 4. instantiate a Tweet instance 
	 * @return - true if a Tweet instance is created
	 *		   - false if a reader returns null
	 *
	 * @throws WrongFileTypeException 
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public boolean hasNext() 
			throws WrongFileTypeException, IOException{
		boolean isDocStart = false;
		String line = null;
		do{
			line = reader.readLine();
			if(line == null){
				reader.close();
				reader = getFileReader();
				if(reader == null)
					return false;
				
				line = reader.readLine();
			}
			line = line.trim();
			if(line.isEmpty()) 
				continue;
			if(line.equals(DOC_START)){
				isDocStart = true;
				continue;
			}
			
			if(line.equals(DOC_END)){
				isDocStart = false;
				total ++;
				if(lang.equals("all")) break;
				if(!checkLanguage(buf.get("text").replaceAll(TEXT_REGEX, "$1")))
					continue;
				break;
			}
			
			if(isDocStart){
				if(line.matches(DOCNO_REGEX)){
					buf.put("docno", line);
				}else if(line.matches(DATETIME_REGEX)){
					buf.put("datetime", line);
				}else if(line.matches(USER_REGEX)){
					buf.put("user", line);
				}else if(line.matches(TEXT_REGEX)){
					buf.put("text", line);
				}
			}
		}while(line != null);
		
		hit ++;
		return true;
	}
	
	

	/**
	 * Parse a raw tweet into a Tweet instance
	 * 
	 * @return - a Tweet instance;
	 */
	public Tweet next(){
		String docNo = buf.get("docno").replaceAll(DOCNO_REGEX, "$1");
		long datetime;
		try {
			 datetime = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH).parse(
					buf.get("datetime").replaceAll(DATETIME_REGEX, "$1")).getTime();
		} catch (ParseException e) {
			datetime = 0;
		}
		String user = buf.get("user").replaceAll(USER_REGEX, "$1");
		String text = buf.get("text").replaceAll(TEXT_REGEX, "$1");
		
		return new Tweet(docNo, datetime, user, text);
	}
	
	/**
	 * Get a raw tweet
	 * 
	 * @return - a raw tweet
	 */
	public String nextRaw(){
		return buf.get("docno") + "\n" 
				+ buf.get("datetime") + "\n" 
				+ buf.get("user") +"\n"
				+ buf.get("text") + "\n";
	}
	
	/**
	 * Get a raw tweet stored in a map
	 * 
	 * @return - a map containing a raw tweet
	 */
	public Map<String, String> nextRawMap(){
		return new HashMap<String, String>(buf);
	}
	
	/**
	 * Clean up
	 * @throws IOException 
	 */
	public void close() 
			throws IOException{
		if(reader != null) reader.close();
		parser = null;
	}
	
	/**
	 * 1. initialize fields
	 * 2. load language detection profiles
	 * 3. change hasCreated to true
	 * 
	 * @param dirName - the tweets directory
	 * @throws FileNotFoundException - when the tweets directory cannot be found
	 * @throws LangDetectException - when the profiles directory cannot be found or profile files are not valid
	 * @throws WrongFileTypeException 
	 */
	private TweetParser(String dirName) 
			throws FileNotFoundException, WrongFileTypeException, LangDetectException{
		rootDir = new File(dirName);
		checkRootDir();
		
		setStartFile(0, 0);
		setEndFile(Integer.MAX_VALUE, Integer.MAX_VALUE);

		total = 0;
		hit = 0;
		readingFile = "";
		
		buf = new HashMap<String, String>();
		
		DetectorFactory.loadProfile(langProfiles);
		setLanguage("all");
		setIgnoreURL(true);
	}
	
	/**
	 * 
	 * @throws FileNotFoundException
	 * @throws WrongFileTypeException
	 */
	private void checkRootDir() 
			throws FileNotFoundException, WrongFileTypeException{
		if(!rootDir.exists())
			throw new FileNotFoundException("No root directory found at " + rootDir.getAbsolutePath());
		if(!rootDir.isDirectory())
			throw new WrongFileTypeException(rootDir.getAbsolutePath() + " is not a directory");
		if(rootDir.listFiles().length == 0)
			throw new FileNotFoundException("No tweets file found");
	}
	
	/**
	 * 
	 * @param buf - a Tweet instance
	 * @return - true if the tweet is authored in the preferred language
	 *         - false otherwise
	 */
	private boolean checkLanguage(String text){
		try {
			Detector detector = DetectorFactory.create();
			detector.append(text);
			if(detector.detect().equals(lang))
				return true;
		} catch (LangDetectException e) {
			if(!ignoreURL && text.matches(HTTP_REGEX))
				return true;
		}	
		
		return false;
	}
	
	/**
	 * 
	 * @param rootDir - the root directory of tweets files
	 * @param curDir - currently reading directory
	 * @param curFile -  currently reading file
	 * @return - a BufferedReader instance if there are tweets files not read
	 * 		   - null if all tweets files have been read
	 * 
	 * @throws FileNotFoundException - when rootDir cannot be found
	 * @throws WrongFileTypeException - when rootDir is not a directory
	 */
	private BufferedReader getFileReader() 
			throws FileNotFoundException, WrongFileTypeException{
	
		while(curDir <= endDir){
			File subDir = rootDir.listFiles()[curDir];
			if(!subDir.isDirectory()){
				curDir ++;
				continue;
			}
			
			while((curDir < endDir && curFile < subDir.listFiles().length) ||
					(curDir == endDir && curFile <= endFile)){
				File file = subDir.listFiles()[curFile];
				if(!file.getName().endsWith(FILE_SUFFIX)){
					curFile ++;
					continue;
				}
				
				curFile ++;
				
				//progress report
				System.out.println("Currently reading: " + file.getAbsolutePath());
				
				readingFile = file.getAbsolutePath();
				return new BufferedReader(
						new InputStreamReader(
								new FileInputStream(file)));
			}
			
			curDir ++;
			curFile = 0;
		}
		
		return null;
	}
	
}

class TweetsParserExistsException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TweetsParserExistsException(String msg){
		super(msg);
	}
}

class WrongFileTypeException extends Exception{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public WrongFileTypeException(String msg){
		super(msg);
	}
}
