package de.sormuras.sors.testmodule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to declare a test module descriptor on-the-fly.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PACKAGE)
public @interface TestModule {

  /** @return the directory where the main {@code module-info.java} file is located */
  String mainModuleDescriptorPath() default "src/main/java";

  /** @return {@code true} to merge with lines from main module descriptor */
  boolean merge() default true;

  /** @return lines of the test module descriptor */
  String[] value() default {};
}
