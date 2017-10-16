package regressionfinder.runner.datamanager;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import regressionfinder.core.statistics.persistence.entities.Execution;

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
		model.addAttribute("executionId", executionId);
		model.addAttribute("metadata", execution.getExecutionMetadata());
        return "execution";
	}
}
