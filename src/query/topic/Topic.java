package query.topic;
/**
 * Topic POJO
 * 
 * @author dc
 *
 */
public class Topic {
	private int topNo;
	private String title;
	private long queryTime;
	private long queryTweetTime;
	
	public Topic(){}
	
	/**
	 * 
	 * @param id - top num
	 * @param t - title
	 * @param qt - query time
	 * @param qtt - query tweet time
	 */
	public Topic(int id, String t, long qt, long qtt){
		topNo = id;
		title = t;
		queryTime = qt;
		queryTweetTime = qtt;
	}

	public int getTopNo() {
		return topNo;
	}

	public void setTopNo(int topNo) {
		this.topNo = topNo;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public long getQueryTime() {
		return queryTime;
	}

	public void setQueryTime(long queryTime) {
		this.queryTime = queryTime;
	}

	public long getQueryTweetTime() {
		return queryTweetTime;
	}

	public void setQueryTweetTime(long queryTweetTime) {
		this.queryTweetTime = queryTweetTime;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("topic num: ").append(getTopNo()).append('\n')
			.append("title: ").append(getTitle()).append('\n')
			.append("query time: ").append(getQueryTime()).append('\n')
			.append("query tweet time: ").append(getQueryTweetTime()).append('\n');
		return sb.toString();
	}
}
