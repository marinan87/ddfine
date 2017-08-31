package regressionfinder.core.manipulation;

import java.io.IOException;

import org.codehaus.plexus.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import regressionfinder.core.EvaluationContext;
import regressionfinder.model.AffectedFile;
import regressionfinder.model.AffectedStructuralEntity;
import regressionfinder.model.CombinedPath;
import regressionfinder.model.MultiModuleMavenJavaProject;

@Component
public class RestoreWorkingAreaVisitor implements WorkingAreaManipulationVisitor {

	@Autowired
	private EvaluationContext evaluationContext;
	
	@Override
	public void visit(AffectedFile entity) throws IOException {
		evaluationContext.getReferenceProject().copyFileToAnotherProject(evaluationContext.getWorkingAreaProject(), entity.getPath());
	}

	@Override
	public void visit(AffectedStructuralEntity entity) throws IOException {
		MultiModuleMavenJavaProject 
			workingProject = evaluationContext.getWorkingAreaProject(),
			referenceProject = evaluationContext.getReferenceProject();
		CombinedPath entityPath = entity.getPath();

		switch (entity.getStructuralChangeType()) {
		case FILE_REMOVED:
			referenceProject.copyFileToAnotherProject(workingProject, entityPath);
			break;
		case FILE_ADDED:
			workingProject.findFile(entityPath).delete();
			break;
		case PACKAGE_REMOVED:
			referenceProject.copyDirectoryToAnotherProject(workingProject, entityPath);
			break;
		case PACKAGE_ADDED:
			FileUtils.deleteDirectory(workingProject.findFile(entityPath));
			break;
		default:
			throw new UnsupportedOperationException();
		}		
	}
}
