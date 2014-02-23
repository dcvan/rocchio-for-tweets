package query.expansion;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;

public class TermCollector {
	
	private class ValueComparator implements Comparator<String>{
		
		private Map<String, Float> map;
		public ValueComparator(Map<String, Float> map){
			this.map = map;
		}
		
		@Override
		public int compare(String arg0, String arg1) {
			Float v0 = map.get(arg0);
			Float v1 = map.get(arg1);
			if(v0.equals(v1))
				return arg0.compareTo(arg1);
			return - v0.compareTo(v1);
		}
		
	}
	
	private final static String TEXT_FN = "text";
	private ScoreDoc[] scoreDocs;
	private IndexReader indexReader;
	private Map<String, Float> termMap;
	private DefaultSimilarity sim;
	
	public TermCollector(ScoreDoc[] scoreDocs, IndexReader indexReader){
		this.scoreDocs = scoreDocs;
		this.indexReader = indexReader;
		termMap = new HashMap<String, Float>();
		sim = new DefaultSimilarity();
	}
	
	public Map<String, Float> getTerms(int n) 
			throws IOException{
		rankTerms();
//		System.out.println(termMap);
		Map<String, Float> tmpMap = new HashMap<String, Float>();
		int cnt = 0;
		for(String term : termMap.keySet()){
			if(cnt == n) break;
 			tmpMap.put(term, termMap.get(term));
			cnt ++;
		}
		
		return tmpMap;
	}
	
	private void rankTerms() 
			throws IOException{
		Bits liveDocs = MultiFields.getLiveDocs(indexReader);
		int numDocs = indexReader.numDocs();
		for(ScoreDoc sd : scoreDocs){
			TermsEnum te = indexReader.getTermVector(sd.doc, TEXT_FN).iterator(null);
			BytesRef br;
			while((br = te.next()) != null){
				if(te.seekExact(br)){
					int docFreq = indexReader.docFreq(new Term(TEXT_FN, br));
					DocsEnum de = te.docs(liveDocs, null);
					if(de == null) return;
					while(de.nextDoc() != DocsEnum.NO_MORE_DOCS){
						float tfidf = de.freq() * sim.idf(docFreq, numDocs);
						String term = br.utf8ToString();
						if(termMap.get(term) == null)
							termMap.put(term, tfidf);
						else
							termMap.put(term, termMap.get(term) + tfidf);
					}
				}
			}
		}
		
		Map<String, Float> sortedMap = new TreeMap<String, Float>(
				new ValueComparator(termMap));
		sortedMap.putAll(termMap);
		termMap = sortedMap;
	}
}
