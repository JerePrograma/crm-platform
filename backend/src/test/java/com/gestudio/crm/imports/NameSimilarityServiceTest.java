package com.gestudio.crm.imports;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NameSimilarityServiceTest {

  private final NameSimilarityService service = new NameSimilarityService();

  @Test
  void identifiesLikelyTypographicalVariation() {
    assertThat(service.similarity("estudio aurora", "estudio auroa"))
        .isGreaterThanOrEqualTo(0.80d)
        .isLessThan(1d);
  }

  @Test
  void doesNotConfuseDifferentNumericBranches() {
    assertThat(service.similarity("institucion fixture 001", "institucion fixture 002"))
        .isLessThan(0.80d);
  }
}
