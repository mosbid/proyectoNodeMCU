package hwvertx;

public class PacoState {

	private int id;
	private String name;
	private boolean state;
	private float level;

	public PacoState() {
		super();
		id = 0;
		name = "";
		state = false;
		level = 0f;
	}

	public PacoState(int id, String name, boolean state, float level) {
		super();
		this.id = id;
		this.name = name;
		this.state = state;
		this.level = level;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isState() {
		return state;
	}

	public void setState(boolean state) {
		this.state = state;
	}

	public float getLevel() {
		return level;
	}

	public void setLevel(float level) {
		this.level = level;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + Float.floatToIntBits(level);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (state ? 1231 : 1237);
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
		PacoState other = (PacoState) obj;
		if (id != other.id)
			return false;
		if (Float.floatToIntBits(level) != Float.floatToIntBits(other.level))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (state != other.state)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PacoState [id=" + id + ", name=" + name + ", state=" + state + ", level=" + level + "]";
	}

}
