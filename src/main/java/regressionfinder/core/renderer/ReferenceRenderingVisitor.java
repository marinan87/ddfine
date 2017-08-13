package regressionfinder.core.renderer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import regressionfinder.core.EvaluationContext;
import regressionfinder.model.AffectedFile;
import regressionfinder.model.AffectedStructuralEntity;

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
		// TODO Auto-generated method stub
		return null;
	}
}
