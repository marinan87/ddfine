package regressionfinder.manipulation;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.springframework.stereotype.Service;

@Service
public class MavenCompiler {

	public void triggerCompilation(File pomFile) {
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(pomFile);
		request.setGoals(Arrays.asList("compile"));
		request.setThreads("1C");
		request.setMavenOpts("-XX:+TieredCompilation -XX:TieredStopAtLevel=1");

		Invoker invoker = new DefaultInvoker();
		invoker.setMavenHome(new File(System.getenv("MAVEN_HOME")));
		try {
			invoker.execute(request);
		} catch (MavenInvocationException e) {
			throw new RuntimeException(e);
		}
		// TODO: run incremental build
	}
}
