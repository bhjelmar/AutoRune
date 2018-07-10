package sample.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data @AllArgsConstructor
public class Rune implements Serializable {

	private String name;
	private int id;

}
