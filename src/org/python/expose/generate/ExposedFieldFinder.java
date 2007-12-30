package org.python.expose.generate;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.FieldVisitor;

public abstract class ExposedFieldFinder implements FieldVisitor, PyTypes {

    public ExposedFieldFinder(String name, FieldVisitor delegate) {
        fieldName = name;
        this.delegate = delegate;
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if(EXPOSED_GET.getDescriptor().equals(desc)) {
            return new DescriptorVisitor(fieldName) {

                @Override
                public void handleResult(String name) {
                    exposeAsGet(name);
                }
            };
        } else if(EXPOSED_SET.getDescriptor().equals(desc)) {
            return new DescriptorVisitor(fieldName) {

                @Override
                public void handleResult(String name) {
                    exposeAsSet(name);
                }
            };
        } else {
            return delegate.visitAnnotation(desc, visible);
        }
    }

    public abstract void exposeAsGet(String name);

    public abstract void exposeAsSet(String name);

    public void visitAttribute(Attribute attr) {
        delegate.visitAttribute(attr);
    }

    public void visitEnd() {
        delegate.visitEnd();
    }

    private String fieldName;

    private FieldVisitor delegate;
}
