package com.gestudio.crm.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

  private final JavaClasses classes = new ClassFileImporter().importPackages("com.gestudio.crm");

  @Test
  void controllersDoNotAccessRepositoriesDirectly() {
    noClasses()
        .that()
        .haveSimpleNameEndingWith("Controller")
        .should()
        .dependOnClassesThat()
        .haveSimpleNameEndingWith("Repository")
        .check(classes);
  }
}
