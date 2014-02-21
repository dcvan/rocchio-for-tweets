package query.expansion;

import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.TopDocs;

public class IDFCalculator {
	
	private TopDocs hits;
	private IndexReader reader;
	private int numDocs;
	private Map<String, Float> termMap;
	
	public IDFCalculator(TopDocs hits, IndexReader reader, int n){
		this.hits = hits;
		this.reader = reader;
		numDocs = n;
	}
	
	
}
