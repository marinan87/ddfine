package simple;

public final class Example {
	
	private final double classVariable = 0;
	
	public int multiplyByEleven(int param) {
		return 11 * param;
	}
	
	final int multiplyByTen(int param) {
		// failure-inducing change
		int factor = 12;
		return param * factor;
	}
	
	void doSomething() {
		int a = 5;
		a += 10;
		a *= 3;
		a++;
		a -= 2;
	}
	
	public void increasingAccessibilityReplacePrivateWithPublicExample() {
		
	}
	
	public void increasingAccessibilityReplaceProtectedWithPublicExample() {
		
	}
	
	private void decreasingAccessibilityInsertPrivateExample() {
		
	}
	
	private void decreasingAccessibilityReplaceProtectedWithPrivateExample() {
		
	}
	
	private void decreasingAccessibilityReplacePublicWithPrivateExample() {
		
	}
}
