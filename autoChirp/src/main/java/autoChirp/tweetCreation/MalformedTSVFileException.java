package autoChirp.tweetCreation;

public class MalformedTSVFileException extends Throwable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int row;
	private int column;
	private String content;
	private String message;
	
	public MalformedTSVFileException(int row, int column, String content, String message){
		this.row = row;
		this.column = column;
		this.content = content;
		this.message = message;
	}
	
	public int getRow() {
		return row;
	}
	public int getColumn() {
		return column;
	}
	public String getContent() {
		return content;
	}
	
	@Override
	public String getMessage(){
		return message;
	}
	
}
