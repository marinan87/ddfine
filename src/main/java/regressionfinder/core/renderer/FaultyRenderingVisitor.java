package regressionfinder.core.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import name.fraser.neil.plaintext.diff_match_patch.Patch;
import regressionfinder.core.EvaluationContext;
import regressionfinder.core.statistics.StatisticsTracker;
import regressionfinder.model.AffectedFile;
import regressionfinder.model.AffectedStructuralEntity;
import regressionfinder.model.CombinedPath;

@Component
public class FaultyRenderingVisitor implements RenderingVisitor {

	@Autowired
	private EvaluationContext evaluationContext;
	
	@Autowired
	private StatisticsTracker statisticsTracker;
	
	
	@Override
	public String visit(AffectedFile entity) {
		StringBuilder result = new StringBuilder();
		
		String sourceCode = evaluationContext.getFaultyProject().tryReadSourceCode(entity.getPath());
		result.append(String.format("<pre class=\"brush: java; highlight: %s\">", getLineNumbers(entity, sourceCode)));
		result.append(sourceCode);
		result.append("</pre>");
		
		return result.toString();
	}
	
	private List<Integer> getLineNumbers(AffectedFile file, String sourceCode) {
		List<Patch> remainingChanges = new ArrayList<>(file.getChangesInFile());
		List<Integer> offsets = new ArrayList<>();
		offsets.add(0);
		List<Integer> lines = new ArrayList<>();
		int totalOffset = 0;
		
		Scanner scanner = new Scanner(sourceCode);
		while (scanner.hasNextLine()) {
			totalOffset += scanner.nextLine().length() + 2;
			offsets.add(totalOffset);
		}
		scanner.close();
		
		for (Patch change : remainingChanges) {
			int line = 0;
			for (; line < offsets.size(); line++) {
				if (change.start1 >= offsets.get(line) && (line + 1 == offsets.size() || change.start1 < offsets.get(line + 1))) {
					break;
				}
			}
			lines.add(line + 1);
		}
		
		statisticsTracker.logNumberOfLinesToInspect(lines.size());
		return lines;
	}

	@Override
	public String visit(AffectedStructuralEntity entity) {
		String result = StringUtils.EMPTY;
		CombinedPath path = entity.getPath();
		
		switch (entity.getStructuralChangeType()) {
		case FILE_ADDED:
		case PACKAGE_ADDED:
			result = path.toString();
			break;
		default:
			result = StringUtils.EMPTY;
			break;
		}
		
		return result;
	}
}
