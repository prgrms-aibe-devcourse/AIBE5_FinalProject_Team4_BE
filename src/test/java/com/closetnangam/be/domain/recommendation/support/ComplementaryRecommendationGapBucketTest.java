package com.closetnangam.be.domain.recommendation.support;

import com.closetnangam.be.domain.recommendation.support.ComplementaryRecommendationClassificationService.ResolvedClassification;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComplementaryRecommendationGapBucketTest {

    @Test
    void outerVarsityCoach_acceptsOnlyMatchingItemTypes() {
        ResolvedClassification varsity = new ResolvedClassification(
                "OUTER", "VARSITY_JACKET", "MALE", "FALL", List.of(), List.of()
        );
        ResolvedClassification padding = new ResolvedClassification(
                "OUTER", "PADDING", "MALE", "WINTER", List.of(), List.of()
        );

        assertThat(ComplementaryRecommendationGapBucket.OUTER_VARSITY_COACH.accepts(varsity)).isTrue();
        assertThat(ComplementaryRecommendationGapBucket.OUTER_VARSITY_COACH.accepts(padding)).isFalse();
    }

    @Test
    void maleUnisex_acceptsMaleAndUnisexOnly() {
        ResolvedClassification male = new ResolvedClassification(
                "TOP", "SHIRT", "MALE", "SPRING", List.of(), List.of()
        );
        ResolvedClassification female = new ResolvedClassification(
                "TOP", "SHIRT", "FEMALE", "SPRING", List.of(), List.of()
        );

        assertThat(ComplementaryRecommendationGapBucket.MALE_UNISEX.accepts(male)).isTrue();
        assertThat(ComplementaryRecommendationGapBucket.MALE_UNISEX.accepts(female)).isFalse();
    }

    @Test
    void fillOrder_hasFiveBuckets() {
        assertThat(ComplementaryRecommendationGapBucket.fillOrder()).hasSize(5);
    }
}
