package regressionfinder.isolatedrunner;

public class MethodDescriptor {
	private final String methodName;
	private final Class<?>[] parameterTypes;
	private final Object[] args;
	
	public MethodDescriptor(String methodName) {
		this(methodName, null, null);
	}
	
	public MethodDescriptor(String methodName, Class<?>[] parameterTypes, Object[] args) {
		this.methodName = methodName;
		this.parameterTypes = parameterTypes;
		this.args = args;
	}
	
	public String getMethodName() {
		return methodName;
	}

	public Class<?>[] getParameterTypes() {
		return parameterTypes;
	}
	
	public Object[] getArgs() {
		return args;
	}
}
