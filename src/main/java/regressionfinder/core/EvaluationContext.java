package regressionfinder.core;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static regressionfinder.runner.CommandLineOption.FAULTY_VERSION;
import static regressionfinder.runner.CommandLineOption.REFERENCE_VERSION;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.deltadebugging.ddcore.DeltaSet;
import org.deltadebugging.ddcore.tester.JUnitTester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.manipulation.FileManipulator;
import regressionfinder.manipulation.FileSourceCodeChange;
import regressionfinder.runner.CommandLineArgumentsInterpreter;

@Component
public class EvaluationContext extends JUnitTester {

	private MavenProject referenceVersionProject, faultyVersionProject, workingAreaProject;

	@Autowired
	private ReflectionalTestMethodInvoker reflectionalInvoker;
			
	public void initializeOnce(CommandLineArgumentsInterpreter arguments) {
		try {		
			referenceVersionProject = new MavenProject(arguments.getValue(REFERENCE_VERSION));
			faultyVersionProject = new MavenProject(arguments.getValue(FAULTY_VERSION));
			
			Path temporaryDirectory = Files.createTempDirectory("deltadebugging");
			referenceVersionProject.copyEverythingTo(temporaryDirectory);
			workingAreaProject = new MavenProject(temporaryDirectory.toString());
			workingAreaProject.triggerCompilationWithTests();
			
			reflectionalInvoker.initializeOnce(arguments);
		} catch (Exception e) {
			System.out.println("Exception during initialization of evaluation context");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public MavenProject getReferenceProject() {
		return referenceVersionProject;
	}
	
	public MavenProject getFaultyProject() {
		return faultyVersionProject;
	}
	
	public MavenProject getWorkingAreaProject() {
		return workingAreaProject;
	}
	
	@Override
	public int test(DeltaSet set) {
		List<FileSourceCodeChange> selectedChangeSet = (List<FileSourceCodeChange>) set.stream().collect(toList());
		
		applySelectedChanges(selectedChangeSet);
		return reflectionalInvoker.testAppliedChangeSet(); 
	}
	
	public void applySelectedChanges(List<FileSourceCodeChange> sourceCodeChanges) {
		Map<Path, List<SourceCodeChange>> mapOfChanges = sourceCodeChanges.stream()
				.collect(toMap(
						FileSourceCodeChange::getPathToFile, 
						change -> Lists.newArrayList(change.getSourceCodeChange()),
						(a, b) -> { 
							a.addAll(b);
							return a;
						}));	
		
		mapOfChanges.entrySet().forEach(entry -> {
			try {
				Path copyOfSource = workingAreaProject.copyFromAnotherProject(referenceVersionProject, entry.getKey());
				new FileManipulator(copyOfSource).applyChanges(entry.getValue());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		workingAreaProject.triggerSimpleCompilation();
	}
}
