package regressionfinder.utils;

import java.util.List;
import java.util.regex.Matcher;
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
	
	private static Pattern INSIDE_PARENTHESES = Pattern.compile("^\\((.*)\\);$");
	private final IDocument document;
	private int offset = 0;
	
	public SourceCodeManipulator(ICompilationUnit cu) throws Exception {	
		ICompilationUnit copyOfSource = JavaModelHelper.copyCompilationUnitToStagingArea(cu);
		ITextEditor textEditor = JavaModelHelper.openTextEditor(copyOfSource);
		document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
	}
	
	private void applySourceCodeChanges(List<SourceCodeChange> sourceCodeChanges) throws Exception {
		sourceCodeChanges.sort((o1, o2) -> {
			int firstStartPosition = o1.getChangedEntity().getStartPosition();
			int secondStartPosition = o2.getChangedEntity().getStartPosition();	
			
			if (firstStartPosition == secondStartPosition && o1 instanceof Insert && o2 instanceof Insert) {
				return ((Insert) o1).getPosition() - ((Insert) o2).getPosition();
			} 
			return firstStartPosition - secondStartPosition;
		});

		for (SourceCodeChange sourceCodeChange : sourceCodeChanges) {
			applySourceCodeChange(sourceCodeChange);
		}
		
		JavaModelHelper.saveModifiedFiles();
	}
	
	private void applySourceCodeChange(SourceCodeChange sourceCodeChange) throws BadLocationException {
		if (sourceCodeChange instanceof Insert) {
			applySourceCodeChange((Insert) sourceCodeChange);
		} else if (sourceCodeChange instanceof Update) {
			applySourceCodeChange((Update) sourceCodeChange);
		} else if (sourceCodeChange instanceof Delete) {
			applySourceCodeChange((Delete) sourceCodeChange);
		} else {
			throw new UnsupportedOperationException(); // TODO: Move operation?
		}
	}
	
	private void applySourceCodeChange(Insert insert) throws BadLocationException {
		// TODO: refactor
		boolean inline = false, commaDelimiter = false;
		switch (insert.getChangeType()) {
			case STATEMENT_INSERT:
				inline = false;
				break;
			case REMOVING_CLASS_DERIVABILITY:
				inline = true;
				break;
			case REMOVING_METHOD_OVERRIDABILITY:
				inline = true;
				break;
			case REMOVING_ATTRIBUTE_MODIFIABILITY:
				inline = true;
				break;
			case INCREASING_ACCESSIBILITY_CHANGE:
				inline = true;
				break;
			case DECREASING_ACCESSIBILITY_CHANGE:
				inline = true;
				break;
			case ADDITIONAL_FUNCTIONALITY:
				inline = false;
				break;
			case ADDITIONAL_OBJECT_STATE:
				inline = false;
				break;
			case ADDITIONAL_CLASS:
				inline = false;
				break;
			case PARAMETER_INSERT:
				inline = true;
				if (insert.getPosition() > 0) {
					commaDelimiter = true;
				}
				break;
			default:
				return;		
		}
		
		StringBuilder textToInsert = appendDelimiters(insert.getChangedEntity().getContent(), inline, commaDelimiter); 
		int startPosition = insert.getChangedEntity().getStartPosition();
		
		document.replace(startPosition + offset, 0, textToInsert.toString());
		offset += textToInsert.length();
	}
	
	private StringBuilder appendDelimiters(String string, boolean inline, boolean commaDelimiter) {
		StringBuilder result = new StringBuilder();
		result.append(getProperStartDelimiter(inline, commaDelimiter));
		result.append(string);
		result.append(getProperEndDelimiter(inline));
		return result;
	}
	
	private String getProperStartDelimiter(boolean inline, boolean commaDelimiter) {
		return inline 
				? (commaDelimiter ? "," : " ")
				: TextUtilities.getDefaultLineDelimiter(document);
	}
	
	private String getProperEndDelimiter(boolean inline) {
		return inline ? " " : TextUtilities.getDefaultLineDelimiter(document);
	}

	private void applySourceCodeChange(Update update) throws BadLocationException {
		switch (update.getChangeType()) {
			case STATEMENT_UPDATE:
			case INCREASING_ACCESSIBILITY_CHANGE:
			case DECREASING_ACCESSIBILITY_CHANGE:
			case PARAMETER_RENAMING:
			case PARAMETER_TYPE_CHANGE:
				break;
			default:
				throw new UnsupportedOperationException();
		}
		
		String newStatement = "";
		if (update.getChangedEntity().getType() == JavaEntityType.RETURN_STATEMENT) {
			newStatement = "return "; 
		} 
		
		if (update.getChangedEntity().getType() != JavaEntityType.PARAMETER) {
			newStatement += normalizeEntityValue(update.getNewEntity().getContent());
		} else {
			newStatement = update.getNewEntity().getUniqueName();
		}
		
		int startPosition = update.getChangedEntity().getStartPosition();
		int changedEntityValueLength = update.getChangedEntity().getEndPosition() - startPosition + 1;
		document.replace(startPosition + offset, changedEntityValueLength, newStatement);
		offset += newStatement.length() - changedEntityValueLength; 
	}	
	
	private void applySourceCodeChange(Delete delete) throws BadLocationException {
		switch (delete.getChangeType()) {
			case REMOVED_FUNCTIONALITY:
				break;
			default:
				throw new UnsupportedOperationException();
		}
		
		if (delete.getChangedEntity().getType() == JavaEntityType.METHOD) {
			int startPosition = delete.getChangedEntity().getStartPosition();
			int changedEntityValueLength = delete.getChangedEntity().getEndPosition() - startPosition + 1;
			
			document.replace(startPosition + offset, changedEntityValueLength, "");
			offset -= changedEntityValueLength;
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	private String normalizeEntityValue(String entityValue) {
		String result = entityValue;
	
		Matcher matcher = INSIDE_PARENTHESES.matcher(result);
		if (matcher.matches()) {
			result = matcher.group(1) + ";";
		}
		
		return result;
	}
	
	public static void copyToStagingAreaWithModifications(ICompilationUnit sourceCU, List<SourceCodeChange> selectedSourceCodeChangeSet) {
		try {
			SourceCodeManipulator sourceCodeHelper = new SourceCodeManipulator(sourceCU);
			sourceCodeHelper.applySourceCodeChanges(selectedSourceCodeChangeSet);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
