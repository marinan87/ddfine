package regressionfinder.utils;

import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ui.texteditor.ITextEditor;

import ch.uzh.ifi.seal.changedistiller.model.classifiers.java.JavaEntityType;
import ch.uzh.ifi.seal.changedistiller.model.entities.Delete;
import ch.uzh.ifi.seal.changedistiller.model.entities.Insert;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.Update;

/*
 * Helper for manipulating source code file. 
 * A trivial mock implementation working only for a few hard-coded cases, with no proper exception handling etc.
 * Will be completely replaced later with normal logic for applying particular source code change. 
 */
public class SourceCodeManipulator {
	
	private static Pattern INSIDE_PARENTHESES = Pattern.compile("^\\(.*\\)$");
	private final IDocument document;
	private int offset = 0; // temporary workaround
	
	public SourceCodeManipulator(ICompilationUnit cu) throws Exception {	
		ICompilationUnit copyOfSource = JavaModelHelper.createCopyOfCompilationUnit(cu);
		ITextEditor textEditor = JavaModelHelper.openTextEditor(copyOfSource);
		document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
	}
		
	private void applySourceCodeChange(SourceCodeChange sourceCodeChange) throws BadLocationException {
		switch (sourceCodeChange.getChangeType()) {
			case STATEMENT_INSERT:
				applyInsertSourceCodeChange(sourceCodeChange);	
				break;
			case STATEMENT_UPDATE:
				applyUpdateSourceCodeChange(sourceCodeChange);
				break;
			case REMOVED_FUNCTIONALITY:
				applyDeleteSourceCodeChange(sourceCodeChange);
			default:
				return;
		}
	}
	
	private void applyInsertSourceCodeChange(SourceCodeChange sourceCodeChange) throws BadLocationException {
		Insert insert = (Insert) sourceCodeChange;
		
		if (sourceCodeChange.getChangedEntity().getType() == JavaEntityType.VARIABLE_DECLARATION_STATEMENT) {			
			String textToInsert = insert.getChangedEntity().getUniqueName() + TextUtilities.getDefaultLineDelimiter(document);
			document.replace(78, 0, textToInsert);
			offset += textToInsert.length(); // temporary workaround
		}		
	}

	private void applyUpdateSourceCodeChange(SourceCodeChange sourceCodeChange) throws BadLocationException {
		Update update = (Update) sourceCodeChange;

		if (update.getChangedEntity().getType() == JavaEntityType.RETURN_STATEMENT) {
			String newStatement = "return " + normalizeEntityValue(update.getNewEntity().getUniqueName());
			int startPosition = update.getChangedEntity().getStartPosition();
			int changedEntityValueLength = update.getChangedEntity().getEndPosition() - startPosition;
			
			document.replace(startPosition + offset, changedEntityValueLength, newStatement);
			offset += newStatement.length() - changedEntityValueLength; // temporary workaround
		}
	}	
	
	private void applyDeleteSourceCodeChange(SourceCodeChange sourceCodeChange) throws BadLocationException {
		Delete delete = (Delete) sourceCodeChange;

		if (delete.getChangedEntity().getType() == JavaEntityType.METHOD) {
			int startPosition = delete.getChangedEntity().getStartPosition();
			int changedEntityValueLength = delete.getChangedEntity().getEndPosition() - startPosition + 1;
			
			document.replace(startPosition + offset, changedEntityValueLength, "");
//			offset -= changedEntityValueLength; // temporary workaround
		}
	}
	
	private String normalizeEntityValue(String entityValue) {
		String result = "";
		if (entityValue.charAt(entityValue.length() - 1) == ';') {
			result = entityValue.substring(0, entityValue.length() - 1);
		}
		
		if (INSIDE_PARENTHESES.matcher(result).matches()) {
			result = result.substring(1, result.length() - 1);
		}
		
		return result;
	}
	
	public static void copyAndModifyLocalizationSource(ICompilationUnit sourceCU, List<SourceCodeChange> selectedSourceCodeChangeSet) throws Exception {
		SourceCodeManipulator sourceCodeHelper = new SourceCodeManipulator(sourceCU);
		
		// SourceCodeChange provided by ChangeDistiller currently does not contain enough information in order to 
		// apply detected insertions to the original source code file. ChangeDistiller will be updated later. 
		for (SourceCodeChange change : selectedSourceCodeChangeSet) {
			sourceCodeHelper.applySourceCodeChange(change);
		}

		JavaModelHelper.saveModifiedFiles();
	}
}
