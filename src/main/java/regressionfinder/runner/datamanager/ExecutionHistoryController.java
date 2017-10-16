package regressionfinder.runner.datamanager;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import regressionfinder.core.statistics.persistence.StatisticsService;
import regressionfinder.core.statistics.persistence.entities.Execution;

@Controller
public class ExecutionHistoryController {
	
	@Autowired
	private StatisticsService statisticsService;
	

	@RequestMapping("/executions")
    public String viewAllExecutions(Model model) {
		List<Execution> allExecutions = statisticsService.findAllExecutionsOrdered();
        model.addAttribute("executions", allExecutions);
        return "executions";
    }
}
