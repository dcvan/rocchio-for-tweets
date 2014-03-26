package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

	private static class plotConfig{
		String title;
		double maxX;
		double minX;
		String labelX;
		double maxY;
		double minY;
		String labelY;
		
		plotConfig(String title, double maxX, double minX, String labelX, double maxY, double minY, String labelY){
			this.title = title;
			this.maxX = maxX;
			this.minX = minX;
			this.labelX = labelX;
			this.maxY = maxY;
			this.minY = minY;
			this.labelY = labelY;
		}
	}

	private final static int WIDTH = 600;
	private final static int HEIGHT = 700;
	private final static double TOP = 30.0;
	private final static double LEFT = 75.0;
	private final static double BOTTOM = 70.0;
	private final static double RIGHT = 160.0;
	private final static double Y_LABEL_DIST = 2.0;
	private final static String REC_BASE = System.getProperty("user.home") + "/Documents/run";
	private final static String CAT_WITH_HTAG = "Hashtag";
	private final static String CAT_WITH_TERM = "Term";
	private final static String CAT_WITH_BOTH = "Term + Hashtag";
	
	public static void main(String[] args) 
			throws IOException {

		plotIterations("map");
//		plotStep(0.1, 1.0, 5, 5, "ndcg");
//		plotDocNumTermNum(100, 500, 5, 25, 0.1, "p@30");
//		plotTermNum(5, 25, 25, 0.1, "ndcg");
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
		DataSeries data = new DataSeries("", table, 0, 1);
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
	
	//only with terms and single iteration by default 
	@SuppressWarnings("unchecked")
	public static void plotDocNum(int start, int end, int termNum, double step, String metric) 
			throws IOException{
		int maxDocNum = 0;
		double maxImpr = 0;
		double minImpr = 0;
		
		Directory recDir = FSDirectory.open(new File(REC_BASE));
		IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(recDir));
		Query stepQuery = NumericRangeQuery.newDoubleRange("step", step, step, true, true);
		Query docQuery = NumericRangeQuery.newIntRange("document number", start, end, true, true);
		Query termQuery = NumericRangeQuery.newIntRange("term number", termNum, termNum, true, true);
		Query runTypeQuery = new TermQuery(new Term("run type", "single iteration"));
		
		BooleanQuery query = new BooleanQuery();
		query.add(stepQuery, BooleanClause.Occur.MUST);
		query.add(docQuery, BooleanClause.Occur.MUST);
		query.add(termQuery, BooleanClause.Occur.MUST);
		query.add(runTypeQuery,BooleanClause.Occur.MUST);
		
		TopDocs hits = searcher.search(query, Integer.MAX_VALUE);
		Map<String, DataTable> dsMap = new HashMap<String, DataTable>();
		for(ScoreDoc sd : hits.scoreDocs){
			Document d = searcher.doc(sd.doc);
			int docNum = d.getField("document number").numericValue().intValue();
			boolean isWithHtag = Boolean.parseBoolean(d.getField("with hashtags").stringValue());
			boolean isWithTerm = Boolean.parseBoolean(d.getField("with selected terms").stringValue());
			String category;
			if(isWithTerm && isWithHtag)
				category = CAT_WITH_BOTH;
			else if(isWithTerm)
				category = CAT_WITH_TERM;
			else
				category = CAT_WITH_HTAG;
			double impr = getValue(d.getField("improvement").stringValue(), metric);
			if(maxDocNum < docNum) maxDocNum = docNum;
			if(maxImpr < impr) maxImpr = impr;
			if(minImpr > impr) minImpr = impr;
			
			if(dsMap.containsKey(category)){
				dsMap.get(category).add(docNum, impr);
			}else{
				DataTable table = new DataTable(Integer.class, Double.class);
				//insert a dummy tuple to show improvement between baseline run and following iterations
				table.add(0, 0.0);
				table.add(docNum, impr);
				dsMap.put(category, table);
			}
		}
		
	    visualize(dsMap, new plotConfig(
	    		metric.toUpperCase() + " Improvement(term#=" + termNum + ", step=" + step + ")", 
	    		maxDocNum + 1.0, 
	    		0,
	    		"Number of Feedback Tweets",
	    		maxImpr + 0.01,
	    		minImpr,
	    		metric.toUpperCase() + " Improvement"));
	    
	    recDir.close();
	    searcher.getIndexReader().close();
	}
	
	//only with terms and single iteration by default 
	@SuppressWarnings("unchecked")
	public static void plotTermNum(int start, int end, int docNum, double step, String metric) 
			throws IOException{
		int maxTermNum = 0;
		double maxImpr = 0;
		double minImpr = 0;
		
		Directory recDir = FSDirectory.open(new File(REC_BASE));
		IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(recDir));
		Query stepQuery = NumericRangeQuery.newDoubleRange("step", step, step, true, true);
		Query docQuery = NumericRangeQuery.newIntRange("document number", docNum, docNum, true, true);
		Query termQuery = NumericRangeQuery.newIntRange("term number", start, end, true, true);
		Query runTypeQuery = new TermQuery(new Term("run type", "single iteration"));
		
		BooleanQuery query = new BooleanQuery();
		query.add(stepQuery, BooleanClause.Occur.MUST);
		query.add(docQuery, BooleanClause.Occur.MUST);
		query.add(termQuery, BooleanClause.Occur.MUST);
		query.add(runTypeQuery,BooleanClause.Occur.MUST);
		
		TopDocs hits = searcher.search(query, Integer.MAX_VALUE);
		Map<String, DataTable> dsMap = new HashMap<String, DataTable>();
		for(ScoreDoc sd : hits.scoreDocs){
			Document d = searcher.doc(sd.doc);
			int termNum = d.getField("term number").numericValue().intValue();
			boolean isWithHtag = Boolean.parseBoolean(d.getField("with hashtags").stringValue());
			boolean isWithTerm = Boolean.parseBoolean(d.getField("with selected terms").stringValue());
			String category;
			if(isWithTerm && isWithHtag)
				category = CAT_WITH_BOTH;
			else if(isWithTerm)
				category = CAT_WITH_TERM;
			else
				category = CAT_WITH_HTAG;
			double impr = getValue(d.getField("improvement").stringValue(), metric);
			if(maxTermNum < termNum) maxTermNum = termNum;
			if(maxImpr < impr) maxImpr = impr;
			if(minImpr > impr) minImpr = impr;
			if(dsMap.containsKey(category)){
				dsMap.get(category).add(termNum, impr);
			}else{
				DataTable table = new DataTable(Integer.class, Double.class);
				//insert a dummy tuple to show improvement between baseline run and following iterations
				table.add(0, 0.0);
				table.add(termNum, impr);
				dsMap.put(category, table);
			}
		}
		
	    visualize(dsMap, new plotConfig(
	    	    		metric.toUpperCase() + " Improvement(tweet#=" + docNum + ", step=" + step + ")", 
	    	    		maxTermNum + 1.0, 
	    	    		0,
	    	    		"Number of Feedback Terms",
	    	    		maxImpr + 0.01,
	    	    		minImpr,
	    	    		metric.toUpperCase() + " Improvement"));
	    
	    recDir.close();
	    searcher.getIndexReader().close();
	}
	
	//only with terms and single iteration by default 
	@SuppressWarnings("unchecked")
	public static void plotStep(double start, double end, int docNum, int termNum, String metric) 
			throws IOException{
		double maxStep = 0;
		double maxImpr = 0;
		double minImpr = 0;
		
		Directory recDir = FSDirectory.open(new File(REC_BASE));
		IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(recDir));
		Query stepQuery = NumericRangeQuery.newDoubleRange("step", start, end, true, true);
		Query docQuery = NumericRangeQuery.newIntRange("document number", docNum, docNum, true, true);
		Query termQuery = NumericRangeQuery.newIntRange("term number", termNum, termNum, true, true);
		Query runTypeQuery = new TermQuery(new Term("run type", "single iteration"));
		
		BooleanQuery query = new BooleanQuery();
		query.add(stepQuery, BooleanClause.Occur.MUST);
		query.add(docQuery, BooleanClause.Occur.MUST);
		query.add(termQuery, BooleanClause.Occur.MUST);
		query.add(runTypeQuery,BooleanClause.Occur.MUST);
		
		TopDocs hits = searcher.search(query, Integer.MAX_VALUE);
		Map<String, DataTable> dsMap = new HashMap<String, DataTable>();
		for(ScoreDoc sd : hits.scoreDocs){
			Document d = searcher.doc(sd.doc);
			double step = d.getField("step").numericValue().doubleValue();
			boolean isWithHtag = Boolean.parseBoolean(d.getField("with hashtags").stringValue());
			boolean isWithTerm = Boolean.parseBoolean(d.getField("with selected terms").stringValue());
			String category;
			if(isWithTerm && isWithHtag)
				category = CAT_WITH_BOTH;
			else if(isWithTerm)
				category = CAT_WITH_TERM;
			else
				category = CAT_WITH_HTAG;
			double impr = getValue(d.getField("improvement").stringValue(), metric);
			if(maxStep < step) maxStep = step;
			if(maxImpr < impr) maxImpr = impr;
			if(minImpr > impr) minImpr = impr;
			if(dsMap.containsKey(category)){
				dsMap.get(category).add(step, impr);
			}else{
				DataTable table = new DataTable(Double.class, Double.class);
				//insert a dummy tuple to show improvement between baseline run and following iterations
				table.add(0.0, 0.0);
				table.add(step, impr);
				dsMap.put(category, table);
			}
		}
		
	    visualize(dsMap, new plotConfig(
	    		metric.toUpperCase() + " Improvement(term#=" + termNum + ", tweet#=" + docNum +")", 
	    		maxStep + 0.01, 
	    		0,
	    		"Weight Decreasing Step",
	    		maxImpr + 0.01,
	    		minImpr,
	    		metric.toUpperCase() + " Improvement"));
	    
	    recDir.close();
	    searcher.getIndexReader().close();
	}
	
	
	private static void visualize(Map<String, DataTable> data, plotConfig config){
		DataSeries[] dsList = new DataSeries[data.keySet().size()];
		int i = 0;
		for(String cat : data.keySet()){
			DataTable table = data.get(cat);
			table.sort(new Ascending(0));
			DataSeries ds = new DataSeries(cat, table, 0, 1);
			dsList[i ++] = ds;
		}
		
		XYPlot plot = new XYPlot(dsList);
		plot.setLegendVisible(true);
		plot.setLegendLocation(Location.EAST);
		plot.setLegendDistance(0.4);
	    plot.setInsets(new Insets2D.Double(TOP, LEFT, BOTTOM, RIGHT));
	    plot.getTitle().setText(config.title);
	    plot.getAxis(XYPlot.AXIS_X).setMin(config.minX);
	    plot.getAxis(XYPlot.AXIS_X).setMax(config.maxX);
	    plot.getAxisRenderer(XYPlot.AXIS_X).setLabel(config.labelX);
	    if(config.minX > 0)
	    	plot.getAxisRenderer(XYPlot.AXIS_X).setIntersection(-Double.MAX_VALUE);
	    plot.getAxis(XYPlot.AXIS_Y).setMin(config.minY);
	    plot.getAxis(XYPlot.AXIS_Y).setMax(config.maxY);
	    if(config.minY > 0)
	    	plot.getAxisRenderer(XYPlot.AXIS_Y).setIntersection(-Double.MAX_VALUE);
	    plot.getAxisRenderer(XYPlot.AXIS_Y).setLabel(config.labelY);
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
		dialog.setTitle(config.title);
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
