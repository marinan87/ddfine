package regressionfinder.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import regressionfinder.model.MavenProject;

@Service
public class MavenCompiler {

	private static final String PHASE_COMPILE = "compile";
	private static final String PHASE_TEST_COMPILE = "test-compile";
	private static final String GOAL_BUILD_CLASSPATH = "dependency:build-classpath";
	private static final String THREADS_1C = "1C";
	private static final String MAVEN_OPTS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1";
	private static final String MAVEN_HOME = System.getenv("MAVEN_HOME");	
	private static final String JARS_SEPARATOR = ";";
	
	@Value("#{new java.io.File('${maven.settings}')}")
	private File mavenSettings;
	
	
	public void triggerCompilationWithTestClasses(MavenProject project) {
		triggerCompilation(project, PHASE_TEST_COMPILE);
	}

	public void triggerSimpleCompilation(MavenProject project) {
		triggerCompilation(project, PHASE_COMPILE);
	}

	private void triggerCompilation(MavenProject project, String phase) {
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(project.getRootPomXml());
		request.setGoals(Arrays.asList(phase));
		request.setThreads(THREADS_1C);
		request.setMavenOpts(MAVEN_OPTS);
		request.setOffline(true);
		request.setGlobalSettingsFile(mavenSettings);

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

	public Stream<URL> getLocalMavenDependencies(MavenProject project) {
		Set<URL> results = new HashSet<>();

		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(project.getRootPomXml());
		request.setGoals(Arrays.asList(GOAL_BUILD_CLASSPATH));
		request.setOffline(true);
		request.setThreads(THREADS_1C);
		request.setGlobalSettingsFile(mavenSettings);
		request.setOutputHandler(line -> {
			if (notInformationalLine(line)) {
				results.addAll(Stream.of(line.split(JARS_SEPARATOR)).map(this::getJarURL).collect(Collectors.toSet()));
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
	
	private URL getJarURL(String path) {
		try {
			return new URL("file:/".concat(path.replace("\\", "/")));
		} catch (MalformedURLException e) {
			throw new RuntimeException(String.format("Error while converting JAR path %s to URL", path.toString()));
		}
	}
}
