package regressionfinder.model;

import static java.lang.String.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.FileUtils;
import org.springframework.util.DigestUtils;

public class MavenJavaProject extends MavenProject {
	
	private static final String SOURCES_DIR = "src/main/java";
	private static final String TARGET_DIR = "target";
	private static final String CLASSES_DIR = "classes";
	private static final String TEST_CLASSES_DIR = "test-classes";

	private final Path sourcesDirectoryPath, targetClassesPath, targetTestClassesPath;
	
	
	public static MavenJavaProject tryCreateMavenProject(Path rootDirectory) {
		MavenJavaProject project = createMavenProject(rootDirectory);
		project.assertIsMavenProject();
		return project;
	}
	
	public static MavenJavaProject createMavenProject(Path rootDirectory) {
		return new MavenJavaProject(rootDirectory);
	}
	
	private MavenJavaProject(Path rootDirectory) {
		super(rootDirectory);

		this.sourcesDirectoryPath = rootDirectory.resolve(SOURCES_DIR);
		Path targetDirectoryPath = rootDirectory.resolve(TARGET_DIR);
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

	public Stream<URL> getClassPaths() {
		Builder<URL> stream = Stream.<URL>builder().add(directoryPathToURL(targetClassesPath));
		
		if (targetTestClassesPath.toFile().isDirectory()) {
			stream.add(directoryPathToURL(targetTestClassesPath));
		}
		
		return stream.build();
	}
	
	private URL directoryPathToURL(Path path) {
		return stringToURL(path.toString(), true);
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
	
	public void copyFileToAnotherProject(MavenJavaProject targetProject, Path relativePath) throws IOException {
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
