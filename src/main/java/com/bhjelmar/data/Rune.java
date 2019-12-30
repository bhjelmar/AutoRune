package com.bhjelmar.data;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.io.Serializable;

@Value
@AllArgsConstructor
public class Rune implements Serializable {

	private static final long serialVersionUID = 730445074778670707L;

	private String name;
	private int id;
	private String imgUrl;

}
