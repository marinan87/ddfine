package regressionfinder.core.renderer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import regressionfinder.core.EvaluationContext;
import regressionfinder.model.AffectedFile;

@Component
public class ReferenceRenderingVisitor implements RenderingVisitor {

	@Autowired
	private EvaluationContext evaluationContext;
	
	@Override
	public String visit(AffectedFile entity) {
		StringBuilder result = new StringBuilder();
		result.append("<pre class=\"brush: java;\">");
		result.append(entity.readSourceCode(evaluationContext.getReferenceProject()));
		result.append("</pre>");
		return result.toString();
	}
}
