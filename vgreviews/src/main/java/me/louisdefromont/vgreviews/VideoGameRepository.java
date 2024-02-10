package me.louisdefromont.vgreviews;

import org.springframework.data.repository.CrudRepository;

public interface VideoGameRepository extends CrudRepository<VideoGame, Long> {
	
}
