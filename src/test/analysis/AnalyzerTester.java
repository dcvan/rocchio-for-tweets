package test.analysis;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;

import analysis.TweetAnalyzer;

public class AnalyzerTester{
	public static void main(String[] args) 
			throws CorruptIndexException, LockObtainFailedException, IOException{
//		String text = "'The sun sets on the war, the day breaks and everything is new' KOC - Winning A Battle, Losing The War";
		String text = "War Rounds: Lessons learned from causalities of War (Iraq and Afghanistan) starts in 10  #GAPACONF";
		Analyzer a1 = new StandardAnalyzer(Version.LUCENE_46);
		Analyzer a2 = new TweetAnalyzer(Version.LUCENE_46);
		
		AnalyzerUtils.displayTokens(a1, text);
		System.out.println();
		AnalyzerUtils.displayTokensWithFullDetails(a1, text);
		System.out.println();
		AnalyzerUtils.displayTokens(a2, text);
		System.out.println();
		AnalyzerUtils.displayTokensWithFullDetails(a2, text);
		
	}
}