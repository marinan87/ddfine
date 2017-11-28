package regressionfinder.core;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Patch;
import regressionfinder.core.statistics.ExecutionPhase;
import regressionfinder.core.statistics.LogDuration;
import regressionfinder.core.statistics.StatisticsTracker;
import regressionfinder.model.CombinedPath;
import regressionfinder.model.MavenJavaProject;
import regressionfinder.model.MinimalApplicableChange;
import regressionfinder.model.MinimalChangeInFile;
import regressionfinder.model.MinimalStructuralChange;
import regressionfinder.model.SourceTreeComparisonResults;
import regressionfinder.model.StructuralChangeType;

@Service
public class SourceTreeDifferencer {

	private static final Path ROOT_PATH = Paths.get(StringUtils.EMPTY);
	
	
	@Autowired
	private EvaluationContext evaluationContext;
	
	@Autowired
	private StatisticsTracker statisticsTracker;
	
	@Autowired
	private diff_match_patch diffMatchPatch;
	
	
	@LogDuration(ExecutionPhase.CHANGE_DISTILLING)
	public List<MinimalApplicableChange> distillChanges() {
		List<MinimalApplicableChange> filteredChanges = new SourceTreeScanner().distillChanges();
		
		List<MinimalChangeInFile> sourceCodeChanges = filteredChanges.stream()
				.filter(change -> (change instanceof MinimalChangeInFile))
				.map(change -> (MinimalChangeInFile) change)
				.collect(Collectors.toList());
		statisticsTracker.logDetectedChanges(sourceCodeChanges);
		
		return filteredChanges;
	}

	private Stream<MinimalChangeInFile> distillChangesForPath(CombinedPath path) {
		try {
			String left = evaluationContext.getReferenceProject().readSourceCode(path);
			String right = evaluationContext.getFaultyProject().readSourceCode(path);
			
			diffMatchPatch.Match_Distance = 5000;
			LinkedList<Patch> patches = diffMatchPatch.patch_make(left, right);
			statisticsTracker.incrementNumberOfSourceCodeChangesBySize(patches.size());

			return patches.stream()
					.map(patch -> {
						statisticsTracker.incrementNumberOfUnsafeSourceCodeChanges();
						return new MinimalChangeInFile(path, patch);
					});
		} catch (IOException ioe) {
			throw new RuntimeException("An I/O error occurred while trying to obtain diff.");
		}
	}
	
	private class SourceTreeScanner {		
		private final SourceTreeComparisonResults comparisonResults;
		private MavenJavaProject currentReferenceProject, currentFaultyProject;
		
		private SourceTreeScanner() {
			this.comparisonResults = new SourceTreeComparisonResults();
		}
		
		public List<MinimalApplicableChange> distillChanges() {
			evaluationContext.getReferenceProject().getMavenProjects().entrySet().forEach(entry -> {
				comparisonResults.setCurrentProjectRelativePath(entry.getKey());
				currentReferenceProject = entry.getValue();
				currentFaultyProject = evaluationContext.getFaultyProject().getMavenProject(entry.getKey());
				
				scanForChangedPaths(ROOT_PATH);
			});	
			
			Stream<MinimalApplicableChange> changesStream = Stream.empty();
			changesStream = Stream.concat(changesStream, 
					comparisonResults.getModifiedFiles().stream().flatMap(SourceTreeDifferencer.this::distillChangesForPath));
			changesStream = Stream.concat(changesStream,
					comparisonResults.getRemovedFiles().stream().map(path -> {
						statisticsTracker.incrementNumberOfStructuralChanges();
						return new MinimalStructuralChange(path, StructuralChangeType.FILE_REMOVED);
					}));
			changesStream = Stream.concat(changesStream,
					comparisonResults.getAddedFiles().stream().map(path -> {
						statisticsTracker.incrementNumberOfStructuralChanges();
						return new MinimalStructuralChange(path, StructuralChangeType.FILE_ADDED);
					}));
			changesStream = Stream.concat(changesStream,
					comparisonResults.getRemovedPackages().stream().map(path -> {
						statisticsTracker.incrementNumberOfStructuralChanges();
						return new MinimalStructuralChange(path, StructuralChangeType.PACKAGE_REMOVED);
					}));
			changesStream = Stream.concat(changesStream,
					comparisonResults.getAddedPackages().stream().map(path -> {
						statisticsTracker.incrementNumberOfStructuralChanges();
						return new MinimalStructuralChange(path, StructuralChangeType.PACKAGE_ADDED);
					}));
			
			return changesStream.collect(Collectors.toList());
		}	
			
		private void scanForChangedPaths(Path relativeToSourceRoot) {
			File directoryInReferenceProject = currentReferenceProject.findFile(relativeToSourceRoot);
			File directoryInFaultyProject = currentFaultyProject.findFile(relativeToSourceRoot);

			Preconditions.checkArgument(directoryInReferenceProject.isDirectory(), 
					format("%s is not a directory!", relativeToSourceRoot));
			Preconditions.checkArgument(directoryInFaultyProject.exists() && directoryInFaultyProject.isDirectory(), 
					format("Directory %s does not exist in faulty project!", directoryInFaultyProject));
			
			scanForChangedJavaPaths(relativeToSourceRoot);
			scanForChangedDirs(relativeToSourceRoot);	
			// TODO: support of file renaming/moving?
		}

		private void scanForChangedJavaPaths(Path relativeToSourceRoot) {
			List<Path> javaPathsInReference = currentReferenceProject.javaPathsInDirectory(relativeToSourceRoot);
			List<Path> javaPathsInFaulty = currentFaultyProject.javaPathsInDirectory(relativeToSourceRoot);
			
			Collections2.filter(javaPathsInReference, Predicates.not(Predicates.in(javaPathsInFaulty))).forEach(comparisonResults::addRemovedFile);
			Collections2.filter(javaPathsInFaulty, Predicates.not(Predicates.in(javaPathsInReference))).forEach(comparisonResults::addAddedFile);
			
			Collections2.filter(javaPathsInReference, Predicates.in(javaPathsInFaulty)).stream()				
					.filter(sizeHasChanged().or(checkSumHasChanged()))
					.forEach(comparisonResults::addModifiedFile);
		}

		private Predicate<Path> sizeHasChanged() {
			return relativePath -> {
				try {
					return currentReferenceProject.size(relativePath) != currentFaultyProject.size(relativePath);
				} catch (IOException e) {
					return true;
				}
			};
		}

		private Predicate<Path> checkSumHasChanged() {
			return relativePath -> {
				try {
					String referenceMd5 = currentReferenceProject.md5Hash(relativePath);
					String faultyMd5 = currentFaultyProject.md5Hash(relativePath);
					return !referenceMd5.equals(faultyMd5);
				} catch (IOException e) {
					return true;
				}			
			};
		}

		private void scanForChangedDirs(Path relativeToSourceRoot) {
			List<Path> subDirectoriesInReference = currentReferenceProject.subDirectoryPathsInDirectory(relativeToSourceRoot);
			List<Path> subDirectoriesInFaulty = currentFaultyProject.subDirectoryPathsInDirectory(relativeToSourceRoot);
			
			Collections2.filter(subDirectoriesInReference, Predicates.not(Predicates.in(subDirectoriesInFaulty))).forEach(comparisonResults::addRemovedPackage);
			Collections2.filter(subDirectoriesInFaulty, Predicates.not(Predicates.in(subDirectoriesInReference))).forEach(comparisonResults::addAddedPackage);

			Collections2.filter(subDirectoriesInReference, Predicates.in(subDirectoriesInFaulty)).forEach(this::scanForChangedPaths);
		}
	}
}
