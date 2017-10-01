package regressionfinder.core.statistics;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TimeTrackingAspect {
	
	@Autowired
	private StatisticsTracker statisticsTracker;
	
	
	@Around("execution(@regressionfinder.core.statistics.LogDuration * *(..)) && @annotation(LogDuration)")
	public Object measureTimeAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
		long startTime = System.currentTimeMillis();
		
		Object result = joinPoint.proceed();
		
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		LogDuration logDurationAnnotation =  method.getAnnotation(LogDuration.class);
		String value = logDurationAnnotation.value().concat(" Took time: %s.");
		
		statisticsTracker.logDuration(value, startTime);
		
		return result;
	}
	
	@Around("execution(@regressionfinder.core.statistics.LogTrialDuration * *(..)) && @annotation(LogTrialDuration)")
	public Object measureTrialTimeAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
		long startTime = System.currentTimeMillis();
		
		Object result = joinPoint.proceed();
				
		statisticsTracker.updateLastTrialMetrics(System.currentTimeMillis() - startTime);
		
		return result;
	}	
}
