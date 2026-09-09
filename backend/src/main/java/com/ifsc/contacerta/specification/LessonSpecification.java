package com.ifsc.contacerta.specification;

import com.ifsc.contacerta.entity.Lesson;
import com.ifsc.contacerta.model.ContentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public final class LessonSpecification {

	private LessonSpecification() {
	}

	/** Acervo do professor, filtrável por situação e por busca em título e resumo. */
	public static Specification<Lesson> ownedBy(UUID teacherId, String search, ContentStatus status) {
		return (root, query, criteriaBuilder) -> {
			var predicates = new ArrayList<Predicate>();
			predicates.add(criteriaBuilder.equal(root.get("teacher").get("id"), teacherId));

			if (search != null && !search.isBlank()) {
				String normalizedSearch = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), normalizedSearch),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("summary")), normalizedSearch)
				));
			}
			if (status != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}

			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}
}
