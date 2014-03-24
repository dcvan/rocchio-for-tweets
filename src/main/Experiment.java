package main;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import run.Run;
import run.RunConfig;
import common.exception.FileExistsException;
import common.exception.InstanceExistsException;
import common.exception.InvalidParameterException;
import common.exception.ResetException;
import common.exception.TweetSearchEvaluatorException;
import common.exception.WrongFileTypeException;

public class Experiment {
	public static void main(String[] args) 
			throws IOException, ParseException, org.apache.lucene.queryparser.classic.ParseException, InvalidParameterException, TweetSearchEvaluatorException, FileExistsException, WrongFileTypeException, InstanceExistsException, ResetException{
		String REC_BASE = System.getProperty("user.home") + "/Documents/run";
		RunConfig config = new RunConfig(5, 5, 
				new EnglishAnalyzer(Version.LUCENE_46), 0.1, true, false, 
				new String[]{"P_30", "map", "ndcg"}, "num of iterations");
		
		Run run = new Run();
		for(int i = 2; i < 6; i ++)
			run.go(config, i);
		
		config.setNote("num of top M terms in top N docs with decreasing boost at a specific step");
		for(int i = 5; i <= 25; i += 5){
			config.setDocNum(i);
			for(int j = 5; j <= 25; j += 5){
				config.setTermNum(j);
				for(double k = 0.1; k < 1.0; k += 0.3){
					config.setStep(k);
					run.go(config, 2);
				}
				for(double k = 1.0; k < 10; k += 3){
					config.setStep(k);
					run.go(config, 2);
				}
			}
		}
		
		run.close();
		
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
