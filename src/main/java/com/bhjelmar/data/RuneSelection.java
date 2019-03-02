package com.bhjelmar.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jsoup.nodes.Element;

import java.util.List;

@Data
@AllArgsConstructor
public class RuneSelection {

	Element element;
	List<String> runes;
	double pickRate;
	double winRate;

}
