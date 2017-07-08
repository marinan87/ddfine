package simple;

public class Example {
	
	private double classVariable = 0;
	
	int multiplyByEleven(int param) {
		return param * 11;
	}
	
	int multiplyByTen(int param) {
		
int factor = 12;
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
	
	private void increasingAccessibilityReplacePrivateWithPublicExample() {
		
	}
	
	protected void increasingAccessibilityReplaceProtectedWithPublicExample() {
		
	}
	
	void increasingAccessibilityInsertProtectedExample() {
		
	}
	
	void decreasingAccessibilityInsertPrivateExample() {
		
	}
	
	protected void decreasingAccessibilityReplaceProtectedWithPrivateExample() {
		
	}
	
	public void decreasingAccessibilityReplacePublicWithPrivateExample() {
		
	}
	
	void parameterAddingExample(int param) {
		
	}
	
	public void methodToCheckParameterRenaming(Object aRenamePara) {}
	
	public void methodToCheckParameterTypeChange(Object first, int typeChange) {}
}
