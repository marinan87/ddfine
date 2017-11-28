package regressionfinder.core.manipulation;

import java.io.IOException;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Patch;
import regressionfinder.model.AffectedFile;
import regressionfinder.model.MultiModuleMavenJavaProject;

public class SourceCodeFileManipulator {
	
	private static Pattern INSIDE_PARENTHESES = Pattern.compile("^\\((.*)\\);$");

	private final AffectedFile file;
	private final MultiModuleMavenJavaProject workingAreaProject;
	private final diff_match_patch diffMatchPatch;
	private final String content;
	
	public SourceCodeFileManipulator(AffectedFile file, MultiModuleMavenJavaProject workingAreaProject, diff_match_patch diffMatchPatch) throws IOException {
		this.file = file;
		this.workingAreaProject = workingAreaProject;
		this.diffMatchPatch = diffMatchPatch;
		
        content = workingAreaProject.readSourceCode(file.getPath());
	}

	public void applyChanges() throws IOException {
		LinkedList<Patch> patches = new LinkedList<>(file.getChangesInFile());
		Object[] results = diffMatchPatch.patch_apply(patches, content);
		workingAreaProject.writeSourceCode(file.getPath(), (String) results[0]);
	}

	public static String normalizeEntityValue(String entityValue) {
		String result = entityValue;

		Matcher matcher = INSIDE_PARENTHESES.matcher(result);
		if (matcher.matches()) {
			result = matcher.group(1) + ";";
		}

		return result;
	}
}
