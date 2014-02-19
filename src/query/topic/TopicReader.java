package query.topic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import common.exception.InstanceExistsException;

public class TopicReader {
	private final static String TOP_START = "<top>";
	private final static String TOP_END = "</top>";
	private final static String TOPNO_REGEX = "<num> Number: MB(.*)</num>";
	private final static String TITLE_REGEX = "<title>(.*)</title>";
	private final static String QT_REGEX = "<querytime>(.*)</querytime>";
	private final static String QTT_REGEX = "<querytweettime>(.*)</querytweettime>";
	private final static String DATE_FORMAT = "EEE MMM dd kk:mm:ss zzzzz yyyy";
	
	private static TopicReader topReader;
	
	private File topFile;
	private BufferedReader reader;
	private Topic top;
	private int total;
	
	private DateFormat df;
	
	/**
	 * 
	 * @param topPath
	 * @return
	 * @throws TopicReaderExistsException
	 * @throws FileNotFoundException
	 */
	public static TopicReader create(String topPath) 
			throws InstanceExistsException, FileNotFoundException{
		if(topReader != null)
			throw new InstanceExistsException(TopicReader.class);
		return new TopicReader(topPath);
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public synchronized boolean hasNext() 
			throws IOException, ParseException{
		boolean isTopStart = false;
		String line;
		while((line = reader.readLine()) != null){
			line = line.trim();
			if(line.isEmpty()) continue;
			if(line.equals(TOP_START)){
				isTopStart = true;
				top = new Topic();
			}
			
			if(line.equals(TOP_END)){
				isTopStart = false;
				total ++;
				return true;
			}
			
			if(isTopStart){
				if(line.matches(TOPNO_REGEX)){
					top.setTopNo(Integer.parseInt(
							line.replaceAll(TOPNO_REGEX, "$1").trim()));
				}else if(line.matches(TITLE_REGEX)){
					top.setTitle(line.replaceAll(TITLE_REGEX, "$1").trim());
				}else if(line.matches(QT_REGEX)){
					top.setQueryTime(
							df.parse(
									line.replaceAll(QT_REGEX, "$1").trim()).getTime());
				}else if(line.matches(QTT_REGEX)){
					top.setQueryTweetTime(Long.parseLong(
							line.replaceAll(QTT_REGEX, "$1").trim()));
				}
			}
		}
		
		return false;
	}
	
	/**
	 * 
	 * @return
	 */
	public synchronized Topic next(){
		return top;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getTotal(){
		return total;
	}
	
	public void close() 
			throws IOException{
		reader.close();
		topReader = null;
	}
	
	private TopicReader(String topPath) 
			throws FileNotFoundException{ 
		topFile = new File(topPath);
		reader = new BufferedReader(
					new InputStreamReader(
							new FileInputStream(topFile)));
		total = 0;
		df = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
	}
}
