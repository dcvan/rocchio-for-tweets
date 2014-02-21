package analysis;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;

public class TweetAnalyzer extends Analyzer{
	private Version matchedVersion;
	
	public TweetAnalyzer(Version matchedVersion){
		this.matchedVersion = matchedVersion;
	}
	@Override
	protected TokenStreamComponents createComponents(String fn, Reader reader) {
		Tokenizer src = new StandardTokenizer(matchedVersion, reader);
		TokenStream tok = new StandardFilter(matchedVersion, src);
		tok = new LowerCaseFilter(matchedVersion, tok);
		tok = new StopFilter(matchedVersion, tok, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
		tok = new PorterStemFilter(tok);
		
		return new TokenStreamComponents(src, tok) {
			
			protected void setReader(final Reader reader) throws IOException{
				super.setReader(reader);
			}
		};
	}

}
