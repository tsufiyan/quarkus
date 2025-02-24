package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

/**
 * A simple stateful {@link javax.interceptor.InvocationContext} implementation used for {@link javax.annotation.PostConstruct}
 * and {@link javax.annotation.PreDestroy} callbacks.
 * <p>
 * All lifecycle callback interceptors of a specific chain must be invoked on the same thread.
 */
class LifecycleCallbackInvocationContext extends AbstractInvocationContext {

    protected final Set<Annotation> bindings;
    protected final List<InterceptorInvocation> chain;
    private int position = 0;

    LifecycleCallbackInvocationContext(Object target, Object[] parameters,
            Set<Annotation> bindings, List<InterceptorInvocation> chain) {
        super(target, parameters, new ContextDataMap(bindings));
        this.chain = chain;
        this.bindings = bindings;
    }

    @Override
    public Object proceed() throws Exception {
        try {
            if (position < chain.size()) {
                // Invoke the next interceptor in the chain
                invokeNext();
            } else {
                // The invocation of proceed in the last interceptor method in the chain
                interceptorChainCompleted();
            }
            // The return value of a lifecycle callback is ignored
            return null;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    @Override
    public Set<Annotation> getInterceptorBindings() {
        return bindings;
    }

    @Override
    public Object[] getParameters() {
        throw new IllegalStateException();
    }

    @Override
    public void setParameters(Object[] params) {
        throw new IllegalStateException();
    }

    protected void interceptorChainCompleted() throws Exception {
        // No-op
    }

    private Object invokeNext() throws Exception {
        try {
            return chain.get(position++).invoke(this);
        } finally {
            position--;
        }
    }

}
