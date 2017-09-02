package regressionfinder.model;

import java.io.File;
import java.nio.file.Path;

public abstract class MavenProject {
	
	private static final String POM_XML = "pom.xml";

	protected final Path rootDirectory;
	protected final File rootPomXml;


	protected MavenProject(Path rootDirectory) {
		this.rootDirectory = rootDirectory;
		this.rootPomXml = rootDirectory.resolve(POM_XML).toFile();
	}
	
	public File getRootPomXml() {
		return rootPomXml;
	}
}
