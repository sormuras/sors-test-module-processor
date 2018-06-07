module de.sormuras.sors.temopro {
  requires java.compiler;

  exports de.sormuras.sors.temopro;

  provides javax.annotation.processing.Processor with
      de.sormuras.sors.temopro.TestModuleProcessor;
}
