package query;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import common.exception.ResetException;

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
	private Map<BytesRef, Float> idfMap;
	private Set<String> queryTerms;
	private DefaultSimilarity sim;
	private int numRetDocs;
	private int numTerms;
	
	public TermCollector(int numDocs, int numTerms){
		this(null, null, null, numDocs, numTerms);
	}
	
	public TermCollector(Query query, ScoreDoc[] scoreDocs, IndexReader indexReader, int numDocs, int numTerms){
		this.scoreDocs = scoreDocs;
		this.indexReader = indexReader;
		termMap = new HashMap<String, Float>();
		idfMap = new HashMap<BytesRef, Float>();
		queryTerms = extractTerms(query);
		sim = new DefaultSimilarity();
		this.numRetDocs = numDocs;
		this.numTerms = numTerms;
	}
	/**
	 * Get top terms of current search
	 * 
	 * @return
	 * @throws IOException 
	 * @throws Exception 
	 */
	public Map<String, Float> getTerms() 
			throws ResetException, IOException{
		if(queryTerms == null || scoreDocs == null || indexReader == null)
			throw new ResetException("TermCollector must be reset before reusing");
		rankTerms();
//		System.out.println(termMap);
		Map<String, Float> tmpMap = new HashMap<String, Float>();
		int cnt = 0;
		for(String term : termMap.keySet()){
			if(cnt == numTerms) break;
//			if(term.length() < 3) continue;
//			if(term.equals("http") || term.equals("bit.li")) continue;
			cnt ++;
//			if(queryTerms.contains(term)) continue;
 			tmpMap.put(term, termMap.get(term));
		}
		
		return tmpMap;
	}
	
	/**
	 * Get query terms of current search
	 * 
	 * @return
	 */
	public Set<String> getQueryTerms(){
		return new HashSet<String>(queryTerms);
	}
	
	void reset(Query q, ScoreDoc[] scoreDocs, IndexReader indexReader){
		queryTerms = extractTerms(q);
		this.scoreDocs = scoreDocs;
		this.indexReader = indexReader;
	}
	
	void clean(){
		reset(null,null,null);
	}
	
	private void rankTerms() 
			throws IOException{
		termMap = new HashMap<String, Float>();
		Directory tmpDir = new RAMDirectory();
		IndexWriter writer = new IndexWriter(tmpDir, 
				new IndexWriterConfig(Version.LUCENE_46, 
						new EnglishAnalyzer(Version.LUCENE_46)));
		
		for(int i = 0; i < ((numRetDocs < scoreDocs.length) ? numRetDocs : scoreDocs.length); i ++)
			writer.addDocument(indexReader.document(scoreDocs[i].doc));
		writer.close();
		
		IndexReader ireader = DirectoryReader.open(tmpDir);
		int numDocs = ireader.numDocs();
		IndexSearcher searcher = new IndexSearcher(ireader);
		ScoreDoc[] docs = searcher.search(new MatchAllDocsQuery(), numRetDocs).scoreDocs;
		for(ScoreDoc sd : docs){
			TermsEnum te = ireader.getTermVector(sd.doc, TEXT_FN).iterator(null);
			BytesRef br;
			while((br = te.next()) != null){
				if(te.seekExact(br)){
					float idf;
					if(idfMap.containsKey(br)){
						idf = idfMap.get(br);
					}else{
						int docFreq = ireader.docFreq(new Term(TEXT_FN, br));
						idf = sim.idf(docFreq, numDocs);
						idfMap.put(br, idf);
					}
					
					String term = br.utf8ToString();
					if(termMap.containsKey(term)){
						termMap.put(term, termMap.get(term) + idf);
					}else{
						termMap.put(term, idf);
					}
				}
			}
		}
		
		Map<String, Float> sortedMap = new TreeMap<String, Float>(
				new ValueComparator(termMap));
		sortedMap.putAll(termMap);
		termMap = sortedMap;
		
		tmpDir.close();
	}
	
	private Set<String> extractTerms(Query q){
		if(q == null) return null;
		Set<Term> tset = new HashSet<Term>();
		Set<String> res = new HashSet<String>();
		q.extractTerms(tset);
		for(Term t : tset)
			res.add(t.text());
		return res;
	}
}
