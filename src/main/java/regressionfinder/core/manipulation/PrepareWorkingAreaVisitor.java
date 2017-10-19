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
public class PrepareWorkingAreaVisitor implements WorkingAreaManipulationVisitor {

	@Autowired
	private EvaluationContext evaluationContext;
	
	@Override
	public void visit(AffectedFile entity) throws IOException {
		new SourceCodeFileManipulator(entity, evaluationContext.getWorkingAreaProject(), evaluationContext.getFaultyProject()).applyChanges();
	}

	@Override
	public void visit(AffectedStructuralEntity entity) throws IOException {
		MultiModuleMavenJavaProject 
			workingProject = evaluationContext.getWorkingAreaProject(),
			faultyProject = evaluationContext.getFaultyProject();
		CombinedPath entityPath = entity.getPath();
		
		switch (entity.getStructuralChangeType()) {
		case FILE_REMOVED:
			workingProject.findFile(entityPath).delete();
			break;
		case FILE_ADDED:
			faultyProject.copyFileToAnotherProject(workingProject, entityPath);
			break;
		case PACKAGE_REMOVED:
			FileUtils.deleteDirectory(workingProject.findFile(entityPath));
			break;
		case PACKAGE_ADDED:
			faultyProject.copyDirectoryToAnotherProject(workingProject, entityPath);
			break;
		default:
			throw new UnsupportedOperationException();
		}
	}
}
