package io.compute;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static java.lang.annotation.ElementType.*;

public class RetryDemo {
    public static void main(String[] args) {
        AdderImpl adder = new AdderImpl();
        Adder adderProxy = new FunctionRetry<Adder>().apply(adder);
        try {
            adderProxy.add(3);
            System.out.println(adder.sum);
        } catch (Exception e) {
            System.out.println(adder.sum);
            // e.printStackTrace();
        }
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(value = {CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, PACKAGE, PARAMETER, TYPE})
    public @interface Retry {
        int times() default 1;
    }

    interface Adder {

        @Retry(times = 10)
        int add(int delta);

        int add0(int delta);
    }

    static class AdderImpl implements Adder {
        private int sum = 0;

        @Override
        public int add(int delta) {
            this.sum += delta;
            throw new RuntimeException();
        }

        @Override
        public int add0(int delta) {
            return sum += delta;
        }
    }

    /**
     * Function is interface.
     *
     * @param <Function>
     */
    static class FunctionRetry<Function> {
        Function apply(Function function) {
            Object object = Proxy.newProxyInstance(
                    function.getClass().getClassLoader(),
                    function.getClass().getInterfaces(),
                    new FunctionDecorator(function)
            );
            return (Function) object;
        }
    }

    static class FunctionDecorator implements InvocationHandler {
        private Object function;

        public FunctionDecorator(Object function) {
            this.function = function;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Retry ann = method.getDeclaredAnnotation(Retry.class);
            if (ann == null) {
                return method.invoke(this.function, args);
            }

            RuntimeException e0 = new RuntimeException();
            for (int i = 0; i < ann.times(); i++) {
                try {
                    return method.invoke(function, args);
                } catch (Throwable e) {
                    e0 = new RuntimeException(e);
                }
            }
            throw e0;
        }
    }
}