package pl.touk.widerest.base;

import javaslang.control.Try;

public abstract class ApiBaseBehaviour {
    protected <R> void when(Try.CheckedSupplier<R> r, Try.CheckedConsumer<R>... thens) throws Throwable {
        R result = r.get();
        for (Try.CheckedConsumer<R> then : thens) {
            then.accept(result);
        }
    }
}
