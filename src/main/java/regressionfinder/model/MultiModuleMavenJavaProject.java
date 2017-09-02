package regressionfinder.model;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.codehaus.plexus.util.FileUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class MultiModuleMavenJavaProject extends MavenProject {
	
	private static final List<String> IGNORED_DIRS = ImmutableList.of("src", ".settings", ".svn", ".metadata", "archetype", "ci");	
	
	private Map<Path, MavenJavaProject> mavenProjects = new HashMap<>();
	
	
	public MultiModuleMavenJavaProject(String aggregatorRoot) {
		super(Paths.get(aggregatorRoot));
		scanPathForMavenJavaProjects(this.rootDirectory);
	}
	
	private void scanPathForMavenJavaProjects(Path projectRoot) {
		File currentDirectory = projectRoot.toFile();
		Preconditions.checkArgument(currentDirectory.isDirectory());
		
		MavenJavaProject javaProject = MavenJavaProject.createMavenProject(projectRoot);
		if (javaProject.isMavenProject()) {
			mavenProjects.put(rootDirectory.relativize(projectRoot), javaProject);
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
		return mavenProjects.values().stream().flatMap(MavenJavaProject::getClassPaths);
	}
	
	public Map<Path, MavenJavaProject> getMavenProjects() {
		return ImmutableMap.copyOf(mavenProjects);
	}
	
	public MavenJavaProject getMavenProject(Path pathToProject) {
		return mavenProjects.get(pathToProject);
	}
	
	private MavenJavaProject getMavenProject(CombinedPath path) {
		return getMavenProject(path.getPathToModule());
	}
	
	public File findFile(CombinedPath path) {
		return getMavenProject(path).findFile(path.getPathToResource());
	}
	
	public String readSourceCode(CombinedPath path) throws IOException {
		return getMavenProject(path).readSourceCode(path.getPathToResource());
	}
	
	public String tryReadSourceCode(CombinedPath path) { 
		return getMavenProject(path).tryReadSourceCode(path.getPathToResource());
	}
	
	public void writeSourceCode(CombinedPath path, String sourceCode) throws IOException {
		getMavenProject(path).writeSourceCode(path.getPathToResource(), sourceCode);		
	}
	
	public void copyFileToAnotherProject(MultiModuleMavenJavaProject targetProject, CombinedPath path) throws IOException {
		getMavenProject(path).copyFileToAnotherProject(targetProject.getMavenProject(path), path.getPathToResource());
	}
	
	public void copyDirectoryToAnotherProject(MultiModuleMavenJavaProject targetProject, CombinedPath path) throws IOException {
		getMavenProject(path).copyDirectoryToAnotherProject(targetProject.getMavenProject(path), path.getPathToResource());
	}
}
