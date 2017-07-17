package regressionfinder.core;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class MavenProject {

	private static final String SOURCES_DIR = "src";
	private static final String TARGET_DIR = "target";
	// TODO: make more general
	private static final String PATH_TO_PACKAGE = "simple";
	private static final String CLASSES_DIR = "classes";
	private static final String TEST_CLASSES_DIR = "test-classes";
	
	private static final String POM_XML = "pom.xml";
	private static final String PHASE_COMPILE = "compile";
	private static final String THREADS_1C = "1C";
	private static final String MAVEN_OPTS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1";
	private static final String MAVEN_HOME = System.getenv("MAVEN_HOME");
		
	private final String rootDirectory;
	private final File rootPomXml;
	private final Path targetClassesPath, targetTestClassesPath;
	
	public MavenProject(String rootDirectory) {
		this.rootDirectory = rootDirectory;
		this.rootPomXml = Paths.get(rootDirectory, POM_XML).toFile();
		
		File targetDirectory = Paths.get(rootDirectory, TARGET_DIR).toFile();
		this.targetClassesPath = targetDirectory.toPath().resolve(CLASSES_DIR);
		this.targetTestClassesPath = targetDirectory.toPath().resolve(TEST_CLASSES_DIR);
		
		checkIsMavenProject();
	}
	
	private void checkIsMavenProject() {
		String errorMessageTemplate = String.format("Folder %s is not a root of Maven project! ", rootDirectory).concat("%s");
		Preconditions.checkState(rootPomXml.isFile(), errorMessageTemplate, "Root pom.xml file is missing.");
		Preconditions.checkState(Paths.get(rootDirectory, SOURCES_DIR).toFile().isDirectory(), errorMessageTemplate, "Source directory is missing.");
		Preconditions.checkState(targetClassesPath.toFile().isDirectory(), errorMessageTemplate, "Target classes directory is missing.");
		Preconditions.checkState(targetTestClassesPath.toFile().isDirectory(), errorMessageTemplate, "Target test-classes directory is missing.");
	}
	
	public void triggerCompilation() {
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(rootPomXml);
		request.setGoals(Arrays.asList(PHASE_COMPILE));
		request.setThreads(THREADS_1C);
		request.setMavenOpts(MAVEN_OPTS);

		Invoker invoker = new DefaultInvoker();
		invoker.setMavenHome(new File(MAVEN_HOME));
		try {
			invoker.execute(request);
		} catch (MavenInvocationException e) {
			throw new RuntimeException(e);
		}
		// TODO: run incremental build
	}
	
	public List<URL> getClassPaths() {
		return Lists.newArrayList(pathToURL(targetClassesPath), pathToURL(targetTestClassesPath));
	}
	
	private URL pathToURL(Path path) {
		try {
			return new URL("file:/".concat(path.toString().replace("\\", "/")).concat("/"));
		} catch (MalformedURLException e) {
			throw new RuntimeException(String.format("Error while converting path %s to URL", path.toString()));
		}
	}
	
	public Path copyHere(String sourceFile) throws IOException {
		Path source = Paths.get(sourceFile);
		Path copy = Paths.get(rootDirectory);
		return Files.copy(source, 
				copy.resolve(Paths.get(SOURCES_DIR, PATH_TO_PACKAGE, source.getFileName().toString())), 
				StandardCopyOption.REPLACE_EXISTING);
	}
}
