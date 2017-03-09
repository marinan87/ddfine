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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

/*
 * Helper class for simplistic navigation in the example workspace. 
 * Will be completely rewritten later.
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
	
	public static ICompilationUnit createCopyOfCompilationUnit(ICompilationUnit cu) throws JavaModelException {
		IJavaProject stagingProject = findJavaProjectInWorkspace("StagingArea");
		IPackageFragment stagingPackage = findPackageInProject(stagingProject, "src", "simple");
		ICompilationUnit copy = stagingPackage.createCompilationUnit(cu.getElementName(), cu.getSource(), true, null);
		copy.becomeWorkingCopy(new NullProgressMonitor());
		copy.commitWorkingCopy(false, new NullProgressMonitor());  
		copy.makeConsistent(new NullProgressMonitor());
		return copy;
	}
		
	private static IPackageFragment findPackageInProject(IJavaProject project, String scope, String name) throws JavaModelException  {	
		IPackageFragment[] packages = project.getPackageFragments();		
		return Stream.of(packages)
				.filter(aPackage -> aPackage.getParent().getElementName().equals(scope) && aPackage.getElementName().equals(name))
				.findFirst()
				.orElse(null);
	}
	
	public static ITextEditor openTextEditor(ICompilationUnit cu) throws PartInitException {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(cu.getPath());
		IEditorPart editor = IDE.openEditor(page, file, true);
		return (ITextEditor) editor;
	}
	
	public static File getFile(ICompilationUnit cu) throws JavaModelException {
		String rawLocation = ((IFile) cu.getUnderlyingResource()).getRawLocation().toString();
		File file = new File(rawLocation);
		assert(file.exists());
		return file;
	}

	public static void saveModifiedFiles() throws JavaModelException {
		try {			
			PlatformUI.getWorkbench().saveAllEditors(false);
			
			IJobManager jobManager = Job.getJobManager();
			jobManager.join(ResourcesPlugin.FAMILY_AUTO_BUILD, new NullProgressMonitor());
			jobManager.join(ResourcesPlugin.FAMILY_AUTO_REFRESH, new NullProgressMonitor());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
