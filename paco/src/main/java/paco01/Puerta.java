package paco01;

public class Puerta {

	private int id;
	private int doorState;
	private String doorAddress;
	private String doorPass;
	private String doorAdmin;
	
	
	
	public Puerta(int id, int doorState, String doorAddress, String doorPass, String doorAdmin) {
		super();
		this.id = id;
		this.doorState = doorState;
		this.doorAddress = doorAddress;
		this.doorPass = doorPass;
		this.doorAdmin = doorAdmin;
}
	
	
	public Puerta() {
		super();
		id = 0;
		doorState = 0;
		doorAddress = "";
		doorPass = "";
		doorAdmin = "";
	}


	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public int getDoorState() {
		return doorState;
	}


	public void setDoorState(int doorState) {
		this.doorState = doorState;
	}


	public String getDoorAddress() {
		return doorAddress;
	}


	public void setDoorAddress(String doorAddress) {
		this.doorAddress = doorAddress;
	}


	public String getDoorPass() {
		return doorPass;
	}


	public void setDoorPass(String doorPass) {
		this.doorPass = doorPass;
	}


	public String getDoorAdmin() {
		return doorAdmin;
	}


	public void setDoorAdmin(String doorAdmin) {
		this.doorAdmin = doorAdmin;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((doorAddress == null) ? 0 : doorAddress.hashCode());
		result = prime * result + ((doorAdmin == null) ? 0 : doorAdmin.hashCode());
		result = prime * result + ((doorPass == null) ? 0 : doorPass.hashCode());
		result = prime * result + doorState;
		result = prime * result + id;
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
		Puerta other = (Puerta) obj;
		if (doorAddress == null) {
			if (other.doorAddress != null)
				return false;
		} else if (!doorAddress.equals(other.doorAddress))
			return false;
		if (doorAdmin == null) {
			if (other.doorAdmin != null)
				return false;
		} else if (!doorAdmin.equals(other.doorAdmin))
			return false;
		if (doorPass == null) {
			if (other.doorPass != null)
				return false;
		} else if (!doorPass.equals(other.doorPass))
			return false;
		if (doorState != other.doorState)
			return false;
		if (id != other.id)
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "Puerta [id=" + id + ", doorState=" + doorState + ", doorAddress=" + doorAddress + ", doorPass="
				+ doorPass + ", doorAdmin=" + doorAdmin + "]";
	}


	
	

}
