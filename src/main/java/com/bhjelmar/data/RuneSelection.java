package com.bhjelmar.data;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.jsoup.nodes.Element;

import java.util.List;

@Value
@AllArgsConstructor
public class RuneSelection {

	Element element;
	List<String> runes;
	double pickRate;
	double winRate;

}
