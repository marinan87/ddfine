package regressionfinder.core.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.core.EvaluationContext;
import regressionfinder.manipulation.SourceCodeFileManipulator;
import regressionfinder.model.AffectedFile;

@Component
public class FaultyRenderingVisitor implements RenderingVisitor {

	@Autowired
	private EvaluationContext evaluationContext;
	
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
		List<SourceCodeChange> remainingChanges = new ArrayList<>(file.getChangesInFile());
		List<Integer> lines = new ArrayList<>();
		
		Scanner scanner = new Scanner(sourceCode);
		int line = 0;
		while (scanner.hasNextLine()) {
			line++;
			String nextLine = scanner.nextLine();
			
			for (SourceCodeChange change : remainingChanges) {
				String changeContent = SourceCodeFileManipulator.normalizeEntityValue(change.getChangedEntity().getContent());
				if (nextLine.contains(changeContent)) {
					lines.add(line);
					remainingChanges.remove(change);
					break;
				}	
			}
		}
		scanner.close();
		
		return lines;
	}
}
