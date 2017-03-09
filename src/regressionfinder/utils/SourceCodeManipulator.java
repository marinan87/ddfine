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
 * Utility class for applying source code change deltas to original file. 
 * A trivial mock implementation working only for a few hard-coded cases, with no proper exception handling etc. 
 */
public class SourceCodeManipulator {
	
	private static Pattern INSIDE_PARENTHESES = Pattern.compile("^\\(.*\\)$");
	private final IDocument document;
	private int offset = 0;
	
	public SourceCodeManipulator(ICompilationUnit cu) throws Exception {	
		ICompilationUnit copyOfSource = JavaModelHelper.createCopyOfCompilationUnit(cu);
		ITextEditor textEditor = JavaModelHelper.openTextEditor(copyOfSource);
		document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
	}
	
	private void applySourceCodeChanges(List<SourceCodeChange> sourceCodeChanges) throws Exception {
		sourceCodeChanges.sort((o1, o2) -> getStartPosition(o1) - getStartPosition(o2));

		for (SourceCodeChange sourceCodeChange : sourceCodeChanges) {
			applySourceCodeChange(sourceCodeChange);
		}
		
		JavaModelHelper.saveModifiedFiles();
	}

	private int getStartPosition(SourceCodeChange sourceCodeChange) {
		switch (sourceCodeChange.getChangeType()) {
			case STATEMENT_INSERT:
				return 78;
			case STATEMENT_UPDATE:
				return ((Update) sourceCodeChange).getChangedEntity().getStartPosition();
			case REMOVED_FUNCTIONALITY:
				return ((Delete) sourceCodeChange).getChangedEntity().getStartPosition();
			default:
				return -1;
		}
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
				break;
			default:
				break;
		}
	}
	
	private void applyInsertSourceCodeChange(SourceCodeChange sourceCodeChange) throws BadLocationException {
		Insert insert = (Insert) sourceCodeChange;
		
		if (sourceCodeChange.getChangedEntity().getType() == JavaEntityType.VARIABLE_DECLARATION_STATEMENT) {			
			String textToInsert = insert.getChangedEntity().getUniqueName() + TextUtilities.getDefaultLineDelimiter(document);
			// SourceCodeChange provided by ChangeDistiller currently does not contain enough information in order to 
			// apply detected insertions to the original source code file. The insert position is now hard-coded.
			// ChangeDistiller will be updated later. 
			document.replace(78, 0, textToInsert);
			offset += textToInsert.length();
		}		
	}

	private void applyUpdateSourceCodeChange(SourceCodeChange sourceCodeChange) throws BadLocationException {
		Update update = (Update) sourceCodeChange;

		if (update.getChangedEntity().getType() == JavaEntityType.RETURN_STATEMENT) {
			String newStatement = "return " + normalizeEntityValue(update.getNewEntity().getUniqueName());
			int startPosition = update.getChangedEntity().getStartPosition();
			int changedEntityValueLength = update.getChangedEntity().getEndPosition() - startPosition;

			document.replace(startPosition + offset, changedEntityValueLength, newStatement);
			offset += newStatement.length() - changedEntityValueLength; 
		}
	}	
	
	private void applyDeleteSourceCodeChange(SourceCodeChange sourceCodeChange) throws BadLocationException {
		Delete delete = (Delete) sourceCodeChange;

		if (delete.getChangedEntity().getType() == JavaEntityType.METHOD) {
			int startPosition = delete.getChangedEntity().getStartPosition();
			int changedEntityValueLength = delete.getChangedEntity().getEndPosition() - startPosition + 1;
			
			document.replace(startPosition + offset, changedEntityValueLength, "");
			offset -= changedEntityValueLength;
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
		sourceCodeHelper.applySourceCodeChanges(selectedSourceCodeChangeSet);
	}
}
