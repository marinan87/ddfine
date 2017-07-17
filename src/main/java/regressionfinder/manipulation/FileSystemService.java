package regressionfinder.manipulation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;

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
	
	public File getReferenceVersion() {
		return getFile(context.getReferenceVersion());
	}
	
	public File getFaultyVersion() {
		return getFile(context.getFaultyVersion());
	}

	private File getFile(String location) {
		File file = new File(location);
		Preconditions.checkState(file.isFile());
		return file;
	}

	public void saveModifiedFiles(StringBuilder content, Path path) {
		try {
			Files.write(path, content.toString().getBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
