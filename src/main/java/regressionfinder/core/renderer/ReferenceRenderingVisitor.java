package regressionfinder.core.renderer;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import regressionfinder.core.EvaluationContext;
import regressionfinder.model.AffectedFile;
import regressionfinder.model.AffectedStructuralEntity;
import regressionfinder.model.CombinedPath;

@Component
public class ReferenceRenderingVisitor implements RenderingVisitor {

	@Autowired
	private EvaluationContext evaluationContext;
	
	@Override
	public String visit(AffectedFile entity) {
		StringBuilder result = new StringBuilder();
		result.append("<pre class=\"brush: java;\">");
		result.append(evaluationContext.getReferenceProject().tryReadSourceCode(entity.getPath()));
		result.append("</pre>");
		return result.toString();
	}

	@Override
	public String visit(AffectedStructuralEntity entity) {
		String result = StringUtils.EMPTY;
		CombinedPath path = entity.getPath();
		
		switch (entity.getStructuralChangeType()) {
		case FILE_REMOVED:
		case PACKAGE_REMOVED:
			result = path.toString();
			break;
		default:
			result = StringUtils.EMPTY;
			break;
		}
		
		return result;
	}
}
