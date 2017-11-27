package regressionfinder.core;

import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.ADDITIONAL_CLASS;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.ADDITIONAL_FUNCTIONALITY;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.ADDITIONAL_OBJECT_STATE;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.ALTERNATIVE_PART_DELETE;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.ATTRIBUTE_TYPE_CHANGE;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.CONDITION_EXPRESSION_CHANGE;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.DECREASING_ACCESSIBILITY_CHANGE;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.INCREASING_ACCESSIBILITY_CHANGE;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.METHOD_RENAMING;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.PARAMETER_DELETE;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.PARAMETER_INSERT;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.PARAMETER_RENAMING;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.PARAMETER_TYPE_CHANGE;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.PARENT_INTERFACE_INSERT;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.REMOVED_FUNCTIONALITY;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.REMOVED_OBJECT_STATE;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.REMOVING_ATTRIBUTE_MODIFIABILITY;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.REMOVING_CLASS_DERIVABILITY;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.REMOVING_METHOD_OVERRIDABILITY;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.RETURN_TYPE_CHANGE;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.STATEMENT_DELETE;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.STATEMENT_INSERT;
import static ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType.STATEMENT_UPDATE;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType;
import ch.uzh.ifi.seal.changedistiller.model.entities.Delete;
import ch.uzh.ifi.seal.changedistiller.model.entities.Insert;
import ch.uzh.ifi.seal.changedistiller.model.entities.Move;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.Update;

public class SupportedModificationsRegistry {
	
	private static final Map<Class<? extends SourceCodeChange>, List<ChangeType>> SUPPORTED_MODIFICATIONS = 
			ImmutableMap.<Class<? extends SourceCodeChange>, List<ChangeType>>builder()
				.put(Insert.class, newArrayList(
						STATEMENT_INSERT, REMOVING_CLASS_DERIVABILITY, REMOVING_METHOD_OVERRIDABILITY, REMOVING_ATTRIBUTE_MODIFIABILITY,
						INCREASING_ACCESSIBILITY_CHANGE, DECREASING_ACCESSIBILITY_CHANGE, ADDITIONAL_FUNCTIONALITY, ADDITIONAL_OBJECT_STATE,
						ADDITIONAL_CLASS, PARAMETER_INSERT, PARENT_INTERFACE_INSERT))
				.put(Update.class, newArrayList(
						STATEMENT_UPDATE, INCREASING_ACCESSIBILITY_CHANGE, DECREASING_ACCESSIBILITY_CHANGE, PARAMETER_RENAMING, PARAMETER_TYPE_CHANGE, 
						METHOD_RENAMING, CONDITION_EXPRESSION_CHANGE, ATTRIBUTE_TYPE_CHANGE, RETURN_TYPE_CHANGE))
				.put(Delete.class, newArrayList(REMOVED_FUNCTIONALITY, STATEMENT_DELETE, ALTERNATIVE_PART_DELETE, REMOVED_OBJECT_STATE, PARAMETER_DELETE))
				.put(Move.class, newArrayList())
				.build();
			
	public static boolean supportsModification(Class<? extends SourceCodeChange> changeClass, ChangeType changeType) {
		return SUPPORTED_MODIFICATIONS.get(changeClass).contains(changeType);
	}
}
