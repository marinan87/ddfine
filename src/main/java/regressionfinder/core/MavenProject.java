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
import java.util.stream.Stream;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.FileUtils;

import com.google.common.base.Preconditions;

public class MavenProject {
	private static final String SOURCES_DIR = "src";
	private static final String TARGET_DIR = "target";
	private static final String CLASSES_DIR = "classes";
	private static final String TEST_CLASSES_DIR = "test-classes";

	private static final String POM_XML = "pom.xml";
	private static final String PHASE_COMPILE = "compile";
	private static final String PHASE_TEST_COMPILE = "test-compile";
	private static final String THREADS_1C = "1C";
	private static final String MAVEN_OPTS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1";
	private static final String MAVEN_HOME = System.getenv("MAVEN_HOME");

	private final String rootDirectory;
	private final File rootPomXml;
	private final Path sourcesDirectoryPath, targetClassesPath, targetTestClassesPath;

	public MavenProject(String rootDirectory) {
		this.rootDirectory = rootDirectory;
		this.rootPomXml = Paths.get(rootDirectory, POM_XML).toFile();

		this.sourcesDirectoryPath = Paths.get(rootDirectory, SOURCES_DIR);
		Path targetDirectoryPath = Paths.get(rootDirectory, TARGET_DIR);
		this.targetClassesPath = targetDirectoryPath.resolve(CLASSES_DIR);
		this.targetTestClassesPath = targetDirectoryPath.resolve(TEST_CLASSES_DIR);

		checkIsMavenProject();
	}

	private void checkIsMavenProject() {
		String errorMessageTemplate = String.format("Folder %s is not a root of Maven project! ", rootDirectory)
				.concat("%s");
		Preconditions.checkState(rootPomXml.isFile(), errorMessageTemplate, "Root pom.xml file is missing.");
		Preconditions.checkState(sourcesDirectoryPath.toFile().isDirectory(), errorMessageTemplate, "Sources directory is missing.");
		Preconditions.checkState(targetClassesPath.toFile().isDirectory(), errorMessageTemplate,
				"Target classes directory is missing.");
		Preconditions.checkState(targetTestClassesPath.toFile().isDirectory(), errorMessageTemplate,
				"Target test-classes directory is missing.");
	}

	public void triggerCompilationWithTests() {
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

		Invoker invoker = new DefaultInvoker();
		invoker.setMavenHome(new File(MAVEN_HOME));
		try {
			invoker.execute(request);
		} catch (MavenInvocationException e) {
			throw new RuntimeException(e);
		}
		// TODO: run incremental build
		// TODO: in-memory compilation for speed up?
	}

	public Stream<URL> getClassPaths() {
		return Stream.of(pathToURL(targetClassesPath), pathToURL(targetTestClassesPath));
	}

	private URL pathToURL(Path path) {
		try {
			return new URL("file:/".concat(path.toString().replace("\\", "/")).concat("/"));
		} catch (MalformedURLException e) {
			throw new RuntimeException(String.format("Error while converting path %s to URL", path.toString()));
		}
	}

	public Path copyFromAnotherProject(MavenProject sourceProject, Path relativePath) throws IOException {
		return Files.copy(sourceProject.findAbsolutePath(relativePath),
				findAbsolutePath(relativePath),
				StandardCopyOption.REPLACE_EXISTING);
	}
	
	public File findFile(Path relativePath) {
		return findAbsolutePath(relativePath).toFile();
	}
	
	public Path findAbsolutePath(Path relativePath) {
		return sourcesDirectoryPath.resolve(relativePath);
	}
	
	public Path findRelativeToSourceRoot(Path absolutePath) {
		return sourcesDirectoryPath.relativize(absolutePath);
	}

	public void copyEverythingTo(Path targetPath) throws IOException {
		FileUtils.copyDirectoryStructure(Paths.get(rootDirectory).toFile(), targetPath.toFile());
	}

	public File getSourceDirectory() {
		return sourcesDirectoryPath.toFile();
	}
}
