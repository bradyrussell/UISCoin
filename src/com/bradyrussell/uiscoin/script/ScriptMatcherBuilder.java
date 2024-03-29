/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.script;

import java.util.logging.Logger;

public class ScriptMatcherBuilder {
    private static final Logger Log = Logger.getLogger(StringBuilder.class.getName());
    private final ScriptMatcher matcher = new ScriptMatcher();

    public ScriptMatcherBuilder op(ScriptOperator Operator){
        matcher.scriptMatch.add(Operator);
        return this;
    }

    public ScriptMatcherBuilder any(){
        return any(1);
    }

    public ScriptMatcherBuilder any(int Count){
        for (int i = 0; i < Count; i++) {
            matcher.scriptMatch.add(null);
        }
        return this;
    }

    public ScriptMatcherBuilder push(){
        matcher.scriptMatch.add(ScriptOperator.PUSH);
        return this;
    }

    public ScriptMatcherBuilder setNumberOptionalOperationsAtEnd(int NumberOptionalOperations){
        matcher.OptionalOperatorsAtEnd = NumberOptionalOperations;
        return this;
    }

    public ScriptMatcher get(){
        return matcher;
    }
}
