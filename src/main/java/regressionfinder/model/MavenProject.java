package regressionfinder.model;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

public abstract class MavenProject {
	
	private static final String PHASE_COMPILE = "compile";
	private static final String PHASE_TEST_COMPILE = "test-compile";
	private static final String GOAL_BUILD_CLASSPATH = "dependency:build-classpath";
	private static final String THREADS_1C = "1C";
	private static final String MAVEN_OPTS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1";
	private static final String MAVEN_HOME = System.getenv("MAVEN_HOME");	
	private static final String SETTINGS_FILE = "C:\\Users\\X\\.m2\\settings_rl.xml";
	private static final String POM_XML = "pom.xml";
	private static final String JARS_SEPARATOR = ";";

	protected final Path rootDirectory;
	protected final File rootPomXml;


	protected MavenProject(Path rootDirectory) {
		this.rootDirectory = rootDirectory;
		this.rootPomXml = rootDirectory.resolve(POM_XML).toFile();
	}
	
	public void triggerCompilationWithTestClasses() {
		triggerCompilation(PHASE_TEST_COMPILE);
	}

	public void triggerSimpleCompilation() {
		triggerCompilation(PHASE_COMPILE);
	}

	private void triggerCompilation(String phase) {
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(rootPomXml);
		request.setGoals(Arrays.asList(phase));
		request.setThreads(THREADS_1C);
		request.setMavenOpts(MAVEN_OPTS);
		request.setOffline(true);
		request.setGlobalSettingsFile(new File(SETTINGS_FILE));

		Invoker invoker = new DefaultInvoker();
		invoker.setMavenHome(new File(MAVEN_HOME));
		try {
			invoker.execute(request);
		} catch (MavenInvocationException e) {
			throw new RuntimeException(e);
		}
		// TODO: compile with dependent projects (mvn option alsomake)
		// TODO: run incremental build
		// TODO: in-memory compilation for speed up? RAM disk?
	}
	
	public Stream<URL> getLocalMavenDependencies() {
		Set<URL> results = new HashSet<>();

		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(rootPomXml);
		request.setGoals(Arrays.asList(GOAL_BUILD_CLASSPATH));
		request.setOffline(true);
		request.setGlobalSettingsFile(new File(SETTINGS_FILE));
		request.setOutputHandler(line -> {
			if (notInformationalLine(line)) {
				results.addAll(Stream.of(line.split(JARS_SEPARATOR)).map(path -> stringToURL(path, false)).collect(Collectors.toSet()));
			}
		});

		Invoker invoker = new DefaultInvoker();
		invoker.setMavenHome(new File(MAVEN_HOME));
		try {
			invoker.execute(request);
		} catch (MavenInvocationException e) {
			throw new RuntimeException(e);
		}
		
		return results.stream();
	}

	private boolean notInformationalLine(String line) {
		return !line.startsWith("[");
	}

	protected URL stringToURL(String path, boolean isDirectory) {
		try {
			String urlPath = "file:/".concat(path.replace("\\", "/"));
			if (isDirectory) {
				urlPath = urlPath.concat("/");
			}
			return new URL(urlPath);
		} catch (MalformedURLException e) {
			throw new RuntimeException(String.format("Error while converting path %s to URL", path.toString()));
		}
	}
}
