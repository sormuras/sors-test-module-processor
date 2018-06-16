package de.sormuras.sors.testmodule.processor;

import de.sormuras.sors.testmodule.TestModule;
import de.sormuras.sors.testmodule.TestModuleExtender;

import static java.lang.String.format;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;
import static javax.tools.StandardLocation.CLASS_OUTPUT;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

public class TestModuleProcessor extends AbstractProcessor {

  private static final String OPTION_VERBOSE = "de.sormuras.sors.temopro.verbose";

  private int roundCounter = 0;
  private boolean verbose = Boolean.getBoolean(OPTION_VERBOSE);

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(TestModule.class.getCanonicalName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  public Set<String> getSupportedOptions() {
    return Set.of(OPTION_VERBOSE);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
    note("Processing magic round #%d -> %s", roundCounter, round);
    note("Living inside %s", getClass().getModule());
    if (round.processingOver()) {
      return true;
    }

    processAllElementsAnnotatedWithTestModule(round.getElementsAnnotatedWith(TestModule.class));
    roundCounter++;
    return true;
  }

  private void error(Element element, String format, Object... args) {
    processingEnv.getMessager().printMessage(ERROR, format(format, args), element);
  }

  private void note(String format, Object... args) {
    if (!verbose) {
      return;
    }
    processingEnv.getMessager().printMessage(NOTE, format(format, args));
  }

  private void processAllElementsAnnotatedWithTestModule(Set<? extends Element> elements) {
    for (Element testModuleAnnotated : elements) {
      ElementKind kind = testModuleAnnotated.getKind();
      if (!kind.equals(ElementKind.PACKAGE)) {
        error(
            testModuleAnnotated,
            "@TestModule expects a package as target, not %s %s",
            kind,
            testModuleAnnotated);
      }
      note("Processing in enclosing: %s", testModuleAnnotated.getEnclosingElement());
      try {
        processElementAnnotatedWithTestModule((PackageElement) testModuleAnnotated);
      } catch (Exception e) {
        error(testModuleAnnotated, "Processing failed: %s", e);
      }
    }
  }

  private void processElementAnnotatedWithTestModule(PackageElement packageElement) {
    var filer = processingEnv.getFiler();
    var testModule = packageElement.getAnnotation(TestModule.class);
    var packageName = packageElement.getQualifiedName().toString();
    note("Package %s is annotated with: %s", packageName, testModule);
    var extender = new TestModuleExtender();

    if (testModule.compile()) {
      try {
        note("Compiling...");
        var path = Paths.get(testModule.mainModuleDescriptorBinary(), "module-info.class");
        // var moduleClass = filer.createClassFile("module-info.class"); // illegal name...
        var moduleClass = filer.createResource(CLASS_OUTPUT, "", "module-info.class");
        try (var mainStream = Files.newInputStream(path);
            var testStream = moduleClass.openOutputStream()) {
          var mainDescriptor = ModuleDescriptor.read(mainStream);
          var builder = extender.builder(mainDescriptor);
          extender.copyMainModuleDirectives(mainDescriptor, builder);
          extender.extend(testModule, builder);
          var bytes = new ModuleDescriptorCompiler().moduleDescriptorToBinary(builder.build());
          testStream.write(bytes);
        }
      } catch (Exception e) {
        error(packageElement, e.toString());
      }
    } else {
      var testLines = List.of(testModule.value());
      if (testLines.isEmpty()) {
        error(packageElement, "No test module descriptor line?!");
        return;
      }
      if (testModule.merge()) {
        note("Merging main and test module descriptors...");
        var path = Paths.get(testModule.mainModuleDescriptorSource(), "module-info.java");
        try {
          var mainLines = Files.readAllLines(path);
          note("Read main module descriptor (%d lines): `%s`", mainLines.size(), path);
          testLines = extender.mergeSourceLines(mainLines, testLines);
        } catch (IOException e) {
          error(packageElement, "Reading main module descriptor from `%s`: %s", path, e);
          return;
        }
      }
      try {
        note("Printing...%n %s", testLines);
        var file = filer.createResource(StandardLocation.SOURCE_OUTPUT, "", "module-info.java");
        try (PrintStream stream = new PrintStream(file.openOutputStream(), false, "UTF-8")) {
          testLines.forEach(stream::println);
        }
      } catch (Exception e) {
        error(packageElement, e.toString());
      }
    }
  }
}
