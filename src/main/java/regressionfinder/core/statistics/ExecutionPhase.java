package regressionfinder.core.statistics;

public enum ExecutionPhase {

	PREPARATION("Preparation phase"), CHANGE_DISTILLING("Change distilling phase"), DELTA_DEBUGGING("Delta debugging phase");
	
	
	private final String displayName;
	
	
	ExecutionPhase(String displayName) {
		this.displayName = displayName;
	}
	
	public String displayName() {
		return displayName;
	}
}
