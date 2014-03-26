package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JDialog;
import javax.swing.JPanel;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import de.erichseifert.gral.data.DataSeries;
import de.erichseifert.gral.data.DataTable;
import de.erichseifert.gral.data.comparators.Ascending;
import de.erichseifert.gral.plots.XYPlot;
import de.erichseifert.gral.plots.lines.DefaultLineRenderer2D;
import de.erichseifert.gral.ui.InteractivePanel;
import de.erichseifert.gral.util.Insets2D;
import de.erichseifert.gral.util.Location;

public class Analysis {

	private class plotConfig{
		String title;
		String legend;
		double maxX;
		String labelX;
		double maxY;
		String labelY;
		
	
	}
	
	private final static String REC_BASE = System.getProperty("user.home") + "/Documents/run";
	private final static int WIDTH = 600;
	private final static int HEIGHT = 700;
	private final static double TOP = 30.0;
	private final static double LEFT = 75.0;
	private final static double BOTTOM = 70.0;
	private final static double RIGHT = 125.0;
	private final static double Y_LABEL_DIST = 2.0;
	
	public static void main(String[] args) 
			throws IOException {

//		plotIterations("p@30");
		plotStepDocNum(0.1, 1.0, 100, 500, 25, "ndcg");
//		plotStepTermNum(0.1, 1.0, 5, 25, 5, "ndcg");
//		plotDocNumTermNum(100, 500, 5, 25, 0.1, "p@30");
//		plotTermNumDocNum(5, 25, 5, 25, 0.1, "p@30");
//		plotTermNumDocNum(5, 25, 5, 25, 0.1, "ndcg");
//		plotTermNumDocNum(5, 25, 5, 25, 0.1, "map");
	}
	
	//only selected terms, 5 terms, 5 docs, 0.1 step by default
	@SuppressWarnings("unchecked")
	public static void plotIterations(String metric) 
			throws IOException{
		int maxIterNum = 0;
		double maxImpr = 0;
		Directory recDir = FSDirectory.open(new File(REC_BASE));
		IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(recDir));
		Query stepQuery = NumericRangeQuery.newDoubleRange("step", 0.1, 0.1, true, true);
		Query docQuery = NumericRangeQuery.newIntRange("document number", 5, 5, true, true);
		Query termQuery = NumericRangeQuery.newIntRange("term number", 5, 5, true, true);
		Query htagOccurQuery = new TermQuery(new Term("with hashtags", "false"));
		Query termOccurQuery = new TermQuery(new Term("with selected terms", "true"));
		
		BooleanQuery single = new BooleanQuery();
		single.add(stepQuery, BooleanClause.Occur.MUST);
		single.add(docQuery, BooleanClause.Occur.MUST);
		single.add(termQuery, BooleanClause.Occur.MUST);
		single.add(htagOccurQuery, BooleanClause.Occur.MUST);
		single.add(termOccurQuery, BooleanClause.Occur.MUST);
		
		Query multiple = new TermQuery(new Term("run type", "multiple iterations"));
		BooleanQuery query = new BooleanQuery();
		query.add(single, BooleanClause.Occur.SHOULD);
		query.add(multiple, BooleanClause.Occur.SHOULD);
		
		TopDocs hits = searcher.search(query, Integer.MAX_VALUE);
		DataTable table = new DataTable(Integer.class, Double.class);
		table.add(0, 0.0);
		for(ScoreDoc sd : hits.scoreDocs){
			Document d = searcher.doc(sd.doc);
			int iterNum = d.getValues("metrics").length - 1;
			double impr = getValue(d.getField("improvement").stringValue(), metric);
			maxIterNum = Math.max(iterNum, maxIterNum);
			maxImpr = Math.max(impr, maxImpr);
			table.add(iterNum, impr);
		}
		
		table.sort(new Ascending(0));
		DataSeries data = new DataSeries("5 Terms", table, 0, 1);
		XYPlot plot = new XYPlot(data);
	    plot.setInsets(new Insets2D.Double(TOP, LEFT, BOTTOM, RIGHT - 50.0));
	    plot.getTitle().setText(metric.toUpperCase() + " Improvement(step=0.1, tweets#=5, terms#=5)");
	    plot.getAxis(XYPlot.AXIS_X).setMin(0);
	    plot.getAxis(XYPlot.AXIS_X).setMax(maxIterNum + 0.1);
	    plot.getAxis(XYPlot.AXIS_Y).setMax(maxImpr + 0.01);
	    plot.getAxisRenderer(XYPlot.AXIS_X).setLabel("Number of Iterations");
	    plot.getAxisRenderer(XYPlot.AXIS_Y).setLabel(metric + " Improvement");
	    plot.getAxisRenderer(XYPlot.AXIS_Y).setLabelDistance(Y_LABEL_DIST);
	    plot.setLineRenderer(data, new DefaultLineRenderer2D());
	    	
	    float a = (float)Math.random(), 
	    	  b = (float)Math.random(), 
	    	  c = (float)Math.random();
	    Color color = new Color(a, b, c);
	    plot.getPointRenderer(data).setColor(color);
	    plot.getLineRenderer(data).setColor(color);
	    
		JDialog dialog = new JDialog();
		dialog.setTitle(metric.toUpperCase() + " Improvement(no hashtag, step=0.1, tweets#=5, terms#=5)");
		dialog.setSize(HEIGHT, WIDTH);
		dialog.setResizable(true);
		
		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout());
		dialog.setContentPane(contentPane);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		contentPane.add(new InteractivePanel(plot), BorderLayout.CENTER);
		dialog.setVisible(true);
		
		recDir.close();
		searcher.getIndexReader().close();
	}
	
	public static void plotHashtagDocNum(){
		
	}
	
	public static void plotHashtagTermNum(){
		
	}
	
	//only with terms and single iteration by default 
	@SuppressWarnings("unchecked")
	public static void plotDocNumTermNum(int dnstart, int dnend, int tnstart, int tnend, double step, String metric) 
			throws IOException{
		int maxDocNum = 0;
		double maxImpr = 0;
		
		Directory recDir = FSDirectory.open(new File(REC_BASE));
		IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(recDir));
		Query stepQuery = NumericRangeQuery.newDoubleRange("step", step, step, true, true);
		Query docQuery = NumericRangeQuery.newIntRange("document number", dnstart, dnend, true, true);
		Query termQuery = NumericRangeQuery.newIntRange("term number", tnstart, tnend, true, true);
		Query runTypeQuery = new TermQuery(new Term("run type", "single iteration"));
		Query htagOccurQuery = new TermQuery(new Term("with hashtags", "false"));
		Query termOccurQuery = new TermQuery(new Term("with selected terms", "true"));
		
		BooleanQuery query = new BooleanQuery();
		query.add(stepQuery, BooleanClause.Occur.MUST);
		query.add(docQuery, BooleanClause.Occur.MUST);
		query.add(termQuery, BooleanClause.Occur.MUST);
		query.add(runTypeQuery,BooleanClause.Occur.MUST);
		query.add(htagOccurQuery, BooleanClause.Occur.MUST);
		query.add(termOccurQuery, BooleanClause.Occur.MUST);
		
		TopDocs hits = searcher.search(query, Integer.MAX_VALUE);
		Map<Integer, DataTable> dsMap = new TreeMap<Integer, DataTable>();
		for(ScoreDoc sd : hits.scoreDocs){
			Document d = searcher.doc(sd.doc);
			int docNum = d.getField("document number").numericValue().intValue();
			int termNum = d.getField("term number").numericValue().intValue();
			double impr = getValue(d.getField("improvement").stringValue(), metric);
			if(maxDocNum < docNum) maxDocNum = docNum;
			if(maxImpr < impr) maxImpr = impr;
			if(dsMap.containsKey(termNum)){
				dsMap.get(termNum).add(docNum, impr);
			}else{
				DataTable table = new DataTable(Integer.class, Double.class);
				//insert a dummy tuple to show improvement between baseline run and following iterations
				table.add(0, 0.0);
				table.add(docNum, impr);
				dsMap.put(termNum, table);
			}
		}
		
	    visualize(dsMap,
	    		metric.toUpperCase() + " Improvement(no hashtags, step=" + step + ")", 
	    		" Terms",
	    		"Number of Feedback Tweets",
	    		maxDocNum + 1, 
	    		metric.toUpperCase() + " Improvement",
	    		maxImpr + 0.01);
	    
	    recDir.close();
	    searcher.getIndexReader().close();
	}
	
	//only with terms and single iteration by default 
	@SuppressWarnings("unchecked")
	public static void plotTermNumDocNum(int tnstart, int tnend, int dnstart, int dnend, double step, String metric) 
			throws IOException{
		int maxTermNum = 0;
		double maxImpr = 0;
		
		Directory recDir = FSDirectory.open(new File(REC_BASE));
		IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(recDir));
		Query stepQuery = NumericRangeQuery.newDoubleRange("step", step, step, true, true);
		Query docQuery = NumericRangeQuery.newIntRange("document number", dnstart, dnend, true, true);
		Query termQuery = NumericRangeQuery.newIntRange("term number", tnstart, tnend, true, true);
		Query runTypeQuery = new TermQuery(new Term("run type", "single iteration"));
		Query htagOccurQuery = new TermQuery(new Term("with hashtags", "false"));
		Query termOccurQuery = new TermQuery(new Term("with selected terms", "true"));
		
		BooleanQuery query = new BooleanQuery();
		query.add(stepQuery, BooleanClause.Occur.MUST);
		query.add(docQuery, BooleanClause.Occur.MUST);
		query.add(termQuery, BooleanClause.Occur.MUST);
		query.add(runTypeQuery,BooleanClause.Occur.MUST);
		query.add(htagOccurQuery, BooleanClause.Occur.MUST);
		query.add(termOccurQuery, BooleanClause.Occur.MUST);
		
		TopDocs hits = searcher.search(query, Integer.MAX_VALUE);
		Map<Integer, DataTable> dsMap = new TreeMap<Integer, DataTable>();
		for(ScoreDoc sd : hits.scoreDocs){
			Document d = searcher.doc(sd.doc);
			int docNum = d.getField("document number").numericValue().intValue();
			int termNum = d.getField("term number").numericValue().intValue();
			double impr = getValue(d.getField("improvement").stringValue(), metric);
			if(maxTermNum < termNum) maxTermNum = termNum;
			if(maxImpr < impr) maxImpr = impr;
			if(dsMap.containsKey(docNum)){
				dsMap.get(docNum).add(termNum, impr);
			}else{
				DataTable table = new DataTable(Integer.class, Double.class);
				//insert a dummy tuple to show improvement between baseline run and following iterations
				table.add(0, 0.0);
				table.add(termNum, impr);
				dsMap.put(docNum, table);
			}
		}
		
	    visualize(dsMap,
	    		metric.toUpperCase() + " Improvement(no hashtags, weight descending step=" + step + ")", 
	    		" Tweets",
	    		"Number of Feedback Terms",
	    		maxTermNum + 1,
	    		metric.toUpperCase() + " Improvement",
	    		maxImpr + 0.01);
	    
	    recDir.close();
	    searcher.getIndexReader().close();
	}
	
	//only with terms and single iteration by default 
	@SuppressWarnings("unchecked")
	public static void plotStepDocNum(double start, double end, int dnstart, int dnend, int termNum, String metric) 
			throws IOException{
		double maxStep = 0;
		double maxImpr = 0;
		
		Directory recDir = FSDirectory.open(new File(REC_BASE));
		IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(recDir));
		Query stepQuery = NumericRangeQuery.newDoubleRange("step", start, end, true, true);
		Query docQuery = NumericRangeQuery.newIntRange("document number", dnstart, dnend, true, true);
		Query termQuery = NumericRangeQuery.newIntRange("term number", termNum, termNum, true, true);
		Query runTypeQuery = new TermQuery(new Term("run type", "single iteration"));
		Query htagOccurQuery = new TermQuery(new Term("with hashtags", "false"));
		Query termOccurQuery = new TermQuery(new Term("with selected terms", "true"));
		
		BooleanQuery query = new BooleanQuery();
		query.add(stepQuery, BooleanClause.Occur.MUST);
		query.add(docQuery, BooleanClause.Occur.MUST);
		query.add(termQuery, BooleanClause.Occur.MUST);
		query.add(runTypeQuery,BooleanClause.Occur.MUST);
		query.add(htagOccurQuery, BooleanClause.Occur.MUST);
		query.add(termOccurQuery, BooleanClause.Occur.MUST);
		
		TopDocs hits = searcher.search(query, Integer.MAX_VALUE);
		Map<Integer, DataTable> dsMap = new TreeMap<Integer, DataTable>();
		for(ScoreDoc sd : hits.scoreDocs){
			Document d = searcher.doc(sd.doc);
			int docNum = d.getField("document number").numericValue().intValue();
			double step = d.getField("step").numericValue().doubleValue();
			double impr = getValue(d.getField("improvement").stringValue(), metric);
			if(maxStep < step) maxStep = step;
			if(maxImpr < impr) maxImpr = impr;
			if(dsMap.containsKey(docNum)){
				dsMap.get(docNum).add(step, impr);
			}else{
				DataTable table = new DataTable(Double.class, Double.class);
				//insert a dummy tuple to show improvement between baseline run and following iterations
				table.add(0.0, 0.0);
				table.add(step, impr);
				dsMap.put(docNum, table);
			}
		}
		
	    visualize(dsMap,
	    		metric.toUpperCase() + " Improvement(no hashtags, feedback terms number=" + termNum + ")", 
	    		" Tweets",
	    		"Weight Decreasing Step",
	    		maxStep + 0.1,
	    		metric.toUpperCase() + " Improvement",
	    		maxImpr + 0.01);
	    
	    recDir.close();
	    searcher.getIndexReader().close();
	}
	
	//only with terms and single iteration by default 
	@SuppressWarnings("unchecked")
	public static void plotStepTermNum(double start, double end, int tnstart, int tnend, int docNum, String metric) 
			throws IOException{
		double maxStep = 0;
		double maxImpr = 0;
		
		Directory recDir = FSDirectory.open(new File(REC_BASE));
		IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(recDir));
		Query stepQuery = NumericRangeQuery.newDoubleRange("step", start, end, true, true);
		Query docQuery = NumericRangeQuery.newIntRange("document number", docNum, docNum, true, true);
		Query termQuery = NumericRangeQuery.newIntRange("term number", tnstart, tnend, true, true);
		Query runTypeQuery = new TermQuery(new Term("run type", "single iteration"));
		Query htagOccurQuery = new TermQuery(new Term("with hashtags", "false"));
		Query termOccurQuery = new TermQuery(new Term("with selected terms", "true"));
		
		BooleanQuery query = new BooleanQuery();
		query.add(stepQuery, BooleanClause.Occur.MUST);
		query.add(docQuery, BooleanClause.Occur.MUST);
		query.add(termQuery, BooleanClause.Occur.MUST);
		query.add(runTypeQuery,BooleanClause.Occur.MUST);
		query.add(htagOccurQuery, BooleanClause.Occur.MUST);
		query.add(termOccurQuery, BooleanClause.Occur.MUST);
		
		TopDocs hits = searcher.search(query, Integer.MAX_VALUE);
		Map<Integer, DataTable> dsMap = new TreeMap<Integer, DataTable>();
		for(ScoreDoc sd : hits.scoreDocs){
			Document d = searcher.doc(sd.doc);
			int termNum = d.getField("term number").numericValue().intValue();
			double step = d.getField("step").numericValue().doubleValue();
			double impr = getValue(d.getField("improvement").stringValue(), metric);
			if(maxStep < step) maxStep = step;
			if(maxImpr < impr) maxImpr = impr;
			if(dsMap.containsKey(termNum)){
				dsMap.get(termNum).add(step, impr);
			}else{
				DataTable table = new DataTable(Double.class, Double.class);
				//insert a dummy tuple to show improvement between baseline run and following iterations
				table.add(0.0, 0.0);
				table.add(step, impr);
				dsMap.put(termNum, table);
			}
		}
		
	    visualize(dsMap,
	    		metric.toUpperCase() + " Improvement(no hashtags, feedback tweets number=" + docNum + ")", 
	    		" Terms",
	    		"Weight Decreasing Step",
	    		maxStep + 0.1,
	    		metric.toUpperCase() + " Improvement",
	    		maxImpr + 0.01);
	    
	    recDir.close();
	    searcher.getIndexReader().close();
	}
	
	private static void visualize(Map<?, DataTable> data, String title, String legend, String labelX, double maxX, String labelY, double maxY){
		DataSeries[] dsList = new DataSeries[data.keySet().size()];
		int i = 0;
		for(Object dn : data.keySet()){
			DataTable table = data.get(dn);
			table.sort(new Ascending(0));
			DataSeries ds = new DataSeries(String.valueOf(dn) + legend, table, 0, 1);
			dsList[i ++] = ds;
		}
		
		XYPlot plot = new XYPlot(dsList);
		plot.setLegendVisible(true);
		plot.setLegendLocation(Location.EAST);
		plot.setLegendDistance(0.4);
	    plot.setInsets(new Insets2D.Double(TOP, LEFT, BOTTOM, RIGHT));
	    plot.getTitle().setText(title);
	    plot.getAxis(XYPlot.AXIS_X).setMin(0);
	    plot.getAxis(XYPlot.AXIS_X).setMax(maxX);
	    plot.getAxis(XYPlot.AXIS_Y).setMin(0);
	    plot.getAxis(XYPlot.AXIS_Y).setMax(maxY);
	    plot.getAxisRenderer(XYPlot.AXIS_X).setIntersection(-Double.MAX_VALUE);
	    plot.getAxisRenderer(XYPlot.AXIS_X).setLabel(labelX);
	    plot.getAxisRenderer(XYPlot.AXIS_Y).setIntersection(-Double.MAX_VALUE);
	    plot.getAxisRenderer(XYPlot.AXIS_Y).setLabel(labelY);
	    plot.getAxisRenderer(XYPlot.AXIS_Y).setLabelDistance(Y_LABEL_DIST);
	    for(i = 0; i < dsList.length; i ++){
	    	DataSeries ds = dsList[i];
	    	plot.setLineRenderer(ds, new DefaultLineRenderer2D());
	    	
	    	float a = (float)Math.random(), 
	    		  b = (float)Math.random(), 
	    		  c = (float)Math.random();
	    	Color color = new Color(a, b, c);
	    	plot.getPointRenderer(ds).setColor(color);
	    	plot.getLineRenderer(ds).setColor(color);
	    }
	    
		JDialog dialog = new JDialog();
		dialog.setTitle(title);
		dialog.setSize(HEIGHT, WIDTH);
		dialog.setResizable(true);
		
		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout());
		dialog.setContentPane(contentPane);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		contentPane.add(new InteractivePanel(plot), BorderLayout.CENTER);
		dialog.setVisible(true);		
	}

	private static double getValue(String mapstr, String metric){
		Map<String, Double> imprs = new HashMap<String, Double>();
		String[] pairs = mapstr.toLowerCase().replaceAll("[{} ]", "").replaceAll("_", "@").split(",");
		for(String p : pairs){
			String[] kv = p.split("=");
			imprs.put(kv[0], Double.parseDouble(kv[1]));
		}
		if(!imprs.containsKey(metric)){
			System.err.println(metric + " not found.");
			System.exit(1);
		}
		return imprs.get(metric);
	}
}
