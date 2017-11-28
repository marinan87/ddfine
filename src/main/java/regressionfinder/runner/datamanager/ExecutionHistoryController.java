package regressionfinder.runner.datamanager;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import regressionfinder.core.statistics.persistence.entities.Execution;
import regressionfinder.core.statistics.persistence.entities.ExecutionMetadata;

@Controller
@RequestMapping("/executions")
public class ExecutionHistoryController {
	
	@Autowired
	private ExecutionHistoryService executionHistoryService;
	
	
	@RequestMapping("/")
    public String viewAllExecutions(Model model) {
		List<Execution> allExecutions = executionHistoryService.findAllExecutionsOrdered();
        model.addAttribute("executions", allExecutions);
        return "executions";
    }
	
	@RequestMapping("/{executionId}")
	public String viewExecution(@PathVariable("executionId") String executionId, Model model) {
		Execution execution = executionHistoryService.findExecution(executionId);
		ExecutionMetadata metadata = execution.getExecutionMetadata();
		model.addAttribute("executionId", executionId);
		model.addAttribute("failedClassName", metadata.getFailedClassName());
		model.addAttribute("failedMethodName", metadata.getFailedMethodName());
		model.addAttribute("metadata", metadata);
        return "execution";
	}
	
	@RequestMapping(value = "/{executionId}", method = RequestMethod.POST)
	public String updateExecution(@PathVariable("executionId") String executionId, 
			@ModelAttribute("metadata") ExecutionMetadata metadata, 
			Model model) {
		executionHistoryService.updateExecutionMetadata(executionId, metadata);
		return "execution";
	}
	
}
