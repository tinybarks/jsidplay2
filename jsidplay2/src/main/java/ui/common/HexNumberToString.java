package ui.common;

import javafx.util.StringConverter;

public final class HexNumberToString extends StringConverter<Number> {

	@Override
	public String toString(Number d) {
		return String.format("0x%4x", d);
	}

	@Override
	public Number fromString(String string) {
		return Integer.decode(string);
	}

}