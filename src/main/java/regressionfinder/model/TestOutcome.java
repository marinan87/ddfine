package regressionfinder.model;

public enum TestOutcome {

	PASS(1), FAIL(-1), UNRESOLVED(2);
	
	int numCode;
	
	TestOutcome(int numCode) {
		this.numCode = numCode;
	}
	
	public static TestOutcome fromNumericCode(int numCode) {
		for (TestOutcome outcome : TestOutcome.values()) {
			if (outcome.numCode == numCode) {
				return outcome;
			}
		}
		throw new IllegalArgumentException("Unknown test outcome numeric code.");
	}
}
