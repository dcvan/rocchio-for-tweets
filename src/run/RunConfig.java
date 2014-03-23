package run;

import org.apache.lucene.analysis.Analyzer;

public class RunConfig {
	private int docNum;
	private int termNum;
	private Analyzer analyzer;
	private double step;
	private boolean withTerms;
	private boolean withHashtags;
	private String[] metrics;
	private String note;

	public RunConfig(int docNum, int termNum, Analyzer analyzer, double step, boolean withTerms,
			boolean withHashtags, String[] metrics, String note) {
		super();
		this.docNum = docNum;
		this.termNum = termNum;
		this.analyzer = analyzer;
		this.step = step;
		this.withTerms = withTerms;
		this.withHashtags = withHashtags;
		this.metrics = metrics;
		this.note = note;
	}
	public int getDocNum() {
		return docNum;
	}
	public void setDocNum(int docNum) {
		this.docNum = docNum;
	}
	public int getTermNum() {
		return termNum;
	}
	public void setTermNum(int termNum) {
		this.termNum = termNum;
	}
	public Analyzer getAnalyzer(){
		return analyzer;
	}
	public void setAnalyzer(Analyzer analyzer){
		this.analyzer = analyzer;
	}
	public double getStep() {
		return step;
	}
	public void setStep(double step) {
		this.step = step;
	}
	public boolean isWithTerms() {
		return withTerms;
	}
	public void setWithTerms(boolean withTerms) {
		this.withTerms = withTerms;
	}
	public boolean isWithHashtags() {
		return withHashtags;
	}
	public void setWithHashtags(boolean withHashtags) {
		this.withHashtags = withHashtags;
	}
	public String[] getMetrics() {
		return metrics;
	}
	public void setMetrics(String[] metrics) {
		this.metrics = metrics;
	}
	public String getNote(){
		return note;
	}
	public void setNote(String note){
		this.note = note;
	}
}
