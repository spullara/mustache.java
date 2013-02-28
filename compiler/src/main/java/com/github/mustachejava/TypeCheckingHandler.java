package com.github.mustachejava;

import java.io.Writer;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import com.github.mustachejava.reflect.BaseObjectHandler;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

/**
 * Rather than pulling values this looks only at types. To check if a template matches the shape of
 * your view classes, pass in the set of classes you expect to have at runtime in the scope.
 * <p/>
 * User: sam Date: 2/3/13 Time: 9:43 AM
 */
public class TypeCheckingHandler extends BaseObjectHandler {
    /**
     * Find a value named "name" in the array of scopes in reverse order.
     * 
     * @param name
     * @param scopes
     * @return
     */
    public Wrapper find(String name, Object[] scopes) {
        for (Object scope : scopes) {
            if (!(scope instanceof Class)) {
                throw new MustacheException("Only classes allowed with this object handler: "
                        + scope);
            }
        }
        int length = scopes.length;
        if (length == 0) {
            throw new MustacheException("Empty scopes");
        }
        for (int i = length - 1; i >= 0; i--) {
            Object scope = scopes[i];
            if (scope == null || !(scope instanceof Class)) {
                throw new MustacheException("Invalid scope: " + scope);
            }
            Class scopeClass = (Class) scope;
            final AccessibleObject member = findMember(scopeClass, name);
            if (member != null) {
                return new Wrapper() {
                    public Object call(Object[] scopes) throws GuardException {
                        if (member instanceof Field) {
                            return ((Field) member).getType();
                        } else if (member instanceof Method) {
                            return ((Method) member).getReturnType();
                        } else {
                            throw new MustacheException("Member not a field or method: " + member);
                        }
                    }
                };
            }
        }
        throw new MustacheException("Failed to find matching field or method: " + name + " in "
                + Arrays.asList(scopes));
    }

    @Override
    public Binding createBinding(final String name, TemplateContext tc, Code code) {
        return new Binding() {
            public Object get(Object[] scopes) {
                return find(name, scopes).call(scopes);
            }
        };
    }

    @Override
    public Writer falsey(Iteration iteration, Writer writer, Object object, Object[] scopes) {
        // Iterate once in either case
        return iterate(iteration, writer, object, scopes);
    }

    @Override
    public Writer iterate(Iteration iteration, Writer writer, Object object, Object[] scopes) {
        return iteration.next(writer, object, scopes);
    }

    @Override
    public String stringify(Object object) {
        if (object instanceof Class) {
            Class c = (Class) object;
            return "[" + c.getSimpleName() + "]";
        }
        throw new MustacheException("Object was not a class: " + object);
    }
}
