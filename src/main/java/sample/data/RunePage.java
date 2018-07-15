package sample.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RunePage {

	@JsonProperty
	List<String> autoModifiedSelections;
	@JsonProperty
	boolean current;
	@JsonProperty
	long id;
	@JsonProperty
	boolean isActive;
	@JsonProperty
	boolean isDeletable;
	@JsonProperty
	boolean isEditable;
	@JsonProperty
	boolean isValid;
	@JsonProperty
	long lastModified;
	@JsonProperty
	String name;
	@JsonProperty
	int order;
	@JsonProperty
	int primaryStyleId;
	@JsonProperty
	List<Integer> selectedPerkIds;
	@JsonProperty
	int subStyleId;

}
