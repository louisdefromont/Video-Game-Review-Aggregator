package me.louisdefromont.vgreviews;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class AdjustedGameScore {
	private double adjustedScore;
	private VideoGame videoGame;
}
