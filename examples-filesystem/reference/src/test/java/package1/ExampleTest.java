package package1;

import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.Assert;
import org.junit.Test;

public class ExampleTest {

	@Test
	public void tenMultipliedByTenIsOneHundred() {
		Example example = new Example();
		Assert.assertThat(example.multiplyByTen(10), equalTo(100));
	}
}
