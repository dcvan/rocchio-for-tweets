package eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import common.exception.InstanceExistsException;
import common.exception.InvalidParameterException;
import common.exception.WrongFileTypeException;

public class TweetSearchEvaluator {
	private final static String TREC_EVAL = "trec_eval/trec_eval -q -m ";
	
	private static Set<String> metrics = new HashSet<String>();
	static{
		metrics.add("map");
		metrics.add("gm_map");
		metrics.add("rprec");
		metrics.add("bpref");
		metrics.add("recip_rank");
		metrics.add("iprec_at_recall_0.00");
		metrics.add("iprec_at_recall_0.10");
		metrics.add("iprec_at_recall_0.20");
		metrics.add("iprec_at_recall_0.30");
		metrics.add("iprec_at_recall_0.40");
		metrics.add("iprec_at_recall_0.50");
		metrics.add("iprec_at_recall_0.60");
		metrics.add("iprec_at_recall_0.70");
		metrics.add("iprec_at_recall_0.80");
		metrics.add("iprec_at_recall_0.90");
		metrics.add("iprec_at_recall_1.00");
		metrics.add("p_5");
		metrics.add("p_10");
		metrics.add("p_15");
		metrics.add("p_20");
		metrics.add("p_30");
		metrics.add("p_100");
		metrics.add("p_200");
		metrics.add("p_500");
		metrics.add("p_1000");
		metrics.add("recall_5");
		metrics.add("recall_10");
		metrics.add("recall_15");
		metrics.add("recall_20");
		metrics.add("recall_30");
		metrics.add("recall_100");
		metrics.add("recall_200");
		metrics.add("recall_500");
		metrics.add("recall_1000");
		metrics.add("infap");
		metrics.add("gm_bpref");
		metrics.add("rprec_mult_0.20");
		metrics.add("rprec_mult_0.40");
		metrics.add("rprec_mult_0.60");
		metrics.add("rprec_mult_0.80");
		metrics.add("rprec_mult_1.00");
		metrics.add("rprec_mult_1.20");
		metrics.add("rprec_mult_1.40");
		metrics.add("rprec_mult_1.60");
		metrics.add("rprec_mult_1.80");
		metrics.add("rprec_mult_2.00");
		metrics.add("utility");
		metrics.add("11pt_avg");
		metrics.add("bing");
		metrics.add("g");
		metrics.add("ndcg");
		metrics.add("ndcg_rel");
		metrics.add("rndcg");
		metrics.add("ndcg_cut_5");
		metrics.add("ndcg_cut_10");
		metrics.add("ndcg_cut_15");
		metrics.add("ndcg_cut_20");
		metrics.add("ndcg_cut_30");
		metrics.add("ndcg_cut_100");
		metrics.add("ndcg_cut_200");
		metrics.add("ndcg_cut_500");
		metrics.add("ndcg_cut_1000");
		metrics.add("relative_p_5");
		metrics.add("relative_p_10");
		metrics.add("relative_p_15");
		metrics.add("relative_p_20");
		metrics.add("relative_p_30");
		metrics.add("relative_p_100");
		metrics.add("relative_p_200");
		metrics.add("relative_p_500");
		metrics.add("relative_p_1000");
		metrics.add("success_1");
		metrics.add("success_5");
		metrics.add("success_10");
		metrics.add("set_p");
		metrics.add("set_relative_p");
		metrics.add("set_recall");
		metrics.add("set_map");
		metrics.add("set_f");
		metrics.add("all_trec");
	}
	private static TweetSearchEvaluator eval;
	
	private String qrel;
	private String result;
	
	public static void main(String[] args) 
			throws WrongFileTypeException, InstanceExistsException, IOException, InvalidParameterException{
		TweetSearchEvaluator eval = TweetSearchEvaluator.create("test-collection/microblog11-qrels.txt", 
				"test-collection/result.txt.2");
		
		System.out.println(eval.evaluate());
	}
	
	public static TweetSearchEvaluator create(String qpath, String rpath) 
			throws FileNotFoundException, WrongFileTypeException, InstanceExistsException{
		File q = new File(qpath);
		File r = new File(rpath);
		
		if(eval != null)
			throw new InstanceExistsException(TweetSearchEvaluator.class);
		if(!q.exists())
			throw new FileNotFoundException("Qrel file " + qpath + " not found.");
		if(!q.isFile())
			throw new WrongFileTypeException(qpath + " is not a text file");
		
		if(!r.exists())
			throw new FileNotFoundException("Result file " + rpath + " not found.");
		if(!r.isFile())
			throw new WrongFileTypeException(rpath + " is not a text file");
		
		return new TweetSearchEvaluator(qpath, rpath);
	}
	
	public String evaluate() 
			throws IOException, InvalidParameterException{
		return evaluate("all", "all");
	}
	
	public String evaluate(String metric) 
			throws IOException, InvalidParameterException{
		return evaluate(metric, "all");
	}
	
	public String evaluate(int topNo) 
			throws IOException, InvalidParameterException{
		return evaluate("all", String.valueOf(topNo));
	}
	
	public String evaluate(String metric, String topNo) 
			throws IOException, InvalidParameterException{
		metric = metric.toLowerCase();
		if("all".equals(metric))
			metric = "all_trec";
		if(metric == null || !metrics.contains(metric))
			throw new InvalidParameterException(metric);
		if(topNo == null || 
				(!"all".equals(topNo) && !topNo.matches("[0-9]+")))
			throw new InvalidParameterException(topNo);
		
		StringBuilder cb = new StringBuilder();
		StringBuilder res = new StringBuilder();
		cb.append(TREC_EVAL)
			.append(metric).append(' ')
			.append(qrel).append(' ')
			.append(result);
		
		Process p = Runtime.getRuntime().exec(cb.toString());
		String line;
		
		BufferedReader resReader = new BufferedReader(
				new InputStreamReader(p.getInputStream()));
		while((line = resReader.readLine()) != null){
			String[] fields = line.split("\t");
			if(topNo.equals(fields[1]))
				res.append(line).append('\n');
		}
		resReader.close();
			
		BufferedReader errReader = new BufferedReader(
				new InputStreamReader(p.getErrorStream()));
		line = null;
		while((line = errReader.readLine()) != null){
			res.append(line).append('\n');
		}
		errReader.close();
		
		return res.toString();
	}
	
	private TweetSearchEvaluator(String q, String r){
		qrel = q;
		result = r;
	} 
	

	
}
