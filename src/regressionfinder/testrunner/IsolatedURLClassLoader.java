package regressionfinder.testrunner;

import java.net.URL;
import java.net.URLClassLoader;

/*
 * Workaround for "No runnable methods" exception when running JUnit tests using JunitCore.run
 * See here: http://stackoverflow.com/questions/24319697/java-lang-exception-no-runnable-methods-exception-in-running-junits/24319836
 */
public class IsolatedURLClassLoader extends URLClassLoader {
	public IsolatedURLClassLoader(URL[] urls) {
		super(urls, null);
	}
}
