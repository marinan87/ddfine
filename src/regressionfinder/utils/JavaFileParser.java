package regressionfinder.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

/*
 * Currently not used.
 */
public class JavaFileParser {

	public static ASTNode parseJavaFile(String path) throws IOException {
		try (FileReader reader = new FileReader(path)) {
			return parse(reader);
		} 
	}

	private static ASTNode parse(Reader r) throws IOException {
		ASTParser parser = createParser();
		parser.setSource(readerToCharArray(r));
		return parser.createAST(null);
	}
	
	private static ASTParser createParser() {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		Map<String, String> pOptions = JavaCore.getOptions();
		pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
		parser.setCompilerOptions(pOptions);
		parser.setResolveBindings(true);
		return parser;
	}

	private static char[] readerToCharArray(Reader r) throws IOException {
		StringBuilder fileData = new StringBuilder();
		try (BufferedReader br = new BufferedReader(r)) {
			char[] buf = new char[10];
			int numRead = 0;
			while ((numRead = br.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[1024];
			}
		}
		return fileData.toString().toCharArray();
	}
	
	public static CompilationUnit parseCompilationUnit(ICompilationUnit unit) {
		ASTParser parser = createParser();
		parser.setSource(unit);
		return (CompilationUnit) parser.createAST(null);
	}
}
