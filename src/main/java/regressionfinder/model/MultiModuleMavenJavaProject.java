package regressionfinder.model;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.codehaus.plexus.util.FileUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class MultiModuleMavenJavaProject extends MavenProject {
	
	private static final List<String> IGNORED_DIRS = ImmutableList.of("src", ".settings", ".svn", ".metadata", "archetype", "ci");	
	
	private List<MavenJavaProject> mavenProjects = new ArrayList<>();
	
	
	public MultiModuleMavenJavaProject(String aggregatorRoot) {
		super(Paths.get(aggregatorRoot));
		scanPathForMavenJavaProjects(this.rootDirectory);
	}
	
	private void scanPathForMavenJavaProjects(Path projectRoot) {
		File currentDirectory = projectRoot.toFile();
		Preconditions.checkArgument(currentDirectory.isDirectory());
		
		MavenJavaProject javaProject = MavenJavaProject.createMavenProject(projectRoot);
		if (javaProject.isMavenProject()) {
			mavenProjects.add(javaProject);
		} else {
			File[] subDirectories = currentDirectory.listFiles(this::isProjectDirectory);
			for (File subDirectory : subDirectories) {
				Path subDirectoryPath = projectRoot.resolve(subDirectory.getName());
				scanPathForMavenJavaProjects(subDirectoryPath);
			}
		}				
	}

	private boolean isProjectDirectory(File file) {
		return file.isDirectory() && !IGNORED_DIRS.contains(file.getName());
	}
	
	public MultiModuleMavenJavaProject cloneToWorkingDirectory(Path workingDirectory) throws IOException {
		// TODO: ignore .svn and .settings folders
		// TODO: copy with original timestamps
		FileUtils.copyDirectoryStructure(rootDirectory.toFile(), workingDirectory.toFile());
		return new MultiModuleMavenJavaProject(workingDirectory.toString());
	}
	
	public Path getRootDirectory() {
		return rootDirectory;
	}
	
	public Stream<URL> collectClassPaths() {
		return mavenProjects.stream().flatMap(MavenJavaProject::getClassPaths);
	}
	
	public Stream<URL> collectLocalMavenDependencies() {
		return mavenProjects.stream().flatMap(MavenProject::getLocalMavenDependencies);
	}
}
