package main;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

public class Display {

	public static void main(String[] args) 
			throws IOException {
		String REC_BASE = System.getProperty("user.home") + "/Documents/run";
		
		IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(REC_BASE))));
		TopDocs hits = searcher.search(new MatchAllDocsQuery(), Integer.MAX_VALUE);
		Map<Long, Document> res = new TreeMap<Long, Document>();
		for(ScoreDoc sd : hits.scoreDocs){
			Document d = searcher.doc(sd.doc);
			res.put(d.getField("timestamp").numericValue().longValue(), d);
		}
		
		for(Document d : res.values()){
			List<IndexableField> fields = d.getFields();
			for(IndexableField f : fields){
				System.out.println(f.name() + ": " + f.stringValue());
			}
		}
	}

}
