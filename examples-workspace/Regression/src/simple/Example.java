package simple;

import java.io.Serializable;

public final class Example {
	
	private final double classVariable = 0;
	private double newClassVariable;
	
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
	
	protected void increasingAccessibilityInsertProtectedExample() {
		
	}
	
	private void decreasingAccessibilityInsertPrivateExample() {
		
	}
	
	private void decreasingAccessibilityReplaceProtectedWithPrivateExample() {
		
	}
	
	private void decreasingAccessibilityReplacePublicWithPrivateExample() {
		
	}
	
	void newMethod() {
		
	}
	
	static class NewNestedClass {
		
	}
	

	void parameterAddingExample(int param, int newParam) {
		
	}
	
	public void methodToCheckParameterRenaming(Object aRenameParam) {}
	
	public void methodToCheckParameterTypeChange(Object first, String typeChange) {}
}
