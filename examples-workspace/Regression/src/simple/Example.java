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
	
	int multiplyByTwelve(int param) {
		return 12 * param;
	}
}
