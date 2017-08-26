package regressionfinder.model;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class ProjectSourceTreeScanner {
	
	private static final List<String> IGNORED_DIRS = ImmutableList.of("src", ".settings", ".svn", ".metadata", "archetype", "ci");	
	
	public static void scanSourceTreeForMavenProjects(Path aggregatorRoot, List<MavenJavaProject> collector) {
		File currentDirectory = aggregatorRoot.toFile();
		Preconditions.checkArgument(currentDirectory.isDirectory());
		
		MavenJavaProject javaProject = MavenJavaProject.createMavenProject(aggregatorRoot.toString());
		if (javaProject.isMavenProject()) {
			collector.add(javaProject);
		} else {
			File[] subDirectories = currentDirectory.listFiles(ProjectSourceTreeScanner::isProjectDirectory);
			for (File subDirectory : subDirectories) {
				Path subDirectoryPath = aggregatorRoot.resolve(subDirectory.getName());
				scanSourceTreeForMavenProjects(subDirectoryPath, collector);
			}
		}		
	}
	
	private static boolean isProjectDirectory(File file) {
		return file.isDirectory() && !IGNORED_DIRS.contains(file.getName());
	}
}
