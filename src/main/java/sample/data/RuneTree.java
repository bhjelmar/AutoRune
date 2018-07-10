package sample.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class RuneTree implements Serializable {

	private String name;
	private int id;
	private List<Rune> runes;

}
