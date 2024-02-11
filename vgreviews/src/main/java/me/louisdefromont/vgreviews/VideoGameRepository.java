package me.louisdefromont.vgreviews;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface VideoGameRepository extends CrudRepository<VideoGame, Long> {
	@Query("SELECT vg FROM VideoGame vg JOIN vg.reviews r WHERE r.sourceURL = :source")
    VideoGame findByReviewSource(@Param("source") String source);

	List<VideoGame> findByTitleContainingIgnoreCase(String title);
	VideoGame findByTitle(String title);
}
