package regressionfinder.utils;

import java.io.File;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/*
 * Helper class for simplistic navigation in the example workspace. 
 */
public class JavaModelHelper {
	
	private static final String JAVANATURE_PROJECT = "org.eclipse.jdt.core.javanature";

	private JavaModelHelper() {
	}

	public static IJavaProject findJavaProjectInWorkspace(String name) {		
		return collectJavaProjectsInWorkspace().stream()
			.filter(project -> project.getElementName().equals(name))
			.findFirst()
			.orElse(null);
	}
	
	private static Collection<IJavaProject> collectJavaProjectsInWorkspace() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot workspaceRoot = workspace.getRoot();
		IProject[] projects = workspaceRoot.getProjects();

		return Stream.of(projects).filter(accessibleJavaProject()).map(JavaCore::create).collect(Collectors.toSet());
	}
	
	private static Predicate<IProject> accessibleJavaProject() {
		return project -> {
			try {
				return project.isNatureEnabled(JAVANATURE_PROJECT);
			} catch (CoreException e) {
				return false;
			}
		};
	}
	
	public static ICompilationUnit getCompilationUnit(String projectName, String fileName) throws JavaModelException {
		IJavaProject aProject = findJavaProjectInWorkspace(projectName);
		IPackageFragment aPackage = findPackageInProject(aProject, "src", "simple");
		return aPackage.getCompilationUnit(fileName);
	}
	
	public static ICompilationUnit createCopyOfCompilationUnit(ICompilationUnit cu, String fileName) throws JavaModelException {
		IJavaProject stagingProject = findJavaProjectInWorkspace("StagingArea");
		IPackageFragment stagingPackage = findPackageInProject(stagingProject, "src", "simple");
		return stagingPackage.createCompilationUnit(fileName, cu.getSource(), true, null);
	}
	
	private static IPackageFragment findPackageInProject(IJavaProject project, String scope, String name) throws JavaModelException  {	
		IPackageFragment[] packages = project.getPackageFragments();		
		return Stream.of(packages)
				.filter(aPackage -> aPackage.getParent().getElementName().equals(scope) && aPackage.getElementName().equals(name))
				.findFirst()
				.orElse(null);
	}
	
	public static File getFile(ICompilationUnit originalCU) throws JavaModelException {
		String rawLocation = ((IFile) originalCU.getUnderlyingResource()).getRawLocation().toString();
		File file = new File(rawLocation);
		assert(file.exists());
		return file;
	}

}
