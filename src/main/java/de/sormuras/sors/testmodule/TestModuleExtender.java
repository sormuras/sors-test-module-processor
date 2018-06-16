package de.sormuras.sors.testmodule;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Builder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TestModuleExtender implements Consumer<Builder> {

  @Override
  public void accept(Builder builder) {
      // builder.requires("org.junit.jupiter.api");
  }

  public Builder builder(ModuleDescriptor mainDescriptor) {
    return ModuleDescriptor.newOpenModule(mainDescriptor.name());
  }

  public Builder copyMainModuleDirectives(ModuleDescriptor mainDescriptor, Builder builder) {
    mainDescriptor.requires().forEach(builder::requires);
    mainDescriptor.exports().forEach(builder::exports);
    mainDescriptor.opens().forEach(builder::opens);
    mainDescriptor.uses().forEach(builder::uses);
    mainDescriptor.provides().forEach(builder::provides);
    return builder;
  }

  public List<String> mergeSourceLines(List<String> mainLines, List<String> testLines) {
    var mergedLines = new ArrayList<String>();
    for (var line : mainLines) {
      if (line.contains("}")) {
        mergedLines.add("  // BEGIN");
        mergedLines.addAll(testLines);
        mergedLines.add("  // END.");
      }
      mergedLines.add(line.replace("module ", "open module "));
    }
    return List.copyOf(mergedLines);
  }
}
