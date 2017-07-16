package regressionfinder.core;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.deltadebugging.ddcore.DeltaSet;
import org.deltadebugging.ddcore.tester.JUnitTester;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.isolatedrunner.DeltaDebuggerTestRunner;
import regressionfinder.isolatedrunner.IsolatedClassLoaderAwareJUnitTestRunner;
import regressionfinder.isolatedrunner.IsolatedURLClassLoader;
import regressionfinder.isolatedrunner.JUnitTestRunner;
import regressionfinder.isolatedrunner.MethodDescriptor;

public class DeltaSetEvaluator extends JUnitTester {
	
	private final Throwable throwable;
	private final EvaluationContext evaluationContext;
	
	public DeltaSetEvaluator(EvaluationContext evaluationContext) {
		super();
		this.evaluationContext = evaluationContext;
		throwable = obtainOriginalStacktrace();
	}
	
	private Throwable obtainOriginalStacktrace() {
		// TODO: move to EvaluationContext
		
		SourceCodeManipulator.copyToStagingAreaWithModifications(evaluationContext.getWorkingArea(), evaluationContext.getFaultyVersion(), new ArrayList<>());
		
		return (Throwable) runMethodInIsolatedTestRunner(JUnitTestRunner.class, 
				new MethodDescriptor("getOriginalException", null, null));
	}
	
	@Override
	public int test(DeltaSet c) {
		@SuppressWarnings("unchecked")
		List<SourceCodeChange> selectedChangeSet = (List<SourceCodeChange>) c.stream().collect(toList());
		return testSelectedChangeSet(selectedChangeSet); 
	}
	
	private int testSelectedChangeSet(List<SourceCodeChange> selectedSourceCodeChangeSet) {
		// TODO: sourcecodemanipulator, deltasetevaluator - singleton beans
		SourceCodeManipulator.copyToStagingAreaWithModifications(evaluationContext.getWorkingArea(), evaluationContext.getReferenceVersion(), selectedSourceCodeChangeSet);
		
		return (int) runMethodInIsolatedTestRunner(DeltaDebuggerTestRunner.class, 
				new MethodDescriptor("runTest", new Class<?>[] { Throwable.class }, new Object[] { throwable }));
	}
	
	private <T extends IsolatedClassLoaderAwareJUnitTestRunner> Object runMethodInIsolatedTestRunner(Class<T> clazz, MethodDescriptor methodDescriptor) {
		try (IsolatedURLClassLoader isolatedClassLoader = new IsolatedURLClassLoader(evaluationContext.getClassPathURLs())) {
			Class<?> runnerClass = isolatedClassLoader.loadClass(clazz.getName());
			Constructor<?> constructor = runnerClass.getConstructor(String.class, String.class);
			
			Object isolatedTestRunner = constructor.newInstance(evaluationContext.getTestClassName(), evaluationContext.getTestMethodName());
		
			Method method = isolatedTestRunner.getClass().getMethod(methodDescriptor.getMethodName(), methodDescriptor.getParameterTypes());
			return method.invoke(isolatedTestRunner, methodDescriptor.getArgs());			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
