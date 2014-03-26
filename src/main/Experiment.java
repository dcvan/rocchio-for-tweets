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
import org.apache.lucene.store.Directory;
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
		for(int i = 3; i <= 7; i ++)
			run.go(config, i);
		
		config.setNote("prepare data");
		for(int i = 5; i <= 25; i += 5){
			config.setDocNum(i);
			for(int j = 5; j <= 25; j += 5){
				config.setTermNum(j);
				for(double k = 0.1; k < 1.0; k += 0.1){
					config.setStep(k);
					run.go(config, 2);
				}
			}
		}
		
		config.setWithHashtags(true);
		for(int i = 5; i <= 20; i += 5){
			config.setDocNum(i);
			for(int j = 5; j <= 25; j += 5){
				config.setTermNum(j);
				for(double k = 0.1; k < 1.0; k += 0.1){
					config.setStep(k);
					run.go(config, 2);
				}
			}
		}
		
//		config.setWithTerms(false);
//		config.setWithHashtags(true);
//		for(int i = 5; i <= 25; i += 5){
//			config.setDocNum(i);
//			for(int j = 5; j <= 25; j += 5){
//				config.setTermNum(j);
//				for(double k = 0.1; k < 1.0; k += 0.1){
//					config.setStep(k);
//					run.go(config, 2);
//				}
//			}
//		}
//		
//		config.setWithHashtags(false);
//		config.setWithTerms(true);
//		
//		config.setStep(0.1);
//		config.setNote("prepare data");
//		for(int i = 100; i <= 500; i += 100){
//			config.setDocNum(i);
//			for(int j = 5; j <= 25; j += 5){
//				config.setTermNum(j);
//				run.go(config, 2);
//			}
//		}
//		
//		config.setWithHashtags(true);
//		config.setStep(0.1);
//		for(int i = 100; i <= 500; i += 100){
//			config.setDocNum(i);
//			for(int j = 5; j <= 25; j += 5){
//				config.setTermNum(j);
//				run.go(config, 2);
//			}
//		}
//		
//		config.setWithTerms(false);
//		config.setStep(0.1);
//		for(int i = 100; i <= 500; i += 100){
//			config.setDocNum(i);
//			for(int j = 5; j <= 25; j += 5){
//				config.setTermNum(j);
//				run.go(config, 2);
//			}
//		}
//		
//		config.setWithHashtags(false);
//		config.setWithTerms(true);
//		
//		config.setDocNum(1000);
//		for(int j = 5; j <= 25; j += 5){
//			config.setTermNum(j);
//			run.go(config, 2);
//			config.setWithHashtags(true);
//			run.go(config, 2);
//			config.setWithTerms(false);
//			run.go(config, 2);
//			config.setWithHashtags(false);
//			config.setWithTerms(true);
//		}
//		
//		run.close();

		Directory runRec = FSDirectory.open(new File(REC_BASE));
		IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(runRec));
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
		
		runRec.close();
		searcher.getIndexReader().close();
	}
}
