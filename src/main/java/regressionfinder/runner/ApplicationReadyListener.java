package regressionfinder.runner;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import regressionfinder.core.EvaluationContext;

public class ApplicationReadyListener implements ApplicationListener<ApplicationReadyEvent> {

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		event.getApplicationContext().getBean(EvaluationContext.class).initOnce();
	}
}
