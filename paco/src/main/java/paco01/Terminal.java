package paco01;

public class Terminal {

	private int id;
	private boolean tryState;
	private String tryDate;
	private int tryNumber;
	private boolean tryBlock;
	private int relatedDoor;
	
	
	public Terminal(int id, boolean tryState, String tryDate, int tryNumber, boolean tryBlock, int relatedDoor) {
		super();
		this.id = id;
		this.tryState = tryState;
		this.tryDate = tryDate;
		this.tryNumber = tryNumber;
		this.tryBlock = tryBlock;
		this.relatedDoor = relatedDoor;
	}
	
	
	public Terminal() {
		super();
		id = 0;
		tryState = true;
		tryDate = "";
		tryNumber = 0;
		tryBlock = false;
		relatedDoor = 0;
	}


	
	
	
	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public boolean isTryState() {
		return tryState;
	}


	public void setTryState(boolean tryState) {
		this.tryState = tryState;
	}


	public String getTryDate() {
		return tryDate;
	}


	public void setTryDate(String tryDate) {
		this.tryDate = tryDate;
	}


	public int getTryNumber() {
		return tryNumber;
	}


	public void setTryNumber(int tryNumber) {
		this.tryNumber = tryNumber;
	}


	public boolean isTryBlock() {
		return tryBlock;
	}


	public void setTryBlock(boolean tryBlock) {
		this.tryBlock = tryBlock;
	}


	public int getRelatedDoor() {
		return relatedDoor;
	}


	public void setRelatedDoor(int relatedDoor) {
		this.relatedDoor = relatedDoor;
	}


	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + relatedDoor;
		result = prime * result + (tryBlock ? 1231 : 1237);
		result = prime * result + ((tryDate == null) ? 0 : tryDate.hashCode());
		result = prime * result + tryNumber;
		result = prime * result + (tryState ? 1231 : 1237);
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Terminal other = (Terminal) obj;
		if (id != other.id)
			return false;
		if (relatedDoor != other.relatedDoor)
			return false;
		if (tryBlock != other.tryBlock)
			return false;
		if (tryDate != other.tryDate)
			return false;
		if (tryNumber != other.tryNumber)
			return false;
		if (tryState != other.tryState)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "Terminal [id=" + id + ", tryState=" + tryState + ", tryDate=" + tryDate + ", tryNumber=" + tryNumber
				+ ", tryBlock=" + tryBlock + ", relatedDoor=" + relatedDoor + "]";
	}
	
	
	
	
	
}
