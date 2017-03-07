package regressionfinder.utils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.ui.PlatformUI;

import ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.java.JavaEntityType;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.Update;

/*
 * Helper for manipulating AST tree of compilation unit. 
 * A trivial mock implementation working only for a few hard-coded cases.
 * Will be completely replaced later with normal logic for applying particular 
 * source code change (or AST node change - depending on the finally chosen approach). 
 */
public class DOMHelper {
	
	private final ICompilationUnit copyOfOriginal;
	private final CompilationUnit cu;
	private final ASTRewrite rewriter;
	
	public DOMHelper(ICompilationUnit copyOfOriginal) {
		this.copyOfOriginal = copyOfOriginal;
		this.cu = parse(copyOfOriginal);
		this.rewriter = ASTRewrite.create(cu.getAST());
	}
	
	private CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}
	
	private void applyChangeToCopyOfOriginal(SourceCodeChange sourceCodeChange) {
		if (sourceCodeChange.getChangeType() == ChangeType.STATEMENT_INSERT) {
			applyASTInsertStatementModification(sourceCodeChange);			
		} else if (sourceCodeChange.getChangeType() == ChangeType.STATEMENT_UPDATE) {
			applyASTUpdateStatementModification(sourceCodeChange);
		}
	}

	private void applyASTInsertStatementModification(SourceCodeChange sourceCodeChange) {
		Block targetMethodBlock = findTargetMethodBlock(sourceCodeChange);
		if (targetMethodBlock == null) {
			return;
		}
		
		if (sourceCodeChange.getChangedEntity().getType() == JavaEntityType.VARIABLE_DECLARATION_STATEMENT) {
			VariableDeclarationFragment fragment = cu.getAST().newVariableDeclarationFragment();
			fragment.setName(cu.getAST().newSimpleName("factor"));
			fragment.setInitializer(cu.getAST().newNumberLiteral("12"));
			VariableDeclarationStatement statement = cu.getAST().newVariableDeclarationStatement(fragment);
			ListRewrite statementsListRewrite = rewriter.getListRewrite(targetMethodBlock, Block.STATEMENTS_PROPERTY);
			statementsListRewrite.insertAt(statement, 0, null);
		}		
	}

	private Block findTargetMethodBlock(SourceCodeChange sourceCodeChange) {
		String parentEntityName = sourceCodeChange.getParentEntity().getUniqueName();
		String[] parentNameParts = parentEntityName.split("\\.");
		
		TypeDeclaration typeDec = null;
		for (Object typeDecObj : cu.types()) {
			typeDec = (TypeDeclaration) typeDecObj;
			if (typeDec.getName().getIdentifier().equals(parentNameParts[1])) {
				break;
			}
		}
		if (typeDec == null) {
			return null;
		}
		
		Optional<MethodDeclaration> targetMethod = Stream.of(typeDec.getMethods())
			.filter(method -> method.getName().getIdentifier().equals(parentNameParts[2].substring(0, parentNameParts[2].indexOf("("))))
			.findFirst();
		if (!targetMethod.isPresent()) {
			return null;
		}
		
		return targetMethod.get().getBody();
	}
	
	private void applyASTUpdateStatementModification(SourceCodeChange sourceCodeChange) {
		Block targetMethodBlock = findTargetMethodBlock(sourceCodeChange);
		if (targetMethodBlock == null) {
			return;
		}
		
		if (sourceCodeChange.getChangedEntity().getType() == JavaEntityType.RETURN_STATEMENT) {			
			ReturnStatement oldReturnStatement = (ReturnStatement) targetMethodBlock.statements().get(targetMethodBlock.statements().size() - 1);
			InfixExpression oldExpression = (InfixExpression) oldReturnStatement.getExpression();
			Expression oldRightOperand = oldExpression.getRightOperand();
			
			ReturnStatement returnStatement = cu.getAST().newReturnStatement();
			InfixExpression expression = cu.getAST().newInfixExpression();
			expression.setOperator(Operator.TIMES);
			SimpleName paramName = cu.getAST().newSimpleName("param");
			if (((Update) sourceCodeChange).getNewEntity().getUniqueName().contains("factor")) {
				expression.setLeftOperand(paramName);
				expression.setRightOperand(cu.getAST().newSimpleName("factor"));
			} else {
				expression.setLeftOperand((oldRightOperand instanceof NumberLiteral) 
					? cu.getAST().newNumberLiteral(((NumberLiteral) oldRightOperand).getToken()) 
					: cu.getAST().newSimpleName(((SimpleName) oldRightOperand).getIdentifier()));
				expression.setRightOperand(paramName);
			}
			returnStatement.setExpression(expression);
			ListRewrite statementsListRewrite = rewriter.getListRewrite(targetMethodBlock, Block.STATEMENTS_PROPERTY);
			statementsListRewrite.replace(oldReturnStatement, returnStatement, null);
		}	
	}	
	
	private void saveChangesToFile() throws JavaModelException {
		try {
			copyOfOriginal.becomeWorkingCopy(new NullProgressMonitor());
			copyOfOriginal.applyTextEdit(rewriter.rewriteAST(), new NullProgressMonitor());
			copyOfOriginal.commitWorkingCopy(false, new NullProgressMonitor());  
			copyOfOriginal.makeConsistent(new NullProgressMonitor());
		   
//	(changes will only be saved if file is currently opened in editor)	
//			Document document = new Document(unit.getSource());
//			TextEdit edits = rewriter.rewriteAST(document, unit.getJavaProject().getOptions(true));
//			edits.apply(document);
//			unit.getBuffer().setContents(document.get());
			
			PlatformUI.getWorkbench().saveAllEditors(false);
			
			IJobManager jobManager = Job.getJobManager();
			jobManager.join(ResourcesPlugin.FAMILY_AUTO_BUILD, new NullProgressMonitor());
			jobManager.join(ResourcesPlugin.FAMILY_AUTO_REFRESH, new NullProgressMonitor());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void copyAndModifyLocalizationSource(ICompilationUnit sourceCU, String fileName,
			List<SourceCodeChange> selectedSourceCodeChangeSet) throws Exception {
		ICompilationUnit copyOfSource = JavaModelHelper.createCopyOfCompilationUnit(sourceCU, fileName);
		
		DOMHelper domHelper = new DOMHelper(copyOfSource);
		// SourceCodeChange provided by ChangeDistiller currently does not contain enough information in order to 
		// apply detected source code change to the original file. Nor does it contain enough data to manipulate source AST tree. 
		// ChangeDistiller uses stand-alone version of Eclipse compiler (ECJ), but ASTRewrite requires using jdt.core.dom API.
		// https://bitbucket.org/sealuzh/tools-changedistiller/issues/31/choice-of-java-parser
		//
		// In applyChangeToCopyOfOriginal, only a couple hard-coded examples are implemented.
		selectedSourceCodeChangeSet.forEach(domHelper::applyChangeToCopyOfOriginal);
		domHelper.saveChangesToFile();
	}
}
