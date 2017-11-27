package regressionfinder.core.manipulation;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.java.JavaEntityType;
import ch.uzh.ifi.seal.changedistiller.model.entities.Delete;
import ch.uzh.ifi.seal.changedistiller.model.entities.Insert;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.Update;
import regressionfinder.model.AffectedFile;
import regressionfinder.model.MultiModuleMavenJavaProject;

/*
 * Utility class for applying source code change deltas to original file. 
 * A trivial mock implementation working only for a few hard-coded cases, with no proper exception handling etc. 
 */
public class SourceCodeFileManipulator {
	
	private static Pattern INSIDE_PARENTHESES = Pattern.compile("^\\((.*)\\);$");

	private final AffectedFile file;
	private final MultiModuleMavenJavaProject workingAreaProject, faultyProject;
	private final StringBuilder content;
	private int offset = 0;
	
	public SourceCodeFileManipulator(AffectedFile file, MultiModuleMavenJavaProject workingAreaProject, MultiModuleMavenJavaProject faultyProject) throws IOException {
		this.file = file;
		this.workingAreaProject = workingAreaProject;
		this.faultyProject = faultyProject;
		
        content = new StringBuilder(workingAreaProject.readSourceCode(file.getPath()));
	}

	public void applyChanges() throws IOException {
		file.getChangesInFile().forEach(this::applySourceCodeChange);
		mergeImports();
		workingAreaProject.writeSourceCode(file.getPath(), content.toString());
	}
	
	private void mergeImports() throws IOException {
		List<String> faultyFileLines = Files.readAllLines(faultyProject.findFile(file.getPath()).toPath());
		Set<String> importLines = faultyFileLines.stream().filter(line -> line.trim().startsWith("import ")).collect(Collectors.toSet());
		String importLinesString = "\r\n" + StringUtils.join(importLines, "\r\n");
		
		Scanner scan = new Scanner(content.toString());
		int importsInsertPosition = scan.skip("package.*?;").match().end();
		scan.close();
	
		content.replace(importsInsertPosition, importsInsertPosition, importLinesString);
	}

	private void applySourceCodeChange(SourceCodeChange sourceCodeChange) {
		if (sourceCodeChange instanceof Insert) {
			applySourceCodeChange((Insert) sourceCodeChange);
		} else if (sourceCodeChange instanceof Update) {
			applySourceCodeChange((Update) sourceCodeChange);
		} else if (sourceCodeChange instanceof Delete) {
			applySourceCodeChange((Delete) sourceCodeChange);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	private void applySourceCodeChange(Insert insert) {
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
		case PARENT_INTERFACE_INSERT:
			inline = true;
			if (insert.getPosition() > 0) {
				commaDelimiter = true;
			}
			break;
		default:
			return;
		}
				
		String changedEntityContent = insert.getChangedEntity().getContent();
		if (insert.getChangedEntity().getType() == JavaEntityType.RETURN_STATEMENT) {
			changedEntityContent = "return " + changedEntityContent;
		} 
		
		changedEntityContent = changedEntityContent.replaceAll("<no type> ", "");
		StringBuilder extractedText = appendDelimiters(changedEntityContent, inline, commaDelimiter);
		int startPosition = insert.getChangedEntity().getStartPosition();
		
		content.replace(startPosition + offset, startPosition + offset, extractedText.toString());
		offset += extractedText.length();
	}

	private StringBuilder appendDelimiters(String string, boolean inline, boolean commaDelimiter) {
		StringBuilder result = new StringBuilder();
		result.append(getProperStartDelimiter(inline, commaDelimiter));
		result.append(string);
		result.append(getProperEndDelimiter(inline));
		return result;
	}

	private String getProperStartDelimiter(boolean inline, boolean commaDelimiter) {
		return inline ? (commaDelimiter ? "," : " ") : "\r\n";
	}

	private String getProperEndDelimiter(boolean inline) {
		return inline ? " " : "\r\n";
	}

	private void applySourceCodeChange(Update update) {
		switch (update.getChangeType()) {
		case ATTRIBUTE_TYPE_CHANGE:
		case STATEMENT_UPDATE:
		case CONDITION_EXPRESSION_CHANGE:
		case INCREASING_ACCESSIBILITY_CHANGE:
		case DECREASING_ACCESSIBILITY_CHANGE:
		case PARAMETER_RENAMING:
		case PARAMETER_TYPE_CHANGE:
		case METHOD_RENAMING:
		case RETURN_TYPE_CHANGE:
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

		newStatement = newStatement.replaceAll("<no type> ", "");

		int startPosition = update.getChangedEntity().getStartPosition();
		int changedEntityValueLength = update.getChangedEntity().getEndPosition() - startPosition + 1;
		if (update.getChangeType() == ChangeType.CONDITION_EXPRESSION_CHANGE) {
			if (update.getChangedEntity().getType() == JavaEntityType.IF_STATEMENT) {
				newStatement = "if (" + newStatement + ")";
				Scanner scanner = new Scanner(content.toString().substring(startPosition + offset));
				Pattern ifCondition = Pattern.compile("if \\((.*)\\)");
				String oldStatement = scanner.findInLine(ifCondition);
				scanner.close();
				changedEntityValueLength = oldStatement.length();
			} else {
				newStatement = "for (" + newStatement + ")";
				Scanner scanner = new Scanner(content.toString().substring(startPosition + offset));
				Pattern forCondition = Pattern.compile("for \\((.*)\\)");
				String oldStatement = scanner.findInLine(forCondition);
				scanner.close();
				changedEntityValueLength = oldStatement.length();
			}
		}
		content.replace(startPosition + offset, startPosition + offset + changedEntityValueLength, newStatement);
		offset += newStatement.length() - changedEntityValueLength;
	}

	private void applySourceCodeChange(Delete delete) {
		switch (delete.getChangeType()) {
		case REMOVED_FUNCTIONALITY:
		case REMOVED_OBJECT_STATE:
		case STATEMENT_DELETE:
		case ALTERNATIVE_PART_DELETE:
		case PARAMETER_DELETE:
			break;
		default:
			throw new UnsupportedOperationException();
		}
		
		int startPosition = delete.getChangedEntity().getStartPosition();
		if (delete.getChangedEntity().getType() == JavaEntityType.PARAMETER) {
			startPosition -= (delete.getChangedEntity().getContent().lastIndexOf(" ") + 1);
		}
		
		if (delete.getChangedEntity().getType() == JavaEntityType.METHOD 
				|| delete.getChangedEntity().getType() == JavaEntityType.VARIABLE_DECLARATION_STATEMENT
				|| delete.getChangedEntity().getType() == JavaEntityType.RETURN_STATEMENT
				|| delete.getChangedEntity().getType() == JavaEntityType.FIELD
				|| delete.getChangedEntity().getType() == JavaEntityType.METHOD_INVOCATION
				|| delete.getChangedEntity().getType() == JavaEntityType.ASSIGNMENT
				|| delete.getChangedEntity().getType() == JavaEntityType.PARAMETER) {
			int changedEntityValueLength = delete.getChangedEntity().getEndPosition() - startPosition + 1;

			content.replace(startPosition + offset, startPosition + offset + changedEntityValueLength, "");
			offset -= changedEntityValueLength;
		} else if (delete.getChangedEntity().getType() == JavaEntityType.IF_STATEMENT) {
			String entity = delete.getChangedEntity().getContent();
			Pattern pattern = Pattern.compile("^\\((.*)\\)$");
			Matcher matcher = pattern.matcher(delete.getChangedEntity().getContent());
			if (matcher.matches()) {
				entity = matcher.group(1);
			}
			int oldValueLength = entity.length();

			content.replace(startPosition + offset + "if (".length(), startPosition + offset + "if (".length() + oldValueLength, "false");
			offset -= (oldValueLength - "false".length());
		} else if (delete.getChangedEntity().getType() == JavaEntityType.ELSE_STATEMENT) {
			// ignore for a while
//			int changedEntityValueLength = delete.getChangedEntity().getEndPosition() - startPosition + 1;
//
//			content.replace(startPosition + offset, startPosition + offset + changedEntityValueLength, "{}");
//			offset -= (changedEntityValueLength - "{}".length());
		} else {
			throw new UnsupportedOperationException();
		}
	}

	public static String normalizeEntityValue(String entityValue) {
		String result = entityValue;

		Matcher matcher = INSIDE_PARENTHESES.matcher(result);
		if (matcher.matches()) {
			result = matcher.group(1) + ";";
		}

		return result;
	}
}
