module de.sormuras.sors.testmodule {
  requires java.compiler;
  requires static org.objectweb.asm;

  exports de.sormuras.sors.testmodule;

  provides javax.annotation.processing.Processor with
      de.sormuras.sors.testmodule.processor.TestModuleProcessor;
}
