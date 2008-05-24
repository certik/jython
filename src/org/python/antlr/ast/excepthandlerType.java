// Autogenerated AST node
package org.python.antlr.ast;
import org.python.antlr.PythonTree;
import org.antlr.runtime.Token;
import java.io.DataOutputStream;
import java.io.IOException;

public class excepthandlerType extends PythonTree {
    public exprType type;
    public exprType name;
    public stmtType[] body;
    public int lineno;
    public int col_offset;

    public static final String[] _fields = new String[]
    {"type","name","body","lineno","col_offset"};

    public excepthandlerType(PythonTree tree, exprType type, exprType name,
    stmtType[] body, int lineno, int col_offset) {
        super(tree);
        this.type = type;
        this.name = name;
        this.body = body;
        for(int ibody=0;ibody<body.length;ibody++) {
            addChild(body[ibody]);
        }
        this.lineno = lineno;
        this.col_offset = col_offset;
    }

    public String toString() {
        return "excepthandler";
    }

    public <R> R accept(VisitorIF<R> visitor) throws Exception {
        traverse(visitor);
        return null;
    }

    public void traverse(VisitorIF visitor) throws Exception {
        if (type != null)
            type.accept(visitor);
        if (name != null)
            name.accept(visitor);
        if (body != null) {
            for (int i = 0; i < body.length; i++) {
                if (body[i] != null)
                    body[i].accept(visitor);
            }
        }
    }

}