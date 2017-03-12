package simple;

public class Example {
	
	private double classVariable = 0;
	
	int multiplyByEleven(int param) {
		return param * 11;
	}
	
	int multiplyByTen(int param) {
		return param * 10;
	}
	
	int multiplyByTwelve(int param) {
		return param * 12;
	}
	
	void doSomething() {
		int a = 5;
		a += 10;
		a -= 2;
	}
	
	public void increasingAccessibilityReplacePrivateWithPublicExample() {
		
	}
	
	protected void increasingAccessibilityReplaceProtectedWithPublicExample() {
		
	}
	
	void decreasingAccessibilityInsertPrivateExample() {
		
	}
	
	protected void decreasingAccessibilityReplaceProtectedWithPrivateExample() {
		
	}
	
	private void decreasingAccessibilityReplacePublicWithPrivateExample() {
		
	}
}
