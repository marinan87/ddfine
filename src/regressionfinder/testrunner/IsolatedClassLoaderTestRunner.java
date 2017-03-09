package regressionfinder.testrunner;

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/*
 * Workaround for "No runnable methods" exception when running JUnit tests using JunitCore.run
 * See here: http://stackoverflow.com/questions/24319697/java-lang-exception-no-runnable-methods-exception-in-running-junits/24319836
 */
public class IsolatedClassLoaderTestRunner {

	public IsolatedClassLoaderTestRunner() {
		ensureLoadedInIsolatedClassLoader(this);
	}

	public int run_invokedReflectively(List<String> testClasses) throws ClassNotFoundException {
		ensureLoadedInIsolatedClassLoader(this);

		List<Class<?>> classes = new ArrayList<>();
		for (String testClass : testClasses) {
			classes.add(Class.forName(testClass));
		}

		Computer computer = new Computer();
		JUnitCore junit = new JUnitCore();
		ensureLoadedInIsolatedClassLoader(junit);

		Result result = junit.run(computer, classes.toArray(new Class[0]));
		boolean hasUnresolvedCompilationProblem = result.getFailures().stream()
				.map(Failure::getMessage)
				.anyMatch(message -> message.contains("Unresolved compilation problem"));
		if (hasUnresolvedCompilationProblem) {
			throw new IllegalStateException();
		}

		return result.getFailureCount();
	}

	private void ensureLoadedInIsolatedClassLoader(Object o) {
		String objectClassLoader = o.getClass().getClassLoader().getClass().getName();
		if (!objectClassLoader.equals(IsolatedURLClassLoader.class.getName())) {
			throw new IllegalStateException(
					String.format("Instance of %s not loaded by a IsolatedURLClassLoader (loaded by %s)", 
							o.getClass(), objectClassLoader));
		}
	}
}