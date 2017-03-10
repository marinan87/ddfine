package simple;

public class Example {

	int multiplyByTen(int param) {
		// failure-inducing change
		int factor = 12;
		return param * factor;
	}
	
	int multiplyByEleven(int param) {
		return 11 * param;
	}
	
	void doSomething() {
		int a = 5;
		a += 10;
		a *= 3;
		a++;
		a -= 2;
	}
}
