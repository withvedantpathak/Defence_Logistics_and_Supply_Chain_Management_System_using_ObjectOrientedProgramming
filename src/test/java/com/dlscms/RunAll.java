package com.dlscms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/** Discovers every compiled *Test class under a directory and runs them with JUnit. */
public final class RunAll {
    private RunAll() { }

    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args.length > 0 ? args[0] : "out/test-classes");
        List<Class<?>> classes;
        try (Stream<Path> s = Files.walk(root)) {
            classes = s.filter(p -> p.getFileName().toString().endsWith("Test.class"))
                    .map(p -> root.relativize(p).toString().replace('\\', '.').replace('/', '.'))
                    .map(n -> n.substring(0, n.length() - ".class".length()))
                    .sorted()
                    .map(RunAll::load)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        Result r = new JUnitCore().run(classes.toArray(new Class<?>[0]));
        for (Failure f : r.getFailures()) {
            System.out.println("FAIL: " + f.getTestHeader() + " -> " + f.getMessage());
        }
        System.out.printf("TESTS: run=%d failed=%d ignored=%d%n",
                r.getRunCount(), r.getFailureCount(), r.getIgnoreCount());
        System.exit(r.wasSuccessful() ? 0 : 1);
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
