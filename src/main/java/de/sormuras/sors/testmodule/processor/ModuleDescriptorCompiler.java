package de.sormuras.sors.testmodule.processor;

import static org.objectweb.asm.Opcodes.ACC_MANDATED;
import static org.objectweb.asm.Opcodes.ACC_MODULE;
import static org.objectweb.asm.Opcodes.ACC_OPEN;
import static org.objectweb.asm.Opcodes.ACC_STATIC_PHASE;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ACC_TRANSITIVE;
import static org.objectweb.asm.Opcodes.V9;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassWriter;

class ModuleDescriptorCompiler {

  /** @param source {@code module-info.java} */
  ModuleDescriptor from(String source) {
    var namePattern = Pattern.compile("(module)\\s+(.+)\\s*\\{.*");
    var nameMatcher = namePattern.matcher(source);
    if (!nameMatcher.find()) {
      throw new IllegalArgumentException("Expected java compilation unit, but got: " + source);
    }
    String name = nameMatcher.group(2).trim();

    var builder = ModuleDescriptor.newOpenModule(name);
    return builder.build();
  }

  byte[] moduleDescriptorToBinary(ModuleDescriptor descriptor) {
    var classWriter = new ClassWriter(0);
    classWriter.visit(V9, ACC_MODULE, "module-info", null, null, null);
    var moduleFlags =
        (descriptor.isOpen() ? ACC_OPEN : 0)
            | ACC_SYNTHETIC; // mark all generated module-info.class as synthetic
    var moduleVersion = descriptor.version().map(Version::toString).orElse(null);
    var mv = classWriter.visitModule(descriptor.name(), moduleFlags, moduleVersion);
    descriptor.packages().forEach(packaze -> mv.visitPackage(packaze.replace('.', '/')));

    descriptor.mainClass().ifPresent(mainClass -> mv.visitMainClass(mainClass.replace('.', '/')));

    descriptor
        .requires()
        .forEach(
            require -> {
              int modifiers =
                  require
                      .modifiers()
                      .stream()
                      .mapToInt(this::modifierToInt)
                      .reduce(0, (a, b) -> a | b);
              mv.visitRequire(require.name(), modifiers, null);
            });
    descriptor
        .exports()
        .forEach(
            export -> {
              int modifiers =
                  export
                      .modifiers()
                      .stream()
                      .mapToInt(this::modifierToInt)
                      .reduce(0, (a, b) -> a | b);
              mv.visitExport(
                  export.source().replace('.', '/'),
                  modifiers,
                  export.targets().toArray(new String[0]));
            });
    descriptor
        .opens()
        .forEach(
            open -> {
              int modifiers =
                  open.modifiers()
                      .stream()
                      .mapToInt(this::modifierToInt)
                      .reduce(0, (a, b) -> a | b);
              mv.visitExport(
                  open.source().replace('.', '/'),
                  modifiers,
                  open.targets().toArray(new String[0]));
            });
    descriptor.uses().forEach(service -> mv.visitUse(service.replace('.', '/')));
    descriptor
        .provides()
        .forEach(
            provide -> {
              mv.visitProvide(
                  provide.service().replace('.', '/'),
                  provide
                      .providers()
                      .stream()
                      .map(name -> name.replace('.', '/'))
                      .toArray(String[]::new));
            });
    mv.visitEnd();
    classWriter.visitEnd();
    return classWriter.toByteArray();
  }

  private int modifierToInt(ModuleDescriptor.Requires.Modifier modifier) {
    switch (modifier) {
      case MANDATED:
        return ACC_MANDATED;
      case SYNTHETIC:
        return ACC_SYNTHETIC;
      case STATIC:
        return ACC_STATIC_PHASE;
      case TRANSITIVE:
        return ACC_TRANSITIVE;
      default:
        throw new IllegalStateException("unknown modifier " + modifier);
    }
  }

  private int modifierToInt(ModuleDescriptor.Exports.Modifier modifier) {
    switch (modifier) {
      case MANDATED:
        return ACC_MANDATED;
      case SYNTHETIC:
        return ACC_SYNTHETIC;
      default:
        throw new IllegalStateException("unknown modifier " + modifier);
    }
  }

  private int modifierToInt(ModuleDescriptor.Opens.Modifier modifier) {
    switch (modifier) {
      case MANDATED:
        return ACC_MANDATED;
      case SYNTHETIC:
        return ACC_SYNTHETIC;
      default:
        throw new IllegalStateException("unknown modifier " + modifier);
    }
  }
}
