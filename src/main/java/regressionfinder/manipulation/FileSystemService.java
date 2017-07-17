package regressionfinder.manipulation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import regressionfinder.core.EvaluationContext;

/*
 * Helper class for performing simplistic operations with files in working directory.
 * Will be completely rewritten later.
 */
@Service
public class FileSystemService {
	
	private static final String PATH_TO_SOURCES = "src";
	private static final String PATH_TO_PACKAGE = "simple";
	
	@Autowired
	private EvaluationContext context;

	
	public String getPathToJavaFile(String versionBasePath, String fileName) {
		return Paths.get(versionBasePath, PATH_TO_SOURCES, PATH_TO_PACKAGE, fileName).toString();
	}
	
	
	public Path copyFileToStagingArea(String sourceFile) throws IOException {
		Path source = Paths.get(sourceFile);
		Path copy = Paths.get(context.getWorkingArea());
		return Files.copy(source, 
				copy.resolve(Paths.get(PATH_TO_SOURCES, PATH_TO_PACKAGE, source.getFileName().toString())), 
				StandardCopyOption.REPLACE_EXISTING);
	}
	
	public File getReferenceVersion() {
		return getFile(context.getReferenceVersion());
	}
	
	public File getFaultyVersion() {
		return getFile(context.getFaultyVersion());
	}

	private File getFile(String location) {
		File file = new File(location);
		assert(file.exists());
		return file;
	}

	public void saveModifiedFilesAndCompile(StringBuilder content, Path path) throws IOException, MavenInvocationException {
		Files.write(path, content.toString().getBytes());
		
		// TODO: extract into separate class
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(Paths.get(context.getWorkingArea(), "pom.xml").toFile());
		request.setGoals(Arrays.asList("compile"));
		request.setThreads("1C");
		request.setMavenOpts("-XX:+TieredCompilation -XX:TieredStopAtLevel=1");
		
		Invoker invoker = new DefaultInvoker();
		invoker.setMavenHome(new File(System.getenv("MAVEN_HOME")));
		invoker.execute(request); 		

		// TODO: run incremental build
	}
}
