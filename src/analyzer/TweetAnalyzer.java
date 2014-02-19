package analyzer;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;

public class TweetAnalyzer extends Analyzer{

	@Override
	public TokenStream tokenStream(String fn, Reader reader) {
		TokenStream ts = new PorterStemFilter(
				new StopFilter(Version.LUCENE_36,
						new LowerCaseFilter(Version.LUCENE_36, 
								new StandardFilter(Version.LUCENE_36,
										new StandardTokenizer(Version.LUCENE_36, reader))), 
						StopAnalyzer.ENGLISH_STOP_WORDS_SET));
		return ts;
	}

}
