package parsing;

/**
 * TweetExtractor
 * 
 * Extract tweet collection in arbitrary size written in specific language.
 * 
 * @author dc
 */
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import com.cybozu.labs.langdetect.LangDetectException;

public class TweetExtractor {
	//main method
	public static void main(String[] args) 
			throws TweetsParserExistsException, WrongFileTypeException, LangDetectException, IOException{
		if(args.length < 3 || args.length > 7){
			System.err.println("Usage: java EnglishSnapshotCreator <source dir> <dest file> <language> [<start subdir> <start file> <end subdir> <end file>]");
			System.exit(1);
		}
		
		//declare basic parameters for parsing tweet collection
		String srcDir = args[0];
		String dest = args[1];
		String lang = args[2];
		int startDir = 0, startFile = 0, endDir = Integer.MAX_VALUE, endFile = Integer.MAX_VALUE;

		if(args.length == 4){
			startDir = Integer.parseInt(args[3]);
		}else if(args.length == 5){
			startDir = Integer.parseInt(args[3]);
			startFile = Integer.parseInt(args[4]);
		}else if(args.length == 6){
			startDir = Integer.parseInt(args[3]);
			startFile = Integer.parseInt(args[4]);
			if(startDir < endDir){
				System.err.println("The number of ending subdirectory must be larger than the number of starting subdirectory");
				System.exit(1);
			}
			endDir = Integer.parseInt(args[5]);
		}else if(args.length == 7){
			startDir = Integer.parseInt(args[3]);
			startFile = Integer.parseInt(args[4]);
			if(startDir < endDir){
				System.err.println("The number of ending subdirectory must be larger than the number of starting subdirectory");
				System.exit(1);
			}
			endDir = Integer.parseInt(args[5]);
			endFile = Integer.parseInt(args[6]);
			if(startDir == endDir &&
					startFile < endFile){
				System.err.println("The number of ending file must be larger than the number of starting file in the same subdirectory");
				System.exit(1);
			}
		}
		
		//new and configure TweetParser instance
		TweetParser parser = TweetParser.create(srcDir);
		parser.setLanguage(lang);
		parser.setStartFile(startDir, startFile);
		parser.setEndFile(endDir, endFile);
		
		//override existing destination file
		File destFile = new File(dest);
		if(destFile.exists())
			destFile.delete();
		
		//timing
		long startTime = System.currentTimeMillis();
		
		//write to destination file
		PrintWriter writer = new PrintWriter(destFile);
		while(parser.hasNext())
			writer.write(parser.nextRaw());
		
		long endTime = System.currentTimeMillis();
		long timeElapsed = endTime - startTime;
		
		//summary
		System.out.println(parser.getHits() + "/" + parser.getTotal() + " copied.");
		System.out.println("time elapsed: " + timeElapsed / 1000 / 60 + "m " 
				+ timeElapsed / 1000 % 60 + "s " 
				+ timeElapsed % 1000 + "ms");
		
		//clean up
		writer.close();
		parser.close();
	}
}
