package regressionfinder.utils;

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

import regressionfinder.testrunner.DeltaSetEvaluator;

/*
 * Helper class for performing simplistic operations with files in working directory.
 * Will be completely rewritten later.
 */
public class FileUtils {
	
	private static final String PATH_TO_SOURCES = "src";
	private static final String PATH_TO_PACKAGE = "simple";
	
	private FileUtils() {
	}
	
	public static String getPathToJavaFile(String basePath, String versionBasePath, String fileName) {
		return Paths.get(basePath, versionBasePath, PATH_TO_SOURCES, PATH_TO_PACKAGE, fileName).toString();
	}
	
	
	public static Path copyFileToStagingArea(String basePath, String sourceFile) throws IOException {
		Path source = Paths.get(sourceFile);
		Path copy = Paths.get(basePath, DeltaSetEvaluator.WORK_AREA);
		return Files.copy(source, 
				copy.resolve(Paths.get(PATH_TO_SOURCES, PATH_TO_PACKAGE, source.getFileName().toString())), 
				StandardCopyOption.REPLACE_EXISTING);
	}

	public static File getFile(String location) {
		File file = new File(location);
		assert(file.exists());
		return file;
	}

	public static void saveModifiedFiles(StringBuilder content, Path path) throws IOException, MavenInvocationException {
		Files.write(path, content.toString().getBytes());
		
		Path stagingAreaPath = path;
		while (!stagingAreaPath.getFileName().endsWith(DeltaSetEvaluator.WORK_AREA)) {
			stagingAreaPath = stagingAreaPath.getParent();
		}
		
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(Paths.get(stagingAreaPath.toString(), "pom.xml").toFile());
		request.setGoals(Arrays.asList("compile"));
		request.setThreads("1C");
		request.setMavenOpts("-XX:+TieredCompilation -XX:TieredStopAtLevel=1");
		
		Invoker invoker = new DefaultInvoker();
		invoker.setMavenHome(new File(System.getenv("MAVEN_HOME")));
		invoker.execute(request); 		

		// TODO: run incremental build
	}

}
