package regressionfinder.model;

import static java.lang.String.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.FileUtils;
import org.springframework.util.DigestUtils;

public class MavenJavaProject {
	private static final String SOURCES_DIR = "src/main/java";
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

	
	public static MavenJavaProject tryCreateMavenProject(String rootDirectory) {
		MavenJavaProject project = createMavenProject(rootDirectory);
		project.assertIsMavenProject();
		return project;
	}
	
	public static MavenJavaProject createMavenProject(String rootDirectory) {
		return new MavenJavaProject(rootDirectory);
	}
	
	private MavenJavaProject(String rootDirectory) {
		this.rootDirectory = rootDirectory;
		this.rootPomXml = Paths.get(rootDirectory, POM_XML).toFile();

		this.sourcesDirectoryPath = Paths.get(rootDirectory, SOURCES_DIR);
		Path targetDirectoryPath = Paths.get(rootDirectory, TARGET_DIR);
		this.targetClassesPath = targetDirectoryPath.resolve(CLASSES_DIR);
		this.targetTestClassesPath = targetDirectoryPath.resolve(TEST_CLASSES_DIR);
	}
	
	private void assertIsMavenProject() {
		String errorMessageTemplate = format("Folder %s is not a root of Maven project! ", rootDirectory).concat("%s");
		
		String reason;
		if (!(reason = whyIsNotMavenProject()).isEmpty()) {
			throw new IllegalStateException(format(errorMessageTemplate, reason));
		}
	}
		
	private String whyIsNotMavenProject() {		
		if (!rootPomXml.isFile()) {
			return "Root pom.xml file is missing.";
		}
		
		File sourcesDirectory = sourcesDirectoryPath.toFile();
		if (!sourcesDirectory.isDirectory() || sourcesDirectory.list().length == 0) {
			return "Sources directory is missing or empty.";
		}
		
		if (!targetClassesPath.toFile().isDirectory()) {
			return "Target classes directory is missing.";
		}
		
		return StringUtils.EMPTY;
	}
	
	public boolean isMavenProject() {
		return whyIsNotMavenProject().isEmpty();
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
		// TODO: compile with dependent projects (mvn option)
		// TODO: run incremental build
		// TODO: in-memory compilation for speed up? RAM disk?
	}

	public Stream<URL> getClassPaths() {
		Builder<URL> stream = Stream.<URL>builder().add(pathToURL(targetClassesPath));
		
		if (targetTestClassesPath.toFile().isDirectory()) {
			stream.add(pathToURL(targetTestClassesPath));
		}
		
		return stream.build();
	}

	private URL pathToURL(Path path) {
		try {
			return new URL("file:/".concat(path.toString().replace("\\", "/")).concat("/"));
		} catch (MalformedURLException e) {
			throw new RuntimeException(String.format("Error while converting path %s to URL", path.toString()));
		}
	}

	public File findFile(Path relativePath) {
		return findAbsolutePath(relativePath).toFile();
	}
	
	private Path findAbsolutePath(Path relativePath) {
		return sourcesDirectoryPath.resolve(relativePath);
	}
	
	private Path findRelativeToSourceRoot(Path absolutePath) {
		return sourcesDirectoryPath.relativize(absolutePath);
	}
	
	public List<Path> javaPathsInDirectory(Path relativeToSourceRoot) {
		return Stream.of(findFile(relativeToSourceRoot).listFiles(this::isJavaFile))
			.map(File::toPath)
			.map(this::findRelativeToSourceRoot)
			.collect(Collectors.toList());
	}
	
	private boolean isJavaFile(File fileName) {
		return fileName.isFile() && fileName.getName().endsWith(".java");
	}
	
	public List<Path> subDirectoryPathsInDirectory(Path relativeToSourceRoot) {
		return Stream.of(findFile(relativeToSourceRoot).listFiles(File::isDirectory))
			.map(File::toPath)
			.map(this::findRelativeToSourceRoot)
			.collect(Collectors.toList());
	}

	public void copyEverythingTo(Path targetPath) throws IOException {
		FileUtils.copyDirectoryStructure(Paths.get(rootDirectory).toFile(), targetPath.toFile());
	}
	
	public void copyToAnotherProject(MavenJavaProject targetProject, Path relativePath) throws IOException {
		Files.copy(findAbsolutePath(relativePath),
				targetProject.findAbsolutePath(relativePath),
				StandardCopyOption.REPLACE_EXISTING);
	}
	
	public void copyDirectoryToAnotherProject(MavenJavaProject targetProject, Path relativePath) throws IOException {
		FileUtils.copyDirectoryStructure(findFile(relativePath), targetProject.findFile(relativePath));
	}
	
	public String md5Hash(Path relativePath) throws IOException {
		File file = findFile(relativePath);
		try (FileInputStream fis = new FileInputStream(file)) {
			return DigestUtils.md5DigestAsHex(fis);			
		}
	}
	
	public long size(Path relativePath) throws IOException {
		return Files.size(findAbsolutePath(relativePath));
	}
	
	public String readSourceCode(Path relativePath) throws IOException {
		return new String(Files.readAllBytes(findAbsolutePath(relativePath)));
	}
	
	public String tryReadSourceCode(Path relativePath) {
		try {
			return readSourceCode(relativePath);
		} catch (IOException ioe) {
			return StringUtils.EMPTY;
		}
	}
	
	public void writeSourceCode(Path relativePath, String sourceCode) throws IOException {
		Files.write(findAbsolutePath(relativePath), sourceCode.getBytes());
	}
}
